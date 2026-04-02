package com.example.day.mcpserver.tools

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class GlobToolsTest {

    // region — normalizeGlobPattern

    @Test
    fun `plain name without wildcards gets wrapped`() {
        assertEquals("**/*Worker*", normalizeGlobPattern("Worker"))
    }

    @Test
    fun `filename glob with stars gets path prefix`() {
        // This was the bug: *Worker* stayed as-is and matched nothing in subdirs
        assertEquals("**/*Worker*", normalizeGlobPattern("*Worker*"))
    }

    @Test
    fun `extension glob gets path prefix`() {
        assertEquals("**/*.kt", normalizeGlobPattern("*.kt"))
    }

    @Test
    fun `single star gets path prefix`() {
        assertEquals("**/*", normalizeGlobPattern("*"))
    }

    @Test
    fun `already anchored pattern is unchanged`() {
        assertEquals("**/*Worker*", normalizeGlobPattern("**/*Worker*"))
    }

    @Test
    fun `double-star prefix without slash is unchanged`() {
        // e.g. "**Worker" is unusual but user explicitly chose it
        assertEquals("**Worker", normalizeGlobPattern("**Worker"))
    }

    @Test
    fun `path pattern with slash is unchanged`() {
        assertEquals("app/src/**/*.kt", normalizeGlobPattern("app/src/**/*.kt"))
    }

    @Test
    fun `path pattern with leading slash is unchanged`() {
        assertEquals("/app/src/**/*.kt", normalizeGlobPattern("/app/src/**/*.kt"))
    }

    // endregion

    // region — globToRegex: *Worker* pattern (the bug scenario)

    private val workerPaths = listOf(
        "app/src/main/java/com/example/day/core/core_features/agent/domain/workers/concrete/OrchestratorWorker.kt",
        "app/src/main/java/com/example/day/core/core_features/pr_review/data/worker/TelegramPollingWorker.kt",
        "app/src/main/java/com/example/day/core/core_features/agent/domain/workers/base/AWorker.kt",
        "app/src/main/java/com/example/day/core/core_features/agent/domain/workers/base/WorkerEvent.kt",
    )

    private val nonWorkerPaths = listOf(
        "app/src/main/java/com/example/day/core/core_features/agent/domain/OrchestratorHelper.kt",
        "app/src/main/java/com/example/day/MainActivity.kt",
    )

    @Test
    fun `raw star-Worker-star matches files with Worker in name in subdirectories`() {
        val regex = globToRegex(normalizeGlobPattern("*Worker*"))
        workerPaths.forEach { path ->
            assertTrue(regex.matches(path), "Expected match for: $path")
        }
    }

    @Test
    fun `raw star-Worker-star does not match files without Worker`() {
        val regex = globToRegex(normalizeGlobPattern("*Worker*"))
        nonWorkerPaths.forEach { path ->
            assertFalse(regex.matches(path), "Expected no match for: $path")
        }
    }

    @Test
    fun `double-star Worker pattern matches same files`() {
        val regex = globToRegex("**/*Worker*")
        workerPaths.forEach { path ->
            assertTrue(regex.matches(path), "Expected match for: $path")
        }
    }

    // endregion

    // region — globToRegex: extension patterns

    @Test
    fun `star-kt matches kotlin files anywhere after normalization`() {
        val regex = globToRegex(normalizeGlobPattern("*.kt"))
        assertTrue(regex.matches("app/src/main/java/com/example/Worker.kt"))
        assertFalse(regex.matches("app/src/main/java/com/example/Worker.md"))
        assertFalse(regex.matches("app/src/main/java/com/example/Worker.kts"))
    }

    @Test
    fun `explicit path pattern is not changed by normalization`() {
        val regex = globToRegex(normalizeGlobPattern("app/src/**/*.kt"))
        assertTrue(regex.matches("app/src/main/java/com/example/Worker.kt"))
        assertFalse(regex.matches("mcp-server/src/main/kotlin/com/example/Server.kt"))
    }

    // endregion

    // region — globToRegex: brace expansion

    @Test
    fun `brace expansion matches any of the alternatives`() {
        val regex = globToRegex(normalizeGlobPattern("*.{kt,java}"))
        assertTrue(regex.matches("app/src/Worker.kt"))
        assertTrue(regex.matches("app/src/Worker.java"))
        assertFalse(regex.matches("app/src/Worker.md"))
    }

    // endregion
}
