package com.ronjunevaldoz.graphyn.core

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
