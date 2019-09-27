package manpageviewer

import java.io.InputStreamReader
import java.util.concurrent.TimeUnit.MILLISECONDS

fun readManPage(term: String): String {
    // Only include "System calls" and "Library functions" sections
    // (see https://en.wikipedia.org/wiki/Man_page#Manual_sections)
    val manSections = "2:3"
    val (exitCode, stdout, stderr) = executeCommand("man", "-S", manSections, term)
    return if (exitCode == 0) {
        val manPage = stdout.replace(Regex(".\b"), "")
        if (manPage.isEmpty()) "No man entry" else manPage
    } else {
        stderr
    }
}

private fun executeCommand(vararg commandAndArgs: String): ExecResult {
    val cmdProc = Runtime.getRuntime().exec(commandAndArgs)
    val stdout = InputStreamReader(cmdProc.inputStream).buffered().readLines().joinToString("\n")
    val stderr = InputStreamReader(cmdProc.errorStream).buffered().readLines().joinToString("\n")
    val exited = cmdProc.waitFor(250, MILLISECONDS)
    return if (exited) ExecResult(cmdProc.exitValue(), stdout, stderr)
    else ExecResult(-1, "", "Timed out waiting for process")
}

private data class ExecResult(val exitCode: Int, val stdout: String, val stderr: String)
