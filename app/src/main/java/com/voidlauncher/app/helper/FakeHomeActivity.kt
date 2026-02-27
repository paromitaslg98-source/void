package com.voidlauncher.app.helper

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.voidlauncher.app.R

class FakeHomeActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_fake_home)
    }
}