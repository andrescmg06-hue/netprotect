package com.netprotect.app.ui.navigation

import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable

/** Sprint 42 (D-01): a [NavStack] that survives rotation and process death. Each feature passes
 * its own [encode]/[decode] for its routes; a route that no longer decodes is dropped and, if
 * nothing survives, the stack starts over from [root]. */
@Composable
fun <R : Any> rememberNavStack(
    root: R,
    encode: (R) -> String,
    decode: (String) -> R?,
): MutableState<NavStack<R>> = rememberSaveable(
    saver = listSaver<MutableState<NavStack<R>>, String>(
        save = { it.value.encode(encode) },
        restore = { saved -> mutableStateOf(NavStack.decode(saved, decode) ?: NavStack.start(root)) },
    ),
) { mutableStateOf(NavStack.start(root)) }

/** Makes the system "back" gesture/button pop [stack] while there is somewhere to go back to; on
 * the bottom entry it stays disabled so the system handles it (leaves the app). */
@Composable
fun <R : Any> NavBackHandler(stack: MutableState<NavStack<R>>) {
    BackHandler(enabled = stack.value.canGoBack) { stack.value = stack.value.pop() }
}
