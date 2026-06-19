package com.ronjunevaldoz.graphyn.ui.cards

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.awaitTouchSlopOrCancellation
import androidx.compose.foundation.gestures.drag
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.ronjunevaldoz.graphyn.core.designsystem.theme.appTheme
import com.ronjunevaldoz.graphyn.core.model.NodeSpec
import com.ronjunevaldoz.graphyn.editor.canvas.NodeCanvasContext
import com.ronjunevaldoz.graphyn.editor.canvas.NodeCanvasFactory
import com.ronjunevaldoz.graphyn.editor.canvas.NodeStatusBadge
import kotlin.math.roundToInt

internal const val HEADER_DP = 28
internal const val ROW_DP = 22
internal const val FOOTER_DIVIDER_DP = 9
internal const val VALUE_DP = 100

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
    override val nodeWidth = 240
    override val nodeHeight = HEADER_DP + inputRows * ROW_DP + FOOTER_DIVIDER_DP + outputRows * ROW_DP

    override fun portAnchorY(portIndex: Int, isInput: Boolean, spec: NodeSpec): Int =
        if (isInput) {
            HEADER_DP + portIndex * ROW_DP + ROW_DP / 2
        } else {
            HEADER_DP + spec.inputs.size * ROW_DP + FOOTER_DIVIDER_DP + portIndex * ROW_DP + ROW_DP / 2
        }

    @Composable
    override fun NodeCanvas(context: NodeCanvasContext) = FieldCard(context, theme)
}

@Composable
private fun FieldCard(ctx: NodeCanvasContext, theme: FieldNodeTheme) {
    val bg = theme.background()
    val borderColor = if (ctx.selected) theme.selectedBorder() else theme.border()
    val shape = RoundedCornerShape(appTheme.shapes.md)
    Box(
        modifier = Modifier
            .width(240.dp).clip(shape).background(bg).border(1.dp, borderColor, shape)
            .clickable { ctx.onSelect() }
            .pointerInput(Unit) {
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    awaitTouchSlopOrCancellation(down.id) { c, _ -> c.consume() }
                        ?: return@awaitEachGesture
                    drag(down.id) { c ->
                        c.consume()
                        val d = c.position - c.previousPosition
                        ctx.onMove(IntOffset(d.x.roundToInt(), d.y.roundToInt()))
                    }
                }
            },
    ) {
        Column {
            FieldHeader(ctx.spec.label, theme)
            FieldBody(
                inputs = ctx.spec.inputs,
                values = ctx.spec.defaultValues + ctx.node.config,
                onValueChange = { key, value -> ctx.onConfigChange(key, value) },
                theme = theme,
            )
            FieldFooter(ctx.spec.outputs, theme)
        }
        NodeStatusBadge(ctx.executionStatus, Modifier.align(Alignment.TopEnd).padding(4.dp), bg)
    }
}
