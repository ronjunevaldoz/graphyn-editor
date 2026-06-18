package com.ronjunevaldoz.graphyn.plugins.stylenodes

import androidx.compose.runtime.Composable
import com.ronjunevaldoz.graphyn.editor.canvas.NodeCanvasContext
import com.ronjunevaldoz.graphyn.ui.cards.FieldCardFactory

@Composable
fun FieldCard(ctx: NodeCanvasContext) = FieldCardFactory(inputRows = 5, outputRows = 1).NodeCanvas(ctx)
