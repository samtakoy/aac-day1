package com.example.day.ragserver.indexing

import java.io.File

object FileScanner {

    private val EXCLUDED_DIRS = setOf("build", ".git", ".gradle", "generated", ".idea", ".kotlin", "node_modules")
    private val INCLUDED_EXTENSIONS = setOf("kt", "kts", "md")

    fun scan(rootPath: String): List<File> {
        val root = File(rootPath)
        require(root.exists()) { "CODE_PATH does not exist: $rootPath" }
        require(root.isDirectory) { "CODE_PATH is not a directory: $rootPath" }

        return root.walkTopDown()
            .onEnter { dir -> dir.name !in EXCLUDED_DIRS }
            .filter { it.isFile && it.extension in INCLUDED_EXTENSIONS }
            .sortedBy { it.absolutePath }
            .toList()
            .also { println("FileScanner: found ${it.size} files in $rootPath") }
    }
}
