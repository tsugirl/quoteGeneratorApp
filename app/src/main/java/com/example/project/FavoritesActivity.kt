package com.example.project

import android.app.ActivityOptions
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.project.model.Quote
import com.google.gson.Gson

class FavoritesActivity : AppCompatActivity() {

    private val PREFS = "quotes_prefs"
    private val KEY_FAVORITES = "favorites_json"

    private lateinit var recycler: RecyclerView
    private lateinit var adapter: FavoriteAdapter
    private val items = mutableListOf<Quote>() // список избранного

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_favorites)
        window.statusBarColor = android.graphics.Color.WHITE

        // recycler для списка объектов, хранящих цитаты
        recycler = findViewById(R.id.favorites_recycler)
        recycler.layoutManager = LinearLayoutManager(this)

        // настройка адаптера
        adapter = FavoriteAdapter(this, items) { quote ->
            removeFromStorage(quote)
            Toast.makeText(this, "Удалено из избранного", Toast.LENGTH_SHORT).show()
        }
        recycler.adapter = adapter

        // кнопки для перехода между экранами
        findViewById<Button>(R.id.btn_to_main_screen).setOnClickListener {
            val options = ActivityOptions.makeCustomAnimation(this, 0, 0)
            startActivity(Intent(this, MainActivity::class.java), options.toBundle())
            finish()
        }

        // отключение возможности перехода с избранного на избранное
        findViewById<Button>(R.id.btn_to_favorites_screen).setOnClickListener {

        }

        loadFromStorage()
    }

    private fun loadFromStorage() { // подгрузка цитат из SharedPreferences
        val prefs = getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val json = prefs.getString(KEY_FAVORITES, null)
        if (json != null) {
            val gson = Gson()
            val arr = gson.fromJson(json, Array<Quote>::class.java)
            items.clear()
            items.addAll(arr)
            adapter.notifyDataSetChanged()
        }
    }

    private fun removeFromStorage(q: Quote) { // удаление цитаты из SharedPreferences
        val prefs = getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val gson = Gson()
        val json = prefs.getString(KEY_FAVORITES, null)
        val list: MutableList<Quote> = if (json != null) {
            gson.fromJson(json, Array<Quote>::class.java).toMutableList()
        } else mutableListOf()
        val removed = list.removeAll { it.text == q.text }
        if (removed) {
            prefs.edit().putString(KEY_FAVORITES, gson.toJson(list)).apply()
        }

        // обновление локального и сетевого адаптера
        val idx = items.indexOfFirst { it.text == q.text }
        if (idx >= 0) {
            items.removeAt(idx)
            adapter.notifyItemRemoved(idx)
        }
    }
}
