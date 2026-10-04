package com.sudipto.longpaste.ui

import android.app.Activity
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.provider.Settings
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import com.sudipto.longpaste.R

class MainActivity : Activity() {
    private val dp: Float get() = resources.displayMetrics.density

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = getColor(R.color.lp_background)
        window.navigationBarColor = getColor(R.color.lp_background)
        window.decorView.systemUiVisibility = 0
        setContentView(buildUi())
    }

    override fun onResume() {
        super.onResume()
        setContentView(buildUi())
    }

    private fun buildUi(): View {
        val scroll = ScrollView(this).apply {
            setBackgroundColor(getColor(R.color.lp_background))
            isFillViewport = true
        }

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(20), dp(28), dp(20), dp(28))
        }
        scroll.addView(root, ViewGroup.LayoutParams(-1, -1))

        val header = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        val icon = ImageView(this).apply {
            setImageResource(R.drawable.ic_logo)
            contentDescription = getString(R.string.app_name)
        }
        header.addView(icon, LinearLayout.LayoutParams(dp(64), dp(64)))

        val titleWrap = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(14), 0, 0, 0)
        }
        titleWrap.addView(textView("LongPaste Keyboard", 25f, R.color.lp_text, false))
        titleWrap.addView(textView("Local clipboard. Long text. One keyboard.", 14f, R.color.lp_muted, false).apply {
            setPadding(0, dp(3), 0, 0)
        })
        header.addView(titleWrap, LinearLayout.LayoutParams(0, -2, 1f))
        root.addView(header)

        val hero = card().apply {
            setPadding(dp(16), dp(16), dp(16), dp(16))
        }
        hero.addView(textView("SET UP ONCE", 11f, R.color.lp_accent, true))
        hero.addView(textView("Make LongPaste your keyboard, then open it anywhere you need to paste saved text.", 16f, R.color.lp_text, false).apply {
            setPadding(0, dp(8), 0, dp(4))
        })
        hero.addView(textView("The app stores clipboard history only on this device. No cloud. No ads. No Internet permission.", 13f, R.color.lp_muted, false))
        root.addView(hero, LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(22) })

        root.addView(sectionLabel("KEYBOARD STATUS"), LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(22) })
        val statusCard = card()
        val statusRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        val statusDot = TextView(this).apply {
            text = "●"
            textSize = 16f
            setTextColor(getColor(if (isImeEnabled()) R.color.lp_accent else R.color.lp_muted))
            gravity = Gravity.CENTER
        }
        statusRow.addView(statusDot, LinearLayout.LayoutParams(dp(28), dp(28)))
        val statusText = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        statusText.addView(textView(
            if (isImeEnabled()) "LongPaste is enabled" else "LongPaste is not enabled",
            15f,
            R.color.lp_text,
            true
        ))
        statusText.addView(textView(
            if (isImeEnabled()) "Open any text field and switch to LongPaste from the keyboard picker."
            else "Enable it in Android's available keyboard settings.",
            12f,
            R.color.lp_muted,
            false
        ).apply { setPadding(0, dp(3), 0, 0) })
        statusRow.addView(statusText, LinearLayout.LayoutParams(0, -2, 1f))
        statusCard.addView(statusRow)
        root.addView(statusCard, LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(8) })

        root.addView(primaryAction("Enable / manage keyboards", "Open Android's keyboard settings") {
            startActivity(Intent(Settings.ACTION_INPUT_METHOD_SETTINGS))
        }, LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(14) })

        root.addView(primaryAction("Choose LongPaste keyboard", "Open the keyboard picker") {
            val manager = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
            manager.showInputMethodPicker()
        }, LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(10) })

        root.addView(sectionLabel("QUICK TEST"), LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(24) })
        val testCard = card().apply { setPadding(dp(12), dp(12), dp(12), dp(12)) }
        val testEditor = EditText(this).apply {
            hint = "Tap here, then choose LongPaste"
            setSingleLine(false)
            minLines = 2
            maxLines = 4
            textSize = 15f
            setTextColor(getColor(R.color.lp_text))
            setHintTextColor(getColor(R.color.lp_muted))
            background = rounded(getColor(R.color.lp_surface_2), dp(12))
            setPadding(dp(12), dp(10), dp(12), dp(10))
        }
        testCard.addView(testEditor, LinearLayout.LayoutParams(-1, dp(92)))
        testCard.addView(primaryAction("Open keyboard picker", "Switch the test field to LongPaste") {
            testEditor.requestFocus()
            val manager = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
            manager.showInputMethodPicker()
        }, LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(10) })
        root.addView(testCard, LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(8) })

        root.addView(sectionLabel("WHAT YOU GET"), LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(24) })
        val features = card()
        listOf(
            "Clipboard history" to "Keep recent text snippets available inside the keyboard.",
            "Large-text paste" to "Insert long plain text in 16 KiB chunks instead of one huge commit.",
            "Search, pin, delete" to "Manage your saved snippets without leaving the current app.",
            "Password-field protection" to "Clipboard capture is suppressed for common sensitive input types."
        ).forEachIndexed { index, pair ->
            val row = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
            }
            row.addView(textView(pair.first, 14f, R.color.lp_text, true))
            row.addView(textView(pair.second, 12f, R.color.lp_muted, false).apply {
                setPadding(0, dp(3), 0, 0)
            })
            features.addView(row)
            if (index != 3) {
                features.addView(divider(), LinearLayout.LayoutParams(-1, dp(1)).apply {
                    topMargin = dp(12)
                    bottomMargin = dp(12)
                })
            }
        }
        root.addView(features, LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(8) })

        root.addView(sectionLabel("TEST THE KEYBOARD"), LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(24) })
        val test = card()
        test.addView(textView("1", 18f, R.color.lp_accent, true))
        test.addView(textView("Open any app with a text field. Tap the keyboard picker and select LongPaste. You should see a full QWERTY keyboard with a clipboard shelf above it.", 13f, R.color.lp_text, false).apply {
            setPadding(0, dp(5), 0, 0)
        })
        root.addView(test, LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(8) })

        root.addView(textView(
            "LongPaste Keyboard v1.1 • Local-first • No ads • No Internet access",
            12f,
            R.color.lp_muted,
            false
        ).apply {
            gravity = Gravity.CENTER
            setPadding(0, dp(26), 0, dp(8))
        })

        return scroll
    }

    private fun isImeEnabled(): Boolean {
        val manager = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
        val packageName = packageName
        return manager.enabledInputMethodList.any { it.packageName == packageName }
    }

    private fun sectionLabel(label: String): TextView = textView(label, 11f, R.color.lp_accent, true)

    private fun primaryAction(title: String, subtitle: String, action: () -> Unit): View {
        val container = card(fill = getColor(R.color.lp_accent)).apply {
            isClickable = true
            isFocusable = true
            setOnClickListener { action() }
            setPadding(dp(16), dp(13), dp(16), dp(13))
        }
        container.addView(textViewColor(title, 15f, Color.WHITE, true))
        container.addView(textViewColor(subtitle, 12f, Color.argb(210, 255, 255, 255), false).apply {
            setPadding(0, dp(3), 0, 0)
        })
        return container
    }

    private fun card(fill: Int = getColor(R.color.lp_surface)): LinearLayout = LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL
        background = rounded(fill, dp(18))
    }

    private fun divider(): View = View(this).apply { setBackgroundColor(getColor(R.color.lp_surface_2)) }

    private fun textView(text: String, size: Float, colorRes: Int, bold: Boolean): TextView = TextView(this).apply {
        this.text = text
        textSize = size
        setTextColor(getColor(colorRes))
        if (bold) setTypeface(typeface, android.graphics.Typeface.BOLD)
    }

    private fun textViewColor(text: String, size: Float, color: Int, bold: Boolean): TextView = TextView(this).apply {
        this.text = text
        textSize = size
        setTextColor(color)
        if (bold) setTypeface(typeface, android.graphics.Typeface.BOLD)
    }

    private fun rounded(fill: Int, radius: Int): GradientDrawable = GradientDrawable().apply {
        shape = GradientDrawable.RECTANGLE
        setColor(fill)
        cornerRadius = radius.toFloat()
    }

    private fun dp(value: Int): Int = (value * dp).toInt()
}
