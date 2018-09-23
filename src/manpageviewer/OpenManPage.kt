package manpageviewer

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.fileEditor.FileEditorManager

class OpenManPage : AnAction() {
    override fun actionPerformed(event: AnActionEvent) {
        val project = event.project ?: return
        val editor = FileEditorManager.getInstance(project).selectedTextEditor ?: return
        val term = editor.termUnderCaret()
        if (term.isEmpty()) return

        val manText = readManPage(term)

        showToolWindow(manText, term, project)
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
}
