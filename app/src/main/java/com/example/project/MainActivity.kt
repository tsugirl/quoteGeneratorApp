package com.example.project

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import com.example.project.FavoritesActivity

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        setupButtons()
    }

    private fun setupButtons() {
        val btnToMain: Button = findViewById(R.id.btn_to_main_screen)
        val btnToFavorites: Button = findViewById(R.id.btn_to_favorites_screen)

        btnToMain.setOnClickListener {
        }

        btnToFavorites.setOnClickListener {
            startActivity(Intent(this, FavoritesActivity::class.java))
            finish()
        }
    }
}