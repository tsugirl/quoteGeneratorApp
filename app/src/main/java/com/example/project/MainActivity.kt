package com.example.project

import android.app.ActivityOptions
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.example.project.model.Quote
import com.google.gson.Gson
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.util.*

class MainActivity : AppCompatActivity() {

    // UI
    private lateinit var quoteText: TextView
    private lateinit var quoteAuthor: TextView
    private lateinit var btnShare: ImageButton
    private lateinit var btnFavorite: ImageButton
    private lateinit var btnPrev: ImageButton
    private lateinit var btnNext: ImageButton

    // хранение цитат
    private val history: LinkedList<Quote> = LinkedList()
    private var currentIndex = 0

    // ключи для получения жсона
    private val PREFS = "quotes_prefs"
    private val KEY_FAVORITES = "favorites_json"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        window.statusBarColor = android.graphics.Color.WHITE

        // получение UI по id'шкам
        quoteText = findViewById(R.id.quote_text)
        quoteAuthor = findViewById(R.id.quote_author)
        btnShare = findViewById(R.id.btn_share)
        btnFavorite = findViewById(R.id.btn_favorite)
        btnPrev = findViewById(R.id.btn_prev)
        btnNext = findViewById(R.id.btn_next)

        setupButtons()
        loadInitialQuote()
    }

    private fun setupButtons() {
        findViewById<Button>(R.id.btn_to_main_screen).setOnClickListener {
            fetchAndShowQuote() // получение цитаты
        }

        findViewById<Button>(R.id.btn_to_favorites_screen).setOnClickListener { // переход в избранное
            val options = ActivityOptions.makeCustomAnimation(this, 0, 0)
            startActivity(Intent(this, FavoritesActivity::class.java), options.toBundle())
        }

        btnShare.setOnClickListener {
            val q = getCurrentQuote() ?: return@setOnClickListener
            shareQuote(q) // делиться цитатой
        }

        btnFavorite.setOnClickListener { // добавление в избранное
            val q = getCurrentQuote() ?: return@setOnClickListener
            addToFavorites(q)
            Toast.makeText(this, "Добавлено в избранное", Toast.LENGTH_SHORT).show()
        }

        btnPrev.setOnClickListener { // проходка по истории
            if (currentIndex + 1 < history.size) {
                currentIndex += 1
                showQuoteFromHistory()
            } else {
                Toast.makeText(this, "Нет предыдущей цитаты", Toast.LENGTH_SHORT).show()
            }
        }

        btnNext.setOnClickListener { // переход к цитатам в истории от старых к новым или генерация новой цитаты
            if (currentIndex - 1 >= 0) {
                currentIndex -= 1
                showQuoteFromHistory()
            } else {
                fetchAndShowQuote()
            }
        }
    }

    private fun loadInitialQuote() { // получение первой цитаты при открытии стартового экрана
        fetchAndShowQuote()
    }

    private fun showQuoteFromHistory() { // индекс текущей цитаты, если смотрим историю
        val q = history.getOrNull(currentIndex)
        if (q != null) {
            quoteText.text = q.text
            quoteAuthor.text = q.author?.let { "— $it" } ?: ""
        }
    }

    private fun getCurrentQuote(): Quote? = history.getOrNull(currentIndex)

    private fun addToHistory(q: Quote) { // добавление в историю при пролистывании через стрелки
        if (history.isNotEmpty() && history.first.text == q.text) return // избегаю дубликатов
        history.addFirst(q)
        while (history.size > 5) history.removeLast()
        currentIndex = 0
        showQuoteFromHistory()
    }

    private fun shareQuote(q: Quote) { // делюсь цитатой
        val send = Intent().apply {
            action = Intent.ACTION_SEND
            putExtra(Intent.EXTRA_TEXT, "\"${q.text}\" — ${q.author ?: "Unknown"}")
            type = "text/plain"
        }
        startActivity(Intent.createChooser(send, "Поделиться цитатой"))
    }

    private fun addToFavorites(q: Quote) {  // добавление в избранное
        val prefs = getSharedPreferences(PREFS, Context.MODE_PRIVATE) // начиная с этой строки и до if - чтение уже находящихся цитат в избранном
        val json = prefs.getString(KEY_FAVORITES, null)
        val gson = Gson()
        val list: MutableList<Quote> = if (json != null) {
            gson.fromJson(json, Array<Quote>::class.java).toMutableList()
        } else {
            mutableListOf()
        }
        if (list.any { it.text == q.text }) return // борьба с дубликатами
        list.add(0, q)
        prefs.edit().putString(KEY_FAVORITES, gson.toJson(list)).apply()
    }

    private fun fetchAndShowQuote() { // заглушки если нет интернета или проблемы с API
        quoteText.text = "Загрузка..."
        quoteAuthor.text = ""
        CoroutineScope(Dispatchers.IO).launch {
            val q = fetchRandomQuote()
            withContext(Dispatchers.Main) {
                if (q != null) {
                    addToHistory(q)
                } else {
                    quoteText.text = "Не удалось загрузить цитату"
                }
            }
        }
    }

    private suspend fun fetchRandomQuote(): Quote? { // получение случайной цитаты
        return try {
            val client = OkHttpClient()
            val request = Request.Builder()
                .url("https://thequoteshub.com/api/random-quote")
                .build()
            val resp = client.newCall(request).execute()
            val body = resp.body?.string() ?: return null

            val json = JSONObject(body)
            val textCandidates = listOf("quote", "content", "text", "body", "quoteText") // рассматриваю разные ключи из-за кривового json'a
            val authorCandidates = listOf("author", "name", "quoteAuthor")

            var text: String? = null
            var author: String? = null

            for (k in textCandidates) {
                if (json.has(k)) {
                    text = json.optString(k, null)
                    if (!text.isNullOrBlank()) break
                }
            }
            for (k in authorCandidates) {
                if (json.has(k)) {
                    author = json.optString(k, null)
                    if (!author.isNullOrBlank()) break
                }
            }

            // если нет на верхнем уровне код ищет в полях data или первом элементе массива
            if (text.isNullOrBlank()) {
                if (json.has("data")) {
                    val data = json.get("data")
                    if (data is JSONObject) {
                        for (k in textCandidates) {
                            if (data.has(k)) {
                                text = data.optString(k, null)
                                if (!text.isNullOrBlank()) break
                            }
                        }
                        for (k in authorCandidates) {
                            if (data.has(k)) {
                                author = data.optString(k, null)
                                if (!author.isNullOrBlank()) break
                            }
                        }
                    }
                }
            }

            // если совсем кривой json, просто пустой блок добавляется
            if (text.isNullOrBlank()) {
                text = body.takeIf { it.isNotBlank() }?.let {
                    it
                }
            }

            Quote(text = text ?: return null, author = author)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}
