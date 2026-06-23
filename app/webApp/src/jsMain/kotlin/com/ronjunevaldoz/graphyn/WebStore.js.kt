package com.ronjunevaldoz.graphyn

import com.ronjunevaldoz.graphyn.core.store.LocalStorageWorkflowStore
import com.ronjunevaldoz.graphyn.core.store.WorkflowStore

internal actual fun createWebStore(): WorkflowStore? = LocalStorageWorkflowStore()
