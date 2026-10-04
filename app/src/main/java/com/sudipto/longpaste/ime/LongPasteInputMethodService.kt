package com.sudipto.longpaste.ime

import android.content.ClipDescription
import android.content.ClipboardManager
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.RippleDrawable
import android.inputmethodservice.InputMethodService
import android.os.Handler
import android.text.InputType
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.ExtractedTextRequest
import android.view.KeyEvent
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.HorizontalScrollView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import com.sudipto.longpaste.R
import com.sudipto.longpaste.data.ClipboardDatabase
import com.sudipto.longpaste.data.ClipboardItem
import com.sudipto.longpaste.util.LargeTextChunker
import com.sudipto.longpaste.util.TextStats
import java.util.Locale
import java.util.concurrent.Executors

class LongPasteInputMethodService : InputMethodService() {
    private enum class Mode { KEYBOARD, CLIPBOARD }

    private lateinit var clipboardManager: ClipboardManager
    private lateinit var database: ClipboardDatabase
    private val dbExecutor = Executors.newSingleThreadExecutor()
    private val mainHandler by lazy { Handler(mainLooper) }
    private var keyboardRoot: LinearLayout? = null
    private var mode = Mode.KEYBOARD
    private var shifted = true
    private var symbols = false
    private var searchQuery = ""

    private val clipboardListener = ClipboardManager.OnPrimaryClipChangedListener { capturePrimaryClip() }

    override fun onCreate() {
        super.onCreate()
        clipboardManager = getSystemService(CLIPBOARD_SERVICE) as ClipboardManager
        database = ClipboardDatabase(this)
        clipboardManager.addPrimaryClipChangedListener(clipboardListener)
        capturePrimaryClip()
    }

    override fun onDestroy() {
        if (::clipboardManager.isInitialized) {
            clipboardManager.removePrimaryClipChangedListener(clipboardListener)
        }
        dbExecutor.shutdownNow()
        if (::database.isInitialized) database.close()
        super.onDestroy()
    }

    override fun onCreateInputView(): View {
        keyboardRoot = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(getColor(R.color.lp_background))
            setPadding(dp(6), dp(5), dp(6), dp(4))
            layoutParams = ViewGroup.LayoutParams(-1, -2)
        }
        renderKeyboard()
        return keyboardRoot!!
    }

    override fun onStartInput(attribute: EditorInfo?, restarting: Boolean) {
        super.onStartInput(attribute, restarting)
        shifted = true
        symbols = false
        mode = Mode.KEYBOARD
    }

    override fun onStartInputView(info: EditorInfo?, restarting: Boolean) {
        super.onStartInputView(info, restarting)
        mode = Mode.KEYBOARD
        renderKeyboard()
    }

    override fun onEvaluateFullscreenMode(): Boolean = false

    private fun renderKeyboard() {
        val root = keyboardRoot ?: return
        root.removeAllViews()

        when (mode) {
            Mode.KEYBOARD -> renderKeyboardMode(root)
            Mode.CLIPBOARD -> renderClipboardMode(root)
        }
    }

    private fun renderKeyboardMode(root: LinearLayout) {
        val toolbar = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL }
        toolbar.addView(key("LongPaste", 12f, flex = 1.25f, green = true) { renderClipboardModeDirect() })
        toolbar.addView(key("COPY", 11f, flex = 0.85f) { copyCurrentField() })
        toolbar.addView(key("↔", 16f, flex = 0.55f) { switchKeyboard() })
        toolbar.addView(key("⌫", 15f, flex = 0.65f) { backspace() })
        root.addView(toolbar, rowParams(48))

        addClipboardShelf(root)

        if (symbols) {
            addSymbolRows(root)
        } else {
            addLetterRows(root)
        }

        val bottom = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL }
        bottom.addView(key("123", 10f, flex = 0.9f) { symbols = !symbols; renderKeyboard() })
        bottom.addView(key(",", 14f) { commitText(",") })
        bottom.addView(key("SPACE", 11f, flex = 3.8f) { commitText(" ") })
        bottom.addView(key(".", 14f) { commitText(".") })
        bottom.addView(key("↵", 18f, flex = 1.05f) { enter() })
        root.addView(bottom, rowParams(48))
    }

    private fun addLetterRows(root: LinearLayout) {
        addKeyRow(root, "QWERTYUIOP")
        addKeyRow(root, "ASDFGHJKL")

        val row = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL }
        row.addView(key(if (shifted) "⇧" else "⇩", 17f, flex = 1.25f, green = shifted) {
            shifted = !shifted
            renderKeyboard()
        })
        "ZXCVBNM".forEach { c ->
            row.addView(key(displayChar(c), 15f) { commitText(displayChar(c)) })
        }
        row.addView(key("⌫", 18f, flex = 1.25f) { backspace() })
        root.addView(row, rowParams(48))
    }

    private fun addSymbolRows(root: LinearLayout) {
        addKeyRow(root, "1234567890")
        addKeyRow(root, "@#$%&*-+")
        addKeyRow(root, "()_!?/;:")
    }

    private fun addKeyRow(root: LinearLayout, labels: String) {
        val row = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL }
        labels.forEach { c ->
            val label = if (!symbols && c.isLetter()) displayChar(c) else c.toString()
            row.addView(key(label, if (label.length > 1) 11f else 15f) {
                commitText(if (!symbols && c.isLetter()) displayChar(c) else c.toString())
            })
        }
        root.addView(row, rowParams(48))
    }

    private fun addClipboardShelf(root: LinearLayout) {
        val shelf = HorizontalScrollView(this).apply {
            isHorizontalScrollBarEnabled = false
            overScrollMode = View.OVER_SCROLL_NEVER
        }
        val itemsRow = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        shelf.addView(itemsRow, FrameLayout.LayoutParams(-2, -1))
        root.addView(shelf, rowParams(58))

        dbExecutor.execute {
            val items = database.list(5, "")
            mainHandler.post {
                itemsRow.removeAllViews()
                if (items.isEmpty()) {
                    itemsRow.addView(key("No saved clips yet", 11f, flex = 1.7f, enabled = false) { })
                } else {
                    items.take(4).forEach { item ->
                        val label = TextStats.preview(item.content, 34)
                        val button = key(label, 11f, fixedWidth = dp(172), multiLine = true) {
                            pasteItem(item.id)
                        }
                        itemsRow.addView(button)
                    }
                    itemsRow.addView(key("ALL", 10f, fixedWidth = dp(62), green = true) { renderClipboardModeDirect() })
                }
            }
        }
    }

    private fun renderClipboardModeDirect() {
        mode = Mode.CLIPBOARD
        renderKeyboard()
    }

    private fun renderClipboardMode(root: LinearLayout) {
        val header = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL }
        header.addView(key("⌨", 18f, flex = 0.7f, green = true) {
            mode = Mode.KEYBOARD
            renderKeyboard()
        })
        header.addView(key("COPY TOP", 10f, flex = 1.2f) { copyTopResult() })
        header.addView(key("CLEAR", 10f, flex = 0.9f) { clearHistoryAndRefresh() })
        root.addView(header, rowParams(48))

        val search = EditText(this).apply {
            hint = "Search clipboard"
            setSingleLine(true)
            setText(searchQuery)
            setTextColor(getColor(R.color.lp_text))
            setHintTextColor(getColor(R.color.lp_muted))
            textSize = 13f
            background = rounded(R.color.lp_surface_2, 12)
            setPadding(dp(14), 0, dp(14), 0)
            addTextChangedListener(object : android.text.TextWatcher {
                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit
                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                    searchQuery = s?.toString().orEmpty()
                    refreshClipboardRows(root)
                }
                override fun afterTextChanged(s: android.text.Editable?) = Unit
            })
        }
        root.addView(search, LinearLayout.LayoutParams(-1, dp(44)).apply {
            topMargin = dp(5)
            bottomMargin = dp(5)
        })

        val scroll = ScrollView(this).apply { overScrollMode = View.OVER_SCROLL_IF_CONTENT_SCROLLS }
        val list = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(0, dp(2), 0, dp(2))
        }
        scroll.addView(list)
        root.addView(scroll, LinearLayout.LayoutParams(-1, 0, 1f))
        refreshClipboardRows(root)
    }

    private fun refreshClipboardRows(root: LinearLayout) {
        val scroll = root.children().firstOrNull { it is ScrollView } as? ScrollView ?: return
        val list = scroll.getChildAt(0) as? LinearLayout ?: return
        list.removeAllViews()
        val querySnapshot = searchQuery
        dbExecutor.execute {
            val items = database.list(80, querySnapshot)
            mainHandler.post {
                list.removeAllViews()
                if (items.isEmpty()) {
                    val empty = TextView(this).apply {
                        text = if (querySnapshot.isBlank()) "No clipboard history yet" else "No matches for \"$querySnapshot\""
                        textSize = 13f
                        setTextColor(getColor(R.color.lp_muted))
                        gravity = Gravity.CENTER
                        setPadding(dp(12), dp(24), dp(12), dp(24))
                    }
                    list.addView(empty, LinearLayout.LayoutParams(-1, dp(84)))
                } else {
                    items.forEach { item -> list.addView(clipboardCard(item)) }
                }
            }
        }
    }

    private fun clipboardCard(item: ClipboardItem): View {
        val card = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            background = rounded(R.color.lp_surface, 14)
            setPadding(dp(12), dp(9), dp(12), dp(9))
        }
        card.addView(TextView(this).apply {
            text = item.title?.takeIf { it.isNotBlank() } ?: TextStats.preview(item.content, 88)
            textSize = 13f
            setTextColor(getColor(R.color.lp_text))
            maxLines = 2
        })
        card.addView(TextView(this).apply {
            text = "${item.characterCount} chars" + if (item.isPinned) " • pinned" else ""
            textSize = 10f
            setTextColor(getColor(R.color.lp_muted))
            setPadding(0, dp(3), 0, dp(4))
        })

        val actions = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        actions.addView(actionKey(if (item.isPinned) "UNPIN" else "PIN") {
            dbExecutor.execute {
                database.setPinned(item.id, !item.isPinned)
                mainHandler.post { keyboardRoot?.let { refreshClipboardRows(it) } }
            }
        })
        actions.addView(actionKey("PASTE") { pasteItem(item.id) })
        actions.addView(actionKey("COPY") { copyItemToSystemClipboard(item.id) })
        actions.addView(actionKey("DEL") {
            dbExecutor.execute {
                database.delete(item.id)
                mainHandler.post { keyboardRoot?.let { refreshClipboardRows(it) } }
            }
        })
        card.addView(actions, LinearLayout.LayoutParams(-1, dp(34)))

        return card.apply {
            isClickable = true
            isFocusable = true
            setOnClickListener { pasteItem(item.id) }
            layoutParams = LinearLayout.LayoutParams(-1, -2).apply { bottomMargin = dp(6) }
        }
    }

    private fun actionKey(label: String, action: () -> Unit): TextView = key(label, 9f, flex = 1f, action = action)

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

    private fun clearHistoryAndRefresh() {
        dbExecutor.execute {
            database.clear()
            mainHandler.post { keyboardRoot?.let { refreshClipboardRows(it) } }
        }
    }

    private fun pasteItem(id: Long) {
        dbExecutor.execute {
            val item = database.get(id) ?: return@execute
            mainHandler.post {
                pasteLargeText(item.content)
            }
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
        if (text.isBlank()) return
        dbExecutor.execute { database.insertOrTouch(text) }
    }

    private fun isSensitiveEditor(): Boolean {
        val type = currentInputEditorInfo?.inputType ?: return false
        val clazz = type and InputType.TYPE_MASK_CLASS
        val variation = type and InputType.TYPE_MASK_VARIATION
        if (clazz == InputType.TYPE_CLASS_TEXT) {
            return variation == InputType.TYPE_TEXT_VARIATION_PASSWORD ||
                variation == InputType.TYPE_TEXT_VARIATION_WEB_PASSWORD ||
                variation == InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
        }
        if (clazz == InputType.TYPE_CLASS_NUMBER) {
            return variation == InputType.TYPE_NUMBER_VARIATION_PASSWORD
        }
        return false
    }

    private fun pasteLargeText(text: String) {
        val connection = currentInputConnection ?: return
        if (text.isEmpty()) return
        connection.beginBatchEdit()
        try {
            LargeTextChunker.chunks(text).forEach { chunk ->
                connection.commitText(chunk, 1)
            }
        } finally {
            connection.endBatchEdit()
        }
    }

    private fun copyCurrentField() {
        val connection = currentInputConnection ?: return
        val request = ExtractedTextRequest().apply {
            flags = 0
            hintMaxChars = 1_000_000
            hintMaxLines = 100_000
        }
        val extracted = connection.getExtractedText(request, 0)?.text?.toString().orEmpty()
        if (extracted.isNotEmpty()) {
            clipboardManager.setPrimaryClip(android.content.ClipData.newPlainText("LongPaste", extracted))
        }
    }

    private fun backspace() {
        currentInputConnection?.deleteSurroundingText(1, 0)
    }

    private fun switchKeyboard() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
            if (!switchToNextInputMethod(false)) {
                (getSystemService(INPUT_METHOD_SERVICE) as android.view.inputmethod.InputMethodManager).showInputMethodPicker()
            }
        } else {
            (getSystemService(INPUT_METHOD_SERVICE) as android.view.inputmethod.InputMethodManager).showInputMethodPicker()
        }
    }

    private fun commitText(text: String) {
        val wasShifted = shifted
        currentInputConnection?.commitText(text, 1)
        if (!symbols && text.length == 1 && text.first().isLetter() && wasShifted) {
            shifted = false
            renderKeyboard()
        }
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

    private fun displayChar(c: Char): String = if (shifted) c.toString() else c.lowercase(Locale.US)

    private fun key(
        label: String,
        textSize: Float,
        flex: Float = 1f,
        fixedWidth: Int? = null,
        green: Boolean = false,
        enabled: Boolean = true,
        multiLine: Boolean = false,
        action: () -> Unit
    ): TextView {
        val view = TextView(this).apply {
            text = label
            this.textSize = textSize
            setTextColor(if (green) Color.WHITE else getColor(R.color.lp_text))
            gravity = Gravity.CENTER
            setTypeface(typeface, android.graphics.Typeface.BOLD)
            maxLines = if (multiLine) 2 else 1
            if (multiLine) ellipsize = android.text.TextUtils.TruncateAt.END
            isEnabled = enabled
            isClickable = enabled
            isFocusable = enabled
            contentDescription = label
            background = pressableBackground(if (green) getColor(R.color.lp_accent) else getColor(R.color.lp_surface_2), if (green) getColor(R.color.lp_accent) else getColor(R.color.lp_surface))
            setOnClickListener { if (enabled) action() }
        }
        val lp = if (fixedWidth != null) {
            LinearLayout.LayoutParams(fixedWidth, -1)
        } else {
            LinearLayout.LayoutParams(0, -1, flex)
        }
        lp.setMargins(dp(2), dp(2), dp(2), dp(2))
        view.layoutParams = lp
        if (!enabled) view.alpha = 0.55f
        return view
    }

    private fun rowParams(height: Int): LinearLayout.LayoutParams = LinearLayout.LayoutParams(-1, dp(height))

    private fun pressableBackground(normal: Int, pressed: Int): RippleDrawable {
        val normalDrawable = GradientDrawable().apply {
            setColor(normal)
            cornerRadius = dp(8).toFloat()
        }
        val mask = GradientDrawable().apply {
            setColor(Color.WHITE)
            cornerRadius = dp(8).toFloat()
        }
        return RippleDrawable(ColorStateList.valueOf(pressed), normalDrawable, mask)
    }

    private fun rounded(colorRes: Int, radius: Int): GradientDrawable = GradientDrawable().apply {
        setColor(getColor(colorRes))
        cornerRadius = dp(radius).toFloat()
    }

    private fun LinearLayout.children(): Sequence<View> = sequence {
        for (i in 0 until childCount) yield(getChildAt(i))
    }

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()
}
