package com.ronjunevaldoz.graphyn.core

/** Marks an API that may change or be removed without notice in any future release. */
@RequiresOptIn(
    message = "This Graphyn API is experimental and may change in a future release without notice.",
    level = RequiresOptIn.Level.WARNING,
)
@Retention(AnnotationRetention.BINARY)
@Target(
    AnnotationTarget.CLASS,
    AnnotationTarget.FUNCTION,
    AnnotationTarget.PROPERTY,
    AnnotationTarget.CONSTRUCTOR,
)
annotation class GraphynExperimentalApi
