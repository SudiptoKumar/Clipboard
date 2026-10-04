package com.sudipto.longpaste.ime

import android.content.ClipDescription
import android.content.ClipboardManager
import android.inputmethodservice.InputMethodService
import android.os.Handler
import android.text.InputType
import android.text.Editable
import android.text.TextWatcher
import android.view.KeyEvent
import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import com.sudipto.longpaste.R
import com.sudipto.longpaste.data.ClipboardDatabase
import com.sudipto.longpaste.data.ClipboardItem
import com.sudipto.longpaste.util.LargeTextChunker
import com.sudipto.longpaste.util.TextStats
import java.util.concurrent.Executors

class LongPasteInputMethodService : InputMethodService() {
    private lateinit var clipboardManager: ClipboardManager
    private lateinit var database: ClipboardDatabase
    private val dbExecutor = Executors.newSingleThreadExecutor()
    private var keyboardRoot: LinearLayout? = null
    private val mainHandler by lazy { Handler(mainLooper) }
    private var searchQuery: String = ""

    private val clipboardListener = ClipboardManager.OnPrimaryClipChangedListener {
        capturePrimaryClip()
    }

    override fun onCreate() {
        super.onCreate()
        clipboardManager = getSystemService(CLIPBOARD_SERVICE) as ClipboardManager
        database = ClipboardDatabase(this)
        clipboardManager.addPrimaryClipChangedListener(clipboardListener)
        capturePrimaryClip()
    }

    override fun onDestroy() {
        clipboardManager.removePrimaryClipChangedListener(clipboardListener)
        dbExecutor.shutdownNow()
        database.close()
        super.onDestroy()
    }

    override fun onCreateInputView(): View = buildKeyboard()

    private fun buildKeyboard(): View {
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(8, 8, 8, 8)
            setBackgroundColor(getColor(R.color.lp_background))
        }

        val toolbar = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        toolbar.addView(actionButton("Clipboard") { showClipboardPanel() }, 0, LinearLayout.LayoutParams(0, 52, 2f))
        toolbar.addView(actionButton("Copy") { copyCurrentField() }, 1, LinearLayout.LayoutParams(0, 52, 1f))
        toolbar.addView(actionButton("⌫") { currentInputConnection?.deleteSurroundingText(1, 0) }, 2, LinearLayout.LayoutParams(0, 52, 1f))
        root.addView(toolbar)

        listOf("QWERTYUIOP", "ASDFGHJKL", "ZXCVBNM").forEach { row ->
            val rowLayout = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
            row.forEach { char ->
                rowLayout.addView(keyButton(char.toString()) { commitText(char.toString()) })
            }
            root.addView(rowLayout, LinearLayout.LayoutParams(-1, 52))
        }

        val bottom = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        bottom.addView(keyButton(",") { commitText(",") })
        bottom.addView(keyButton("Space", weight = 4f) { commitText(" ") })
        bottom.addView(keyButton(".") { commitText(".") })
        bottom.addView(keyButton("↵") { enter() })
        root.addView(bottom, LinearLayout.LayoutParams(-1, 52))

        keyboardRoot = root
        return root
    }

    private fun showClipboardPanel() {
        val root = keyboardRoot ?: return
        root.removeAllViews()

        val header = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        header.addView(actionButton("⌨ Keyboard") { setInputView(buildKeyboard()) }, 0, LinearLayout.LayoutParams(0, 52, 1.7f))
        header.addView(actionButton("Copy Top") { copyTopResult() }, 1, LinearLayout.LayoutParams(0, 52, 1.1f))
        header.addView(actionButton("Clear") { clearHistoryAndRefresh(root) }, 2, LinearLayout.LayoutParams(0, 52, 1f))
        root.addView(header)

        val search = EditText(this).apply {
            hint = "Search clipboard"
            setText(searchQuery)
            setSingleLine(true)
            setTextColor(getColor(R.color.lp_text))
            setHintTextColor(getColor(R.color.lp_muted))
            setBackgroundResource(R.drawable.key_background)
            addTextChangedListener(object : TextWatcher {
                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit
                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                    searchQuery = s?.toString().orEmpty()
                    refreshClipboardRows(root)
                }
                override fun afterTextChanged(s: Editable?) = Unit
            })
        }
        root.addView(search, LinearLayout.LayoutParams(-1, 52).apply { setMargins(0, 6, 0, 6) })

        val scroll = ScrollView(this)
        val list = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(4, 4, 4, 4)
        }
        scroll.addView(list)
        root.addView(scroll, LinearLayout.LayoutParams(-1, 0, 1f))
        refreshClipboardRows(root)
    }

    private fun refreshClipboardRows(root: LinearLayout) {
        val scroll = root.children().firstOrNull { it is ScrollView } as? ScrollView ?: return
        val list = scroll.getChildAt(0) as? LinearLayout ?: return
        list.removeAllViews()
        dbExecutor.execute {
            val items = database.list(100, searchQuery)
            mainHandler.post {
                if (items.isEmpty()) {
                    list.addView(TextView(this).apply {
                        text = if (searchQuery.isBlank()) "No clipboard items yet" else "No matches"
                        setTextColor(getColor(R.color.lp_muted))
                        textSize = 14f
                        setPadding(12, 24, 12, 24)
                    })
                } else {
                    items.forEach { item -> addClipboardRow(list, item, root) }
                }
            }
        }
    }

    private fun addClipboardRow(parent: LinearLayout, item: ClipboardItem, root: LinearLayout) {
        val row = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(12, 8, 12, 8)
            setBackgroundResource(R.drawable.surface_background)
        }
        val title = TextView(this).apply {
            text = item.title?.takeIf { it.isNotBlank() } ?: TextStats.preview(item.content, 72)
            setTextColor(getColor(R.color.lp_text))
            textSize = 14f
        }
        val charCount = TextStats.preview(item.content).length
        val meta = TextView(this).apply {
            text = "Preview $charCount+ chars${if (item.isPinned) " • Pinned" else ""}"
            setTextColor(getColor(R.color.lp_muted))
            textSize = 11f
        }
        row.addView(title)
        row.addView(meta)

        val actions = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        actions.addView(actionButton(if (item.isPinned) "Unpin" else "Pin") {
            dbExecutor.execute {
                database.setPinned(item.id, !item.isPinned)
                mainHandler.post { refreshClipboardRows(root) }
            }
        })
        actions.addView(actionButton("Copy") { copyItemToSystemClipboard(item.id) })
        actions.addView(actionButton("Paste") { pasteItem(item.id) })
        actions.addView(actionButton("Delete") {
            dbExecutor.execute {
                database.delete(item.id)
                mainHandler.post { refreshClipboardRows(root) }
            }
        })
        row.addView(actions, LinearLayout.LayoutParams(-1, 44))
        parent.addView(row, LinearLayout.LayoutParams(-1, 120).apply { bottomMargin = 6 })
    }

    private fun copyTopResult() {
        dbExecutor.execute {
            val item = database.list(1, searchQuery).firstOrNull() ?: return@execute
            mainHandler.post { copyItemToSystemClipboard(item.id) }
        }
    }

    private fun copyItemToSystemClipboard(id: Long) {
        dbExecutor.execute {
            val item = database.get(id) ?: return@execute
            clipboardManager.setPrimaryClip(android.content.ClipData.newPlainText("LongPaste", item.content))
        }
    }

    private fun clearHistoryAndRefresh(root: LinearLayout) {
        dbExecutor.execute {
            database.clear()
            mainHandler.post { refreshClipboardRows(root) }
        }
    }

    private fun pasteItem(id: Long) {
        dbExecutor.execute {
            val item = database.get(id) ?: return@execute
            pasteLargeText(item.content)
        }
    }

    private fun capturePrimaryClip() {
        if (!::clipboardManager.isInitialized || !clipboardManager.hasPrimaryClip()) return
        if (isSensitiveEditor()) return
        val description = clipboardManager.primaryClipDescription ?: return
        val isText = description.hasMimeType(ClipDescription.MIMETYPE_TEXT_PLAIN) ||
            description.hasMimeType(ClipDescription.MIMETYPE_TEXT_HTML)
        if (!isText) return
        val text = clipboardManager.primaryClip?.getItemAt(0)?.coerceToText(this)?.toString() ?: return
        if (text.isEmpty()) return
        dbExecutor.execute { database.insertOrTouch(text) }
    }

    private fun isSensitiveEditor(): Boolean {
        val type = currentInputEditorInfo?.inputType ?: return false
        if ((type and InputType.TYPE_MASK_CLASS) == InputType.TYPE_CLASS_TEXT) {
            val variation = type and InputType.TYPE_MASK_VARIATION
            if (variation == InputType.TYPE_TEXT_VARIATION_PASSWORD ||
                variation == InputType.TYPE_TEXT_VARIATION_WEB_PASSWORD ||
                variation == InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD) return true
        }
        if ((type and InputType.TYPE_MASK_CLASS) == InputType.TYPE_CLASS_NUMBER) {
            val variation = type and InputType.TYPE_MASK_VARIATION
            if (variation == InputType.TYPE_NUMBER_VARIATION_PASSWORD) return true
        }
        return false
    }

    private fun pasteLargeText(text: String) {
        mainHandler.post {
            val connection = currentInputConnection ?: return@post
            if (text.isEmpty()) return@post
            connection.beginBatchEdit()
            try {
                LargeTextChunker.chunks(text).forEach { chunk ->
                    connection.commitText(chunk, 1)
                }
            } finally {
                connection.endBatchEdit()
            }
            setInputView(buildKeyboard())
        }
    }

    private fun copyCurrentField() {
        val connection = currentInputConnection ?: return
        val request = android.view.inputmethod.ExtractedTextRequest().apply {
            flags = 0
            hintMaxChars = 1_000_000
            hintMaxLines = 100_000
        }
        val extracted = connection.getExtractedText(request, 0)?.text?.toString().orEmpty()
        if (extracted.isNotEmpty()) {
            clipboardManager.setPrimaryClip(android.content.ClipData.newPlainText("LongPaste", extracted))
        }
    }

    private fun commitText(text: String) {
        currentInputConnection?.commitText(text, 1)
    }

    private fun enter() {
        val connection = currentInputConnection ?: return
        val action = currentInputEditorInfo?.imeOptions?.and(EditorInfo.IME_MASK_ACTION) ?: EditorInfo.IME_ACTION_NONE
        if (action != EditorInfo.IME_ACTION_NONE && action != EditorInfo.IME_ACTION_UNSPECIFIED) {
            connection.performEditorAction(action)
        } else {
            connection.sendKeyEvent(KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_ENTER))
            connection.sendKeyEvent(KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_ENTER))
        }
    }

    private fun actionButton(label: String, action: () -> Unit): Button = Button(this).apply {
        text = label
        isAllCaps = false
        textSize = 11f
        setTextColor(getColor(R.color.lp_text))
        setBackgroundResource(R.drawable.key_background)
        setOnClickListener { action() }
    }

    private fun keyButton(label: String, weight: Float = 1f, action: () -> Unit): Button = Button(this).apply {
        text = label
        textSize = if (label.length > 1) 12f else 14f
        isAllCaps = false
        setTextColor(getColor(R.color.lp_text))
        setBackgroundResource(R.drawable.key_background)
        setOnClickListener { action() }
        layoutParams = LinearLayout.LayoutParams(0, -1, weight).apply { setMargins(2, 2, 2, 2) }
    }

    private fun LinearLayout.children(): Sequence<View> = sequence {
        for (i in 0 until childCount) yield(getChildAt(i))
    }
}
