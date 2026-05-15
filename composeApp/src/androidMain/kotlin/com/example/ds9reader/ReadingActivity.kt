package com.example.ds9reader

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.commit
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import org.readium.r2.navigator.epub.EpubNavigatorFragment
import org.readium.r2.shared.publication.Publication
import org.readium.r2.streamer.Streamer
import org.readium.r2.streamer.parser.epub.EpubParser
import java.io.File

/**
 * Android reading activity that hosts the Readium EpubNavigatorFragment.
 * Launched from the common ReaderScreen to display EPUB content.
 */
class ReadingActivity : AppCompatActivity() {

    companion object {
        private const val EXTRA_FILE_PATH = "file_path"
        private const val TAG = "ReadingActivity"

        fun start(context: Context, filePath: String) {
            val intent = Intent(context, ReadingActivity::class.java).apply {
                putExtra(EXTRA_FILE_PATH, filePath)
            }
            context.startActivity(intent)
        }
    }

    private lateinit var streamer: Streamer
    private var navigatorFragment: EpubNavigatorFragment? = null
    private val activityScope = MainScope()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val filePath = intent.getStringExtra(EXTRA_FILE_PATH)
            ?: run {
                Log.e(TAG, "No file path provided")
                finish()
                return
            }

        // Initialize the streamer with EPUB parser
        streamer = Streamer(
            context = this,
            parsers = listOf(EpubParser())
        )

        // Load the publication asynchronously
        activityScope.launch {
            val publication = streamer.open(
                file = File(filePath),
                sender = null
            )

            if (publication == null) {
                Log.e(TAG, "Failed to open publication: $filePath")
                finish()
                return@launch
            }

            // Create and show the navigator fragment
            if (savedInstanceState == null) {
                navigatorFragment = EpubNavigatorFragment.create(
                    publication = publication,
                    context = this@ReadingActivity
                )

                supportFragmentManager.commit {
                    replace(android.R.id.content, navigatorFragment!!)
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        streamer.close()
        activityScope.cancel()
    }
}
