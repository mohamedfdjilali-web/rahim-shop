package com.rahimshop.admin

import android.app.Activity
import android.os.Bundle
import android.graphics.Color
import android.view.Gravity
import android.widget.TextView

class MainActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val text = TextView(this)

        text.text = "Rahim Shop\n\nالتطبيق يعمل بنجاح ✅"
        text.textSize = 24f
        text.setTextColor(Color.BLACK)
        text.gravity = Gravity.CENTER

        setContentView(text)
    }
}
