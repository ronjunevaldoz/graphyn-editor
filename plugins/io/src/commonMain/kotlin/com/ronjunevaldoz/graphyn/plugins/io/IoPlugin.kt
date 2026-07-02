package com.ronjunevaldoz.graphyn.plugins.io

import com.ronjunevaldoz.graphyn.core.common.FileIO
import com.ronjunevaldoz.graphyn.core.model.WorkflowValue
import com.ronjunevaldoz.graphyn.pluginapi.GRAPHYN_PLUGIN_API_VERSION
import com.ronjunevaldoz.graphyn.pluginapi.GraphynPlugin
import com.ronjunevaldoz.graphyn.pluginapi.GraphynPluginMetadata
import com.ronjunevaldoz.graphyn.pluginapi.GraphynPluginRegistrar
import io.ktor.client.request.request
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpMethod
import io.ktor.http.contentType
import io.ktor.http.isSuccess

object IoPlugin : GraphynPlugin {
    override val metadata = GraphynPluginMetadata(
        id = "graphyn.io",
        displayName = "I/O Operations",
        version = "1.0.0",
        apiVersion = GRAPHYN_PLUGIN_API_VERSION,
    )

    private val httpClient by lazy { createHttpClient() }

    override fun register(registrar: GraphynPluginRegistrar) {
        listOf(specHttpRequest, specFileRead, specFileWrite, specFileBrowse, specFolderBrowse, specWebhookPost, specEnvRead, specResolvePath)
            .forEach { registrar.registerNodeSpec(it) }

        registrar.registerExecutor(specHttpRequest.type) { inputs ->
            val url = (inputs["url"] as? WorkflowValue.StringValue)?.value?.takeIf { it.isNotBlank() }
                ?: return@registerExecutor mapOf("body" to WorkflowValue.StringValue(""), "statusCode" to WorkflowValue.IntValue(0), "ok" to WorkflowValue.BooleanValue(false))
            val method = (inputs["method"] as? WorkflowValue.StringValue)?.value ?: "GET"
            val body = (inputs["body"] as? WorkflowValue.StringValue)?.value
            try {
                val response = httpClient.request(url) {
                    this.method = HttpMethod.parse(method)
                    if (body != null) setBody(body)
                }
                mapOf("body" to WorkflowValue.StringValue(response.bodyAsText()), "statusCode" to WorkflowValue.IntValue(response.status.value), "ok" to WorkflowValue.BooleanValue(response.status.isSuccess()))
            } catch (e: Exception) {
                mapOf("body" to WorkflowValue.StringValue("Error: ${e.message}"), "statusCode" to WorkflowValue.IntValue(0), "ok" to WorkflowValue.BooleanValue(false))
            }
        }

        registrar.registerExecutor(specFileRead.type) { inputs ->
            val path = (inputs["path"] as? WorkflowValue.StringValue)?.value ?: ""
            val content = FileIO.read(path)
            mapOf("content" to WorkflowValue.StringValue(content ?: ""), "exists" to WorkflowValue.BooleanValue(content != null))
        }

        registrar.registerExecutor(specFileWrite.type) { inputs ->
            val path = (inputs["path"] as? WorkflowValue.StringValue)?.value ?: ""
            val content = (inputs["content"] as? WorkflowValue.StringValue)?.value ?: ""
            val append = (inputs["append"] as? WorkflowValue.BooleanValue)?.value ?: false
            val ok = path.isNotBlank() && FileIO.write(path, content, append)
            mapOf("success" to WorkflowValue.BooleanValue(ok))
        }

        registrar.registerExecutor(specFileBrowse.type) { inputs ->
            mapOf("path" to (inputs["path"] ?: WorkflowValue.StringValue("")))
        }

        registrar.registerExecutor(specFolderBrowse.type) { inputs ->
            mapOf("path" to (inputs["path"] ?: WorkflowValue.StringValue("")))
        }

        registrar.registerExecutor(specWebhookPost.type) { inputs ->
            val url = (inputs["url"] as? WorkflowValue.StringValue)?.value?.takeIf { it.isNotBlank() }
                ?: return@registerExecutor mapOf("ok" to WorkflowValue.BooleanValue(false), "statusCode" to WorkflowValue.IntValue(0))
            val payload = (inputs["payload"] as? WorkflowValue.StringValue)?.value ?: "{}"
            try {
                val response = httpClient.request(url) {
                    method = HttpMethod.Post
                    contentType(ContentType.Application.Json)
                    setBody(payload)
                }
                mapOf("ok" to WorkflowValue.BooleanValue(response.status.isSuccess()), "statusCode" to WorkflowValue.IntValue(response.status.value))
            } catch (e: Exception) {
                mapOf("ok" to WorkflowValue.BooleanValue(false), "statusCode" to WorkflowValue.IntValue(0))
            }
        }

        registrar.registerExecutor(specEnvRead.type) { inputs ->
            val name = (inputs["name"] as? WorkflowValue.StringValue)?.value ?: ""
            val value = if (name.isNotBlank()) EnvReader.get(name) else null
            mapOf("value" to (value?.let { WorkflowValue.StringValue(it) } ?: WorkflowValue.NullValue), "found" to WorkflowValue.BooleanValue(value != null))
        }

        registrar.registerExecutor(specResolvePath.type) { inputs ->
            val baseDirVal = inputs["base_dir"] as? WorkflowValue.StringValue
            val relativePathVal = inputs["relative_path"] as? WorkflowValue.StringValue

            if (baseDirVal == null || relativePathVal == null) {
                return@registerExecutor mapOf("error" to WorkflowValue.StringValue("Missing or invalid input types: 'base_dir' and 'relative_path' must both be Strings."))
            }

            val resolvedPath = FileIO.resolvePath(baseDirVal.value, relativePathVal.value)
            mapOf("resolved_path" to WorkflowValue.StringValue(resolvedPath))
        }
    }
}
