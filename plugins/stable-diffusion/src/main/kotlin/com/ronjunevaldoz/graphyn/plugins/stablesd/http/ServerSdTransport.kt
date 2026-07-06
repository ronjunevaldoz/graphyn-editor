package com.ronjunevaldoz.graphyn.plugins.stablesd.http

import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsBytes
import io.ktor.http.ContentType
import io.ktor.http.contentType

internal data class ServerSdResponse(val status: Int, val body: ByteArray)

internal interface ServerSdTransport {
    suspend fun get(path: String, conn: SdConnection): ServerSdResponse
    suspend fun postJson(path: String, json: String, conn: SdConnection): ServerSdResponse
    suspend fun postBytes(path: String, bytes: ByteArray, conn: SdConnection): ServerSdResponse
    suspend fun delete(path: String, conn: SdConnection): ServerSdResponse
}

internal class HttpServerSdTransport(
    private val client: HttpClient = HttpClient(CIO) { engine { requestTimeout = 1_800_000 } },
) : ServerSdTransport {
    override suspend fun get(path: String, conn: SdConnection) = client.get("${conn.baseUrl}$path") { authWith(conn) }.toResponse()
    override suspend fun postJson(path: String, json: String, conn: SdConnection) = client.post("${conn.baseUrl}$path") {
        contentType(ContentType.Application.Json); setBody(json); authWith(conn)
    }.toResponse()
    override suspend fun postBytes(path: String, bytes: ByteArray, conn: SdConnection) = client.post("${conn.baseUrl}$path") {
        setBody(bytes); authWith(conn)
    }.toResponse()
    override suspend fun delete(path: String, conn: SdConnection) = client.delete("${conn.baseUrl}$path") { authWith(conn) }.toResponse()
}

private suspend fun HttpResponse.toResponse() = ServerSdResponse(status.value, bodyAsBytes())
