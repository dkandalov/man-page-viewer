package manpageviewer

import com.intellij.openapi.Disposable
import com.intellij.openapi.actionSystem.ActionGroup
import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.actionSystem.ActionPlaces
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.SimpleToolWindowPanel
import com.intellij.openapi.util.Disposer
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowAnchor
import com.intellij.openapi.wm.ToolWindowAnchor.RIGHT
import com.intellij.openapi.wm.ToolWindowManager
import com.intellij.ui.content.ContentFactory
import java.util.concurrent.atomic.AtomicBoolean
import javax.swing.JComponent

fun registerToolWindowIn(
    project: Project,
    toolWindowId: String,
    disposable: Disposable,
    location: ToolWindowAnchor = RIGHT,
    toolbarActionGroup: ActionGroup? = null,
    createComponent: () -> JComponent
): ToolWindow {

    newDisposable(disposable) {
        ToolWindowManager.getInstance(project).unregisterToolWindow(toolWindowId)
    }

    val manager = ToolWindowManager.getInstance(project)
    if (manager.getToolWindow(toolWindowId) != null) {
        manager.unregisterToolWindow(toolWindowId)
    }

    val component =
        if (toolbarActionGroup == null) {
            createComponent()
        } else {
            SimpleToolWindowPanel(true).let {
                it.setContent(createComponent())
                it.setToolbar(ActionManager.getInstance().createActionToolbar(ActionPlaces.EDITOR_TOOLBAR, toolbarActionGroup, true).component)
                it
            }
        }

    val toolWindow = manager.registerToolWindow(toolWindowId, false, location)
    val content = ContentFactory.SERVICE.getInstance().createContent(component, "", false)
    toolWindow.contentManager.addContent(content)
    return toolWindow
}

fun Disposable.createChild() = newDisposable(parents = this, onDisposal = {})

fun newDisposable(vararg parents: Disposable, onDisposal: () -> Unit = {}) = newDisposable(parents.toList(), onDisposal)

fun newDisposable(parents: Collection<Disposable>, onDisposal: () -> Unit = {}): Disposable {
    val isDisposed = AtomicBoolean(false)
    val disposable = Disposable {
        val wasUpdated = isDisposed.compareAndSet(false, true)
        if (wasUpdated) onDisposal()
    }
    parents.forEach { parent ->
        // can't use here "Disposer.register(parent, disposable)"
        // because Disposer only allows one parent to one child registration of Disposable objects
        Disposer.register(parent, Disposable {
            Disposer.dispose(disposable)
        })
    }
    return disposable
}
