package com.ronjunevaldoz.graphyn.plugins.stylenodes

import androidx.compose.ui.graphics.Color

// Shared surface — identical across all three card styles
internal val NODE_BG        = Color(0xFF1C1C1E)
internal val NODE_BORDER    = Color(0xFF3A3A3C)
internal val NODE_SELECT    = Color(0xFF8B5CF6)  // unified purple selection
internal val NODE_TEXT      = Color(0xFFE8E8E8)
internal val NODE_MUTED     = Color(0xFF8A8A8F)

// Distinct header per card style (intentional, communicates domain)
internal val DARK_HEADER_BG = Color(0xFF3D2A6B)  // purple  — AI / ComfyUI
internal val FIELD_HEADER_BG = Color(0xFF1B3A5C)  // navy   — Geometry / Blender
internal val CIRCLE_BG      = Color(0xFF6366F1)   // indigo — Automation / n8n

// FieldCard value pill
internal val FIELD_VALUE_BG = Color(0xFF2C2C2E)

internal val CORNER_RADIUS  = 6
