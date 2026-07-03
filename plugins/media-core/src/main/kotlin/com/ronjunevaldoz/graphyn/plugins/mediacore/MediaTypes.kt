package com.ronjunevaldoz.graphyn.plugins.mediacore

import com.ronjunevaldoz.graphyn.core.model.WorkflowType
import com.ronjunevaldoz.graphyn.core.model.WorkflowValue

object MediaTypes {
    val videoHandle = WorkflowType.RecordType(
        mapOf(
            "kind" to WorkflowType.EnumType(listOf("video")),
            "path" to WorkflowType.StringType,
            "mime_type" to WorkflowType.StringType,
        ),
    )

    val audioHandle = WorkflowType.RecordType(
        mapOf(
            "kind" to WorkflowType.EnumType(listOf("audio")),
            "path" to WorkflowType.StringType,
            "mime_type" to WorkflowType.StringType,
        ),
    )

    val imageHandle = WorkflowType.RecordType(
        mapOf(
            "kind" to WorkflowType.EnumType(listOf("image")),
            "path" to WorkflowType.StringType,
            "mime_type" to WorkflowType.StringType,
        ),
    )

    fun videoValue(path: String, mimeType: String = "video/mp4"): WorkflowValue.RecordValue =
        mediaValue(kind = "video", path = path, mimeType = mimeType)

    fun audioValue(path: String, mimeType: String = "audio/wav"): WorkflowValue.RecordValue =
        mediaValue(kind = "audio", path = path, mimeType = mimeType)

    fun imageValue(path: String, mimeType: String = "image/png"): WorkflowValue.RecordValue =
        mediaValue(kind = "image", path = path, mimeType = mimeType)

    fun path(value: WorkflowValue?, expectedKind: String): String {
        val fields = (value as? WorkflowValue.RecordValue)?.fields
            ?: error("Expected a $expectedKind media handle.")
        val kind = (fields["kind"] as? WorkflowValue.StringValue)?.value
        require(kind == expectedKind) { "Expected a $expectedKind media handle, got ${kind ?: "unknown"}." }
        return (fields["path"] as? WorkflowValue.StringValue)?.value
            ?.takeIf(String::isNotBlank)
            ?: error("$expectedKind media handle has no path.")
    }

    private fun mediaValue(kind: String, path: String, mimeType: String): WorkflowValue.RecordValue =
        WorkflowValue.RecordValue(
            mapOf(
                "kind" to WorkflowValue.StringValue(kind),
                "path" to WorkflowValue.StringValue(path),
                "mime_type" to WorkflowValue.StringValue(mimeType),
            ),
        )
}
