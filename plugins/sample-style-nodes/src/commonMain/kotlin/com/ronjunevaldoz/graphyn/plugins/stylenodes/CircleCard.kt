package com.ronjunevaldoz.graphyn.plugins.stylenodes

import androidx.compose.runtime.Composable
import com.ronjunevaldoz.graphyn.editor.canvas.NodeCanvasContext
import com.ronjunevaldoz.graphyn.ui.cards.ShapeCardFactory

@Composable
fun CircleCard(ctx: NodeCanvasContext) = ShapeCardFactory().NodeCanvas(ctx)
