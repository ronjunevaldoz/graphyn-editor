package com.ronjunevaldoz.graphyn.ui.cards

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.ronjunevaldoz.graphyn.core.designsystem.theme.appTheme
import com.ronjunevaldoz.graphyn.core.model.NodeSpec
import com.ronjunevaldoz.graphyn.editor.canvas.NodeCanvasContext
import com.ronjunevaldoz.graphyn.editor.canvas.NodeCanvasFactory
import com.ronjunevaldoz.graphyn.editor.canvas.NodeStatusBadge

internal const val HEADER_DP = 28
internal const val ROW_DP = 22
internal const val FOOTER_DIVIDER_DP = 1
internal const val VALUE_DP = 100
internal const val CARD_WIDTH_DP = 240
internal const val RECORD_POPUP_MIN_DP = 140
internal const val RECORD_POPUP_MAX_DP = 220
internal const val LIST_POPUP_MIN_DP = 120
internal const val LIST_POPUP_MAX_DP = 200
internal const val VALUE_MIN_DP = 48
internal const val VALUE_MAX_DP = 80

/**
 * Card with labelled field rows. Use for nodes whose inputs the user edits directly on the canvas.
 *
 * Height is computed automatically: `HEADER + inputRows × ROW + DIVIDER + outputRows × ROW`.
 *
 * ### Input type widgets
 * | [WorkflowType] | Widget | Behaviour |
 * |---|---|---|
 * | StringType / BooleanType | Text chip | Click to inline-edit; commits on focus loss |
 * | IntType | `−∣value∣+` stepper | Centre-click to type; `−`/`+` steps by 1 |
 * | DoubleType | `−∣value∣+` stepper | Centre-click to type; `−`/`+` steps by 0.1 |
 * | EnumType | Dropdown chip | Single-select popup |
 * | MultiEnumType | Dropdown chip | Multi-select popup with checkboxes |
 * | ListType(elementType) | `N items ▾` chip | Popup: add/remove items, inline edit per item |
 * | RecordType(fields) | `{ N fields } ▾` chip | Popup: one editable row per schema key |
 * | NullableType | Falls through to inner type widget | — |
 * | OpaqueType | Text chip (display only) | Accepts any connected type |
 *
 * ### Theme customisation
 * ```kotlin
 * FieldCardFactory(
 *     theme = FieldNodeTheme(
 *         background       = { appTheme.colors.surface },
 *         headerBackground = { appTheme.colors.surfaceVariant },
 *         selectedBorder   = { appTheme.colors.borderFocus },
 *         labelColor       = { appTheme.colors.onSurfaceVariant },
 *         valueText        = { appTheme.colors.onSurface },
 *     ),
 *     inputRows = 4,
 *     outputRows = 2,
 * )
 * ```
 */
class FieldCardFactory(
    val theme: FieldNodeTheme = FieldNodeTheme(),
    val inputRows: Int = 3,
    val outputRows: Int = 3,
) : NodeCanvasFactory {
    override val nodeWidth = CARD_WIDTH_DP
    override val nodeHeight = HEADER_DP + inputRows * ROW_DP + FOOTER_DIVIDER_DP + outputRows * ROW_DP

    override fun portAnchorY(portIndex: Int, isInput: Boolean, spec: NodeSpec): Int =
        if (isInput) {
            HEADER_DP + portIndex * ROW_DP + ROW_DP / 2
        } else {
            HEADER_DP + spec.inputs.size * ROW_DP + FOOTER_DIVIDER_DP + portIndex * ROW_DP + ROW_DP / 2
        }

    @Composable
    override fun NodeCanvas(context: NodeCanvasContext) {
        CompositionLocalProvider(LocalFieldNodeTheme provides theme) {
            FieldCardColorPickerHost { FieldCard(context) }
        }
    }
}

@Composable
private fun FieldCard(ctx: NodeCanvasContext) {
    val theme = LocalFieldNodeTheme.current
    val bg = theme.background()
    val borderColor = if (ctx.selected) theme.selectedBorder() else theme.border()
    val shape = RoundedCornerShape(appTheme.shapes.md)
    Box(
        modifier = Modifier
            .width(CARD_WIDTH_DP.dp)
            .clip(shape)
            .background(bg)
            .border(1.dp, borderColor, shape)
            .clickable { ctx.onSelect() },
    ) {
        Column {
            FieldHeader(
                ctx.spec.label,
                theme,
                description = ctx.spec.description,
                onMove = ctx.onMove,
                nodeId = ctx.node.id
            )
            FieldBody(
                inputs = ctx.spec.inputs,
                values = ctx.spec.defaultValues + ctx.node.config,
                onValueChange = { key, value -> ctx.onConfigChange(key, value) },
                theme = theme,
                modifier = Modifier.padding(horizontal = 10.dp)
            )

            if(ctx.spec.outputs.isNotEmpty()) {
                Spacer(
                    Modifier.fillMaxWidth()
                        .height(FOOTER_DIVIDER_DP.dp)
                        .background(theme.divider())
                )
            }

            FieldFooter(
                outputs = ctx.spec.outputs,
                theme = theme,
                modifier = Modifier.padding(horizontal = 10.dp)
            )
        }
        NodeStatusBadge(ctx.executionStatus, Modifier.align(Alignment.TopEnd).padding(appTheme.spacing.xs), bg)
    }
}
