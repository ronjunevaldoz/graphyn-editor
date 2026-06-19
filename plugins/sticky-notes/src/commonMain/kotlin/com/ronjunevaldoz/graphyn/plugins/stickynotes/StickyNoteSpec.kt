package com.ronjunevaldoz.graphyn.plugins.stickynotes

import com.ronjunevaldoz.graphyn.core.model.NodeSpec
import com.ronjunevaldoz.graphyn.core.model.WorkflowValue

const val STICKY_NOTE_TYPE = "graphyn.sticky_note"
const val STICKY_NOTE_CATEGORY = "graphyn.annotations"
const val STICKY_NOTE_TEXT_KEY = "text"
const val STICKY_NOTE_W_KEY = "__w"
const val STICKY_NOTE_H_KEY = "__h"
const val STICKY_NOTE_DEFAULT_W = 200
const val STICKY_NOTE_DEFAULT_H = 160

internal val StickyNoteSpec = NodeSpec(
    type = STICKY_NOTE_TYPE,
    label = "Sticky Note",
    description = "Canvas annotation — text note with no data ports.",
    category = STICKY_NOTE_CATEGORY,
    inputs = emptyList(),
    outputs = emptyList(),
    defaultValues = mapOf(STICKY_NOTE_TEXT_KEY to WorkflowValue.StringValue("")),
)
