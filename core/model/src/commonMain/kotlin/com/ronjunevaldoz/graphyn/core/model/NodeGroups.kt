package com.ronjunevaldoz.graphyn.core.model

/**
 * Standard parent-folder names used to nest related node categories in the palette. Categories
 * that declare the same group value are shown under one collapsible folder.
 *
 * Lives in `core:model` (not the editor contract) so any module — editor or headless — can
 * reference folder names without depending on the UI layer. Keeping them here also means the
 * taxonomy can be renamed in one place.
 */
object NodeGroups {
    const val SOCIALS = "Socials"
    const val DATA = "Data"
    const val FLOW = "Flow"
    const val CREATIVE = "Creative"
}
