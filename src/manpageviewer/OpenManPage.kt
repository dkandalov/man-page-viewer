package manpageviewer

import com.intellij.execution.filters.TextConsoleBuilderFactory
import com.intellij.execution.ui.ConsoleView
import com.intellij.execution.ui.ConsoleViewContentType.NORMAL_OUTPUT
import com.intellij.execution.ui.ExecutionConsole
import com.intellij.icons.AllIcons
import com.intellij.icons.AllIcons.Actions.Cancel
import com.intellij.openapi.actionSystem.*
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Disposer
import com.intellij.openapi.util.Key
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowAnchor
import com.intellij.openapi.wm.ToolWindowManager
import java.awt.BorderLayout
import java.io.InputStreamReader
import javax.swing.JPanel

class OpenManPage : AnAction() {
    override fun actionPerformed(event: AnActionEvent) {
        val project = event.project ?: return
        val editor = FileEditorManager.getInstance(project).selectedTextEditor ?: return
        val term = editor.termUnderCaret()
        if (term.isEmpty()) return

        val manText = readManPage(term)

        showInToolWindow(manText, term, project)
    }

    private fun readManPage(term: String): String {
        // Only include "System calls" and "Library functions" sections
        // (see https://en.wikipedia.org/wiki/Man_page#Manual_sections)
        val manSections = "2:3"
        val manPage = execute2("man", "-S", manSections, term).stdout.replace(Regex(".\b"), "")
        return if (manPage.isEmpty()) "No man entry" else manPage
    }

    private fun Editor.termUnderCaret(): String {
        val selectedText = selectionModel.selectedText?.toLowerCase()
        if (selectedText != null) return selectedText

        val offset = caretModel.offset
        val text = document.charsSequence.toString()
        val from = 0.until(offset).reversed().find { i -> !Character.isJavaIdentifierPart(text[i]) }?.let { it + 1 }
            ?: offset
        val to = offset.until(text.length).find { i -> !Character.isJavaIdentifierPart(text[i]) } ?: offset
        return text.substring(from, to).toLowerCase()
    }

    private fun execute2(vararg commandAndArgs: String): ExecResult {
        val cmdProc = Runtime.getRuntime().exec(commandAndArgs)
        val stdout = InputStreamReader(cmdProc.inputStream).buffered().readLines().joinToString("\n")
        val stderr = InputStreamReader(cmdProc.errorStream).buffered().readLines().joinToString("\n")
        return ExecResult(cmdProc.exitValue(), stdout, stderr)
    }

    private data class ExecResult(val exitCode: Int, val stdout: String, val stderr: String)

    private val consoleKey: Key<ConsoleView> = Key("manpageviewer.consoleKey")
    private val toolWindowId = "Man"

    private fun showInToolWindow(message: String, consoleTitle: String, project: Project) {
        val existingConsole = project.getUserData(consoleKey)
        val existingToolWindow = ToolWindowManager.getInstance(project).getToolWindow(toolWindowId)
        if (existingConsole != null && existingToolWindow != null) {
            existingConsole.clear()
            existingConsole.print(message, NORMAL_OUTPUT)
            existingConsole.scrollTo(0)
            existingToolWindow.title = consoleTitle
            existingToolWindow.activate({}, false)
            return
        }

        val console = TextConsoleBuilderFactory.getInstance().createBuilder(project).console
        project.putUserData(consoleKey, console)

        val toolWindowDisposable = project.newChildDisposable()
        Disposer.register(toolWindowDisposable, console)
        val toolbarActions = DefaultActionGroup().apply {
            add(object : AnAction(Cancel) {
                override fun actionPerformed(event: AnActionEvent) {
                    project.putUserData(consoleKey, null)
                    Disposer.dispose(toolWindowDisposable)
                }
            })
        }
        val consoleComponent = MyConsolePanel(console, toolbarActions)
        console.print(message, NORMAL_OUTPUT)
        console.scrollTo(0)

        val toolWindow = registerToolWindowIn(
            project = project,
            toolWindowId = toolWindowId,
            parentDisposable = toolWindowDisposable,
            location = ToolWindowAnchor.BOTTOM,
            toolbarActionGroup = null,
            createComponent = { consoleComponent }
        )
        toolWindow.title = consoleTitle
        toolWindow.icon = AllIcons.Toolwindows.Documentation // Has to be 13x13 icon
        toolWindow.activate({}, false)
    }

    private class MyConsolePanel(console: ExecutionConsole, toolbarActions: ActionGroup) : JPanel(BorderLayout()) {
        init {
            val toolbarPanel = JPanel(BorderLayout())
            toolbarPanel.add(ActionManager.getInstance().createActionToolbar(ActionPlaces.UNKNOWN, toolbarActions, false).component)
            add(toolbarPanel, BorderLayout.WEST)
            add(console.component, BorderLayout.CENTER)
        }
    }
}
