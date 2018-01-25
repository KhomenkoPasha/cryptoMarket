package app.khom.pavlo.crypto.ui

import android.content.Intent
import android.support.v7.app.AppCompatActivity
import android.os.Bundle

import app.khom.pavlo.crypto.ui.main.MainActivity

class SplashActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        startActivity(Intent(this, MainActivity::class.java))
        finish()
    }
}
