package com.example.ds9reader.readium

import android.graphics.Color
import android.os.Bundle
import android.view.Gravity
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ProgressBar
import android.widget.TextView
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.fragment.app.FragmentContainerView
import androidx.fragment.app.commit
import androidx.lifecycle.lifecycleScope
import com.example.ds9reader.R
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.readium.r2.navigator.epub.EpubNavigatorFactory
import org.readium.r2.navigator.epub.EpubNavigatorFragment
import org.readium.r2.navigator.epub.EpubPreferences
import org.readium.r2.shared.publication.Publication
import org.readium.r2.shared.util.asset.AssetRetriever
import org.readium.r2.shared.util.getOrElse
import org.readium.r2.shared.util.http.DefaultHttpClient
import org.readium.r2.streamer.PublicationOpener
import org.readium.r2.streamer.parser.DefaultPublicationParser

/**
 * Experimental Readium-based EPUB reader.
 * Renders real XHTML + images from the EPUB instead of plain-text extraction.
 */
class ReadiumReaderActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_BOOK_ID = "book_id"
        const val EXTRA_FILE_PATH = "file_path"
        const val EXTRA_TITLE = "title"
        private const val NAVIGATOR_TAG = "readium_epub_navigator"
    }

    private var publication: Publication? = null
    private lateinit var statusView: TextView
    private lateinit var loadingView: ProgressBar
    private lateinit var container: FragmentContainerView

    override fun onCreate(savedInstanceState: Bundle?) {
        // Dummy factory so process-restore does not crash before we open the book.
        supportFragmentManager.fragmentFactory = EpubNavigatorFragment.createDummyFactory()
        super.onCreate(savedInstanceState)

        val title = intent.getStringExtra(EXTRA_TITLE) ?: "Reading"
        val filePath = intent.getStringExtra(EXTRA_FILE_PATH).orEmpty()

        val root = FrameLayout(this).apply {
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT,
            )
            setBackgroundColor(Color.WHITE)
        }

        val toolbar = Toolbar(this).apply {
            id = R.id.readium_toolbar
            setBackgroundColor(Color.parseColor("#1A1C18"))
            setTitleTextColor(Color.WHITE)
            setNavigationIcon(androidx.appcompat.R.drawable.abc_ic_ab_back_material)
            setNavigationOnClickListener { finish() }
            this.title = title
            layoutParams = FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT,
                Gravity.TOP,
            )
        }

        container = FragmentContainerView(this).apply {
            id = R.id.readium_container
            layoutParams = FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT,
            ).apply {
                topMargin = (56 * resources.displayMetrics.density).toInt()
            }
        }

        loadingView = ProgressBar(this).apply {
            layoutParams = FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT,
                Gravity.CENTER,
            )
        }

        statusView = TextView(this).apply {
            layoutParams = FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT,
                Gravity.CENTER,
            ).apply {
                marginStart = 48
                marginEnd = 48
            }
            gravity = Gravity.CENTER
            setTextColor(Color.DKGRAY)
            textSize = 15f
            visibility = android.view.View.GONE
        }

        root.addView(container)
        root.addView(toolbar)
        root.addView(loadingView)
        root.addView(statusView)
        setContentView(root)

        onBackPressedDispatcher.addCallback(
            this,
            object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    finish()
                }
            },
        )

        if (filePath.isBlank()) {
            showError("Missing EPUB file path.")
            return
        }

        if (savedInstanceState == null) {
            openPublication(filePath, title)
        } else {
            // Activity restored; if fragment exists, hide loading.
            val existing = supportFragmentManager.findFragmentByTag(NAVIGATOR_TAG)
            if (existing != null) {
                loadingView.visibility = android.view.View.GONE
            } else {
                openPublication(filePath, title)
            }
        }
    }

    private fun openPublication(filePath: String, title: String) {
        loadingView.visibility = android.view.View.VISIBLE
        statusView.visibility = android.view.View.GONE

        lifecycleScope.launch {
            val result = withContext(Dispatchers.IO) {
                runCatching {
                    val file = File(filePath)
                    if (!file.exists()) error("EPUB file not found:\n$filePath")

                    val httpClient = DefaultHttpClient()
                    val assetRetriever = AssetRetriever(
                        contentResolver = contentResolver,
                        httpClient = httpClient,
                    )
                    val asset = assetRetriever.retrieve(file).getOrElse {
                        error("Unable to open EPUB asset: $it")
                    }
                    val opener = PublicationOpener(
                        publicationParser = DefaultPublicationParser(
                            context = this@ReadiumReaderActivity,
                            httpClient = httpClient,
                            assetRetriever = assetRetriever,
                            pdfFactory = null,
                        ),
                    )
                    val publication = opener.open(
                        asset = asset,
                        allowUserInteraction = false,
                    ).getOrElse {
                        error("Unable to parse EPUB: $it")
                    }
                    publication
                }
            }

            result.onSuccess { pub ->
                publication = pub
                loadingView.visibility = android.view.View.GONE
                attachNavigator(pub, title)
            }.onFailure { err ->
                showError(err.message ?: "Failed to open book with Readium.")
            }
        }
    }

    private fun attachNavigator(publication: Publication, title: String) {
        val factory = EpubNavigatorFactory(publication).createFragmentFactory(
            initialLocator = null,
            initialPreferences = EpubPreferences(),
            listener = null,
            configuration = EpubNavigatorFragment.Configuration(),
        )
        supportFragmentManager.fragmentFactory = factory
        supportFragmentManager.commit {
            replace(
                R.id.readium_container,
                EpubNavigatorFragment::class.java,
                Bundle(),
                NAVIGATOR_TAG,
            )
        }
        supportActionBar?.title = title
    }

    private fun showError(message: String) {
        loadingView.visibility = android.view.View.GONE
        statusView.visibility = android.view.View.VISIBLE
        statusView.text = message
    }

    override fun onDestroy() {
        publication?.close()
        publication = null
        super.onDestroy()
    }
}
