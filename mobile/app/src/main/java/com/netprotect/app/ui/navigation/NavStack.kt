package com.netprotect.app.ui.navigation

/** Sprint 42 (D-01): the app's navigation is a plain stack of routes, not `navigation-compose`.
 * Two levels of depth and no deep links do not justify a dependency.
 *
 * Pure Kotlin and immutable, so the whole behaviour is testable on the JVM. The route type [R] is
 * whatever each feature declares (a `sealed interface` per area); this class knows nothing about
 * screens. The stack is never empty: the bottom entry is where "back" ends.
 */
class NavStack<R : Any> private constructor(val entries: List<R>) {

    init {
        require(entries.isNotEmpty()) { "a NavStack always has at least its root" }
    }

    val current: R get() = entries.last()

    /** True when "back" would go somewhere; false on the bottom entry, where the system decides. */
    val canGoBack: Boolean get() = entries.size > 1

    /** Opens [route] on top. Opening the route that is already on top is a no-op, so a double tap
     * cannot stack the same screen twice. */
    fun push(route: R): NavStack<R> = if (route == current) this else NavStack(entries + route)

    /** Goes back one entry; on the bottom entry returns this stack unchanged. */
    fun pop(): NavStack<R> = if (canGoBack) NavStack(entries.dropLast(1)) else this

    /** Goes back to the nearest entry for which [predicate] is true, dropping everything above
     * it. Returns this stack unchanged if nothing matches. */
    fun popTo(predicate: (R) -> Boolean): NavStack<R> {
        val index = entries.indexOfLast(predicate)
        return if (index < 0 || index == entries.lastIndex) this else NavStack(entries.take(index + 1))
    }

    /** Replaces the top entry (e.g. sign-in success replaces the login route). */
    fun replaceTop(route: R): NavStack<R> = NavStack(entries.dropLast(1) + route)

    /** Drops the whole history and starts again from [root] (e.g. switching mode, signing out). */
    fun resetTo(root: R): NavStack<R> = start(root)

    /** One string per entry, for saving state across rotation/process death. */
    fun encode(encodeRoute: (R) -> String): List<String> = entries.map(encodeRoute)

    override fun equals(other: Any?): Boolean = other is NavStack<*> && other.entries == entries

    override fun hashCode(): Int = entries.hashCode()

    override fun toString(): String = "NavStack$entries"

    companion object {
        fun <R : Any> start(root: R): NavStack<R> = NavStack(listOf(root))

        /** Rebuilds a stack from [encoded]. Entries that no longer decode (a route renamed in a
         * newer build) are skipped; if none survive, the result is null and the caller starts over
         * from its root. */
        fun <R : Any> decode(encoded: List<String>, decodeRoute: (String) -> R?): NavStack<R>? {
            val routes = encoded.mapNotNull(decodeRoute)
            return if (routes.isEmpty()) null else NavStack(routes)
        }
    }
}
