package com.netprotect.app.feature.home

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/** Placeholder written by Claude so HomeScreen compiles; DeepSeek replaces the body (Sprint 43). */
@Composable
fun RoleSelectionScreen(
    displayName: String?,
    email: String,
    error: String?,
    onSelectTutor: () -> Unit,
    onSelectSupervised: () -> Unit,
    onSignOut: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(modifier.fillMaxSize())
}
