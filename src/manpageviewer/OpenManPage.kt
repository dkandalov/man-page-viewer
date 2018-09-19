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
            val from = (0..offset).reversed().find { i -> !Character.isJavaIdentifierPart(text[i]) } ?: offset + 1
            val to = (offset..text.length).find { i -> !Character.isJavaIdentifierPart(text[i]) } ?: offset
            term = text.substring(from, to).toLowerCase()
        }
        if (term.isEmpty()) return

        // man -S 2:3 $1 | col -bx
        val manSections = "2:3" // only include System calls and Library functions sections
        val manText = execute2("man", "-S", manSections, term).stdout
        val (consoleView, closeAction) = showInConsole2(manText, "man $term", project)
        consoleView.scrollTo(0)

        // TODO use closeAction
    }

    private fun execute2(vararg commandAndArgs: String): ExecResult {
        val cmdProc = Runtime.getRuntime().exec(commandAndArgs)
        return ExecResult(
            exitCode = cmdProc.exitValue(),
            stdout = InputStreamReader(cmdProc.inputStream).buffered().readLines().joinToString("\n"),
            stderr = InputStreamReader(cmdProc.errorStream).buffered().readLines().joinToString("\n")
        )
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
