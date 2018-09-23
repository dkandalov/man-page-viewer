package manpageviewer

import java.io.InputStreamReader

fun readManPage(term: String): String {
    // Only include "System calls" and "Library functions" sections
    // (see https://en.wikipedia.org/wiki/Man_page#Manual_sections)
    val manSections = "2:3"
    val manPage = executeCommand("man", "-S", manSections, term).stdout.replace(Regex(".\b"), "")
    return if (manPage.isEmpty()) "No man entry" else manPage
}

private fun executeCommand(vararg commandAndArgs: String): ExecResult {
    val cmdProc = Runtime.getRuntime().exec(commandAndArgs)
    val stdout = InputStreamReader(cmdProc.inputStream).buffered().readLines().joinToString("\n")
    val stderr = InputStreamReader(cmdProc.errorStream).buffered().readLines().joinToString("\n")
    return ExecResult(cmdProc.exitValue(), stdout, stderr)
}

private data class ExecResult(val exitCode: Int, val stdout: String, val stderr: String)
