package manpageviewer

import com.intellij.execution.ExecutionManager
import com.intellij.execution.executors.DefaultRunExecutor
import com.intellij.execution.filters.TextConsoleBuilderFactory
import com.intellij.execution.ui.ConsoleView
import com.intellij.execution.ui.ConsoleViewContentType.NORMAL_OUTPUT
import com.intellij.execution.ui.ExecutionConsole
import com.intellij.execution.ui.RunContentDescriptor
import com.intellij.execution.ui.actions.CloseAction
import com.intellij.icons.AllIcons
import com.intellij.openapi.actionSystem.*
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.ModalityState.NON_MODAL
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.project.Project
import java.awt.BorderLayout
import java.io.InputStreamReader
import java.util.concurrent.atomic.AtomicReference
import javax.swing.JPanel

class OpenManPage : AnAction() {
    override fun actionPerformed(event: AnActionEvent) {
        val project = event.project ?: return
        val editor = FileEditorManager.getInstance(project).selectedTextEditor ?: return

        var term = editor.selectionModel.selectedText?.toLowerCase()
        if (term == null) {
            val offset = editor.caretModel.offset
            val text = editor.document.charsSequence.toString()
            val from = 0.until(offset).reversed().find { i -> !Character.isJavaIdentifierPart(text[i]) }?.let{ it + 1 } ?: offset
            val to = offset.until(text.length).find { i -> !Character.isJavaIdentifierPart(text[i]) } ?: offset
            term = text.substring(from, to).toLowerCase()
        }
        if (term.isEmpty()) return

        // Only include "System calls" and "Library functions" sections
        // (see https://en.wikipedia.org/wiki/Man_page#Manual_sections)
        val manSections = "2:3"
        val manText = execute2("man", "-S", manSections, term).stdout.replace(Regex(".\b"), "")
        val (consoleView, closeAction) = showInConsole2(manText, "man $term", project)
        consoleView.scrollTo(0)

        // TODO use closeAction
    }

    private fun execute2(vararg commandAndArgs: String): ExecResult {
        val cmdProc = Runtime.getRuntime().exec(commandAndArgs)
        val stdout = InputStreamReader(cmdProc.inputStream).buffered().readLines().joinToString("\n")
        val stderr = InputStreamReader(cmdProc.errorStream).buffered().readLines().joinToString("\n")
        return ExecResult(cmdProc.exitValue(), stdout, stderr)
    }

    private data class ExecResult(val exitCode: Int, val stdout: String, val stderr: String)

    private fun showInConsole2(message: String, consoleTitle: String = "",  project: Project): Pair<ConsoleView, CloseAction> {
        val result = AtomicReference<Pair<ConsoleView, CloseAction>>(null)
        // Use reference for consoleTitle because get groovy Reference class like in this bug http://jira.codehaus.org/browse/GROOVY-5101
        val titleRef = AtomicReference(consoleTitle)

        ApplicationManager.getApplication().invokeAndWait({
            val console = TextConsoleBuilderFactory.getInstance().createBuilder(project).console
            console.print(message, NORMAL_OUTPUT)

            val toolbarActions = DefaultActionGroup()
            val consoleComponent = MyConsolePanel(console, toolbarActions)
            val descriptor = object : RunContentDescriptor(console, null, consoleComponent, titleRef.get()) {
                override fun isContentReuseProhibited() = true
                override fun getIcon() = AllIcons.Nodes.Plugin
            }
            val executor = DefaultRunExecutor.getRunExecutorInstance()

            val closeAction = CloseAction(executor, descriptor, project)
            toolbarActions.add(closeAction)
            console.createConsoleActions().forEach { toolbarActions.add(it) }

            ExecutionManager.getInstance(project).contentManager.showRunContent(executor, descriptor)
            result.set(Pair(console, closeAction))
        }, NON_MODAL)

        return result.get()!!
    }

    private class MyConsolePanel(consoleView: ExecutionConsole, toolbarActions: ActionGroup) : JPanel(BorderLayout()) {
        init {
            val toolbarPanel = JPanel(BorderLayout())
            toolbarPanel.add(ActionManager.getInstance().createActionToolbar(ActionPlaces.UNKNOWN, toolbarActions, false).component)
            add(toolbarPanel, BorderLayout.WEST)
            add(consoleView.component, BorderLayout.CENTER)
        }
    }
}
