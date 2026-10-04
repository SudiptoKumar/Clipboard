package com.sudipto.longpaste.ui

import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.view.Gravity
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import com.sudipto.longpaste.R

class MainActivity : android.app.Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(buildUi())
    }

    private fun buildUi(): View {
        val scroll = ScrollView(this)
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            setPadding(32, 48, 32, 48)
            setBackgroundColor(getColor(R.color.lp_background))
        }

        val icon = TextView(this).apply {
            text = "▣"
            textSize = 56f
            setTextColor(getColor(R.color.lp_accent))
            gravity = Gravity.CENTER
        }
        root.addView(icon, LinearLayout.LayoutParams(-1, 90))

        val title = TextView(this).apply {
            text = getString(R.string.app_name)
            textSize = 28f
            setTextColor(getColor(R.color.lp_text))
            gravity = Gravity.CENTER
        }
        root.addView(title)

        val subtitle = TextView(this).apply {
            text = "Large-text clipboard keyboard\nLocal-first • No cloud • No ads"
            textSize = 15f
            setTextColor(getColor(R.color.lp_muted))
            gravity = Gravity.CENTER
        }
        root.addView(subtitle, LinearLayout.LayoutParams(-1, 90))

        root.addView(actionButton("1. Enable keyboard") {
            startActivity(Intent(Settings.ACTION_INPUT_METHOD_SETTINGS))
        })

        root.addView(actionButton("2. Select LongPaste Keyboard") {
            val manager = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
            manager.showInputMethodPicker()
        })

        root.addView(actionButton("Open keyboard settings") {
            startActivity(Intent(Settings.ACTION_INPUT_METHOD_SETTINGS))
        })

        val info = TextView(this).apply {
            text = "Clipboard history stays on-device. Large entries are inserted in 16 KiB chunks. The first release targets reliable 1 MiB plain-text handling."
            textSize = 14f
            setTextColor(getColor(R.color.lp_muted))
            setPadding(0, 32, 0, 0)
        }
        root.addView(info)
        scroll.addView(root)
        return scroll
    }

    private fun actionButton(label: String, action: () -> Unit): Button = Button(this).apply {
        text = label
        isAllCaps = false
        setTextColor(getColor(R.color.lp_white))
        setBackgroundResource(R.drawable.action_background)
        setOnClickListener { action() }
        layoutParams = LinearLayout.LayoutParams(-1, 56).apply { setMargins(0, 10, 0, 10) }
    }
}
