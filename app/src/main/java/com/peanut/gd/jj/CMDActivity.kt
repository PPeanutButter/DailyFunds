package com.peanut.gd.jj

import android.app.Activity
import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.view.inputmethod.EditorInfo
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView

open class CMDActivity : Activity() {
    private var onTextInputListener: TextInputListener? = null
    private lateinit var li:LinearLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_cmd)
        li = findViewById(R.id.li)
        findViewById<EditText>(R.id.editTextTextMultiLine).apply {
            this.setOnEditorActionListener { _, actionId, _ ->
                if (actionId == EditorInfo.IME_ACTION_DONE) {
                    onTextInputListener?.onInput(this.text.toString())
                    this.text.clear()
                    return@setOnEditorActionListener true
                }
                return@setOnEditorActionListener false
            }
        }
    }

    protected fun printV(a: String) = print(a, Color.WHITE)
    protected fun printI(a: String) = print(a, Color.GREEN)
    protected fun printE(a: String) = print(a, Color.RED)
    protected fun printW(a: String) = print(a, Color.YELLOW)
//    protected fun printD(a: String) = print(a, Color.CYAN)

    protected fun registerInputListener(func: (String) -> Unit) {
        this.onTextInputListener = object : TextInputListener {
            override fun onInput(s: String) {
                func.invoke(s)
            }
        }
    }

    private fun print(a: String, c: Int) {
        li.addView(TextView(this).apply {
            this.text = a
            this.textSize = 12f
            this.setTextColor(c)
            this.setTextIsSelectable(true)
            this.typeface = Typeface.createFromAsset(this@CMDActivity.assets, "ubuntu_mono.ttf")
        }, li.childCount - 1)
    }

    fun String.toFixedLengthString(l: Int?): String {
        if (l == null)
            return this
        return if (this.realLength() > l)
            this.substring(0, l - 3) + "..."
        else {
            this + " ".repeat(l - this.realLength())
        }
    }

    private fun String.realLength(): Int {
        var valueLength = 0
        for (i in this)
            valueLength += if (i.toString().matches(Regex("[\u4e00-\u9fa5]"))) 2 else 1
        return valueLength
    }
}

interface TextInputListener {
    fun onInput(s: String)
}