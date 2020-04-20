package manpageviewer

import com.intellij.execution.filters.TextConsoleBuilderFactory
import com.intellij.execution.ui.ConsoleView
import com.intellij.execution.ui.ConsoleViewContentType.NORMAL_OUTPUT
import com.intellij.icons.AllIcons
import com.intellij.openapi.actionSystem.*
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Disposer
import com.intellij.openapi.util.Key
import com.intellij.openapi.wm.ToolWindowAnchor
import com.intellij.openapi.wm.ToolWindowManager
import java.awt.BorderLayout
import javax.swing.JPanel

private val consoleKey: Key<ConsoleView> = Key("manpageviewer.consoleKey")
private const val toolWindowId = "Man"

fun showToolWindow(message: String, consoleTitle: String, project: Project) {
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
        add(object : AnAction(AllIcons.Actions.Cancel) {
            override fun actionPerformed(event: AnActionEvent) {
                project.putUserData(consoleKey, null)
                Disposer.dispose(toolWindowDisposable)
            }
        })
    }
    val consoleComponent = JPanel(BorderLayout()).apply {
        val toolbarPanel = JPanel(BorderLayout()).apply {
            add(ActionManager.getInstance().createActionToolbar(ActionPlaces.UNKNOWN, toolbarActions, false).component)
        }
        add(toolbarPanel, BorderLayout.WEST)
        add(console.component, BorderLayout.CENTER)
    }
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
    toolWindow.setIcon(AllIcons.Toolwindows.Documentation) // Has to be 13x13 icon
    toolWindow.activate({}, false)
}
