package com.ronjunevaldoz.graphyn.plugins.linkedin

import kotlin.test.Test
import kotlin.test.assertEquals

class LinkedInNodeSpecsTest {

    @Test
    fun allSpecsCarryLinkedInCategory() {
        // Without a category the palette can't group the nodes under the LinkedIn / Socials folder.
        assertEquals(
            emptyList(),
            LinkedInNodeSpecs.all.filter { it.category != LinkedInNodeSpecs.CATEGORY }.map { it.type },
        )
    }
}
