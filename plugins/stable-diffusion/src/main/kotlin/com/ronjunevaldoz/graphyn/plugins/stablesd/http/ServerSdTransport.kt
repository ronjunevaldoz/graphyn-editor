package com.ronjunevaldoz.graphyn.plugins.stablesd.http

import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsBytes
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.contentType

internal data class ServerSdResponse(val status: Int, val body: ByteArray)

internal interface ServerSdTransport {
    suspend fun get(path: String, conn: SdConnection): ServerSdResponse
    suspend fun postJson(path: String, json: String, conn: SdConnection): ServerSdResponse
    suspend fun postBytes(path: String, bytes: ByteArray, conn: SdConnection): ServerSdResponse
    suspend fun delete(path: String, conn: SdConnection): ServerSdResponse
}

internal class HttpServerSdTransport(
    private val client: HttpClient = HttpClient(CIO) {
        engine {
            requestTimeout = 1_800_000
            // Ktor CIO defaults pipelineMaxSize to 20 (HTTP/1.1 pipelining) and keepAliveTime to
            // 5000ms — this client is reused for every sd.* node in a workflow run (ServerSdClient
            // is constructed once per HttpStableDiffusionBackend, itself once per CLI run), so
            // every request in a run shares one connection pool. Confirmed via a real end-to-end
            // run against a Modal-hosted server-sd: a generateImage() call's own internal request
            // sequence (ping/status/jobs/upload/models-exists/generate-ex, all firing back-to-back
            // on a kept-alive connection) got an empty "303" on the final generate-ex call with
            // zero server-side error logged, while an equivalent single fresh-connection curl
            // request succeeded immediately. Disabling pipelining alone (pipelineMaxSize=1) did NOT
            // fix it — the failure survived a real retest — so this forces every request onto its
            // own fresh connection instead, via both a very short keep-alive window and an explicit
            // "Connection: close" header (the standard signal any HTTP/1.1-aware proxy should
            // respect). Costs a full TCP/TLS handshake per request; trading that for correctness
            // against Modal's edge, whatever specifically it's doing with reused connections.
            endpoint.pipelineMaxSize = 1
            endpoint.keepAliveTime = 1
        }
    },
) : ServerSdTransport {
    override suspend fun get(path: String, conn: SdConnection) = client.get("${conn.baseUrl}$path") { authWith(conn); closeConnection() }.toResponse()
    override suspend fun postJson(path: String, json: String, conn: SdConnection) = client.post("${conn.baseUrl}$path") {
        contentType(ContentType.Application.Json); setBody(json); authWith(conn); closeConnection()
    }.toResponse()
    override suspend fun postBytes(path: String, bytes: ByteArray, conn: SdConnection) = client.post("${conn.baseUrl}$path") {
        setBody(bytes); authWith(conn); closeConnection()
    }.toResponse()
    override suspend fun delete(path: String, conn: SdConnection) = client.delete("${conn.baseUrl}$path") { authWith(conn); closeConnection() }.toResponse()
}

private fun io.ktor.client.request.HttpRequestBuilder.closeConnection() {
    header(HttpHeaders.Connection, "close")
}

private suspend fun HttpResponse.toResponse() = ServerSdResponse(status.value, bodyAsBytes())
