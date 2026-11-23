package com.example.fbxrigstudio

import android.annotation.SuppressLint
import android.net.Uri
import android.os.Bundle
import android.webkit.JavascriptInterface
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.webkit.WebViewAssetLoader

class MainActivity : AppCompatActivity() {

    private lateinit var webView: WebView
    private var selectedFbxUri: Uri? = null
    private var selectedTextureUri: Uri? = null

    private val pickFbxLauncher =
        registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
            if (uri != null) {
                selectedFbxUri = uri
                webView.post {
                    webView.evaluateJavascript("window.onFbxSelected && window.onFbxSelected();", null)
                }
            }
        }

    private val pickTextureLauncher =
        registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
            if (uri != null) {
                selectedTextureUri = uri
                webView.post {
                    webView.evaluateJavascript("window.onTextureSelected && window.onTextureSelected();", null)
                }
            }
        }

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        webView = WebView(this)
        setContentView(webView)

        val assetLoader = WebViewAssetLoader.Builder()
            .addPathHandler("/assets/", WebViewAssetLoader.AssetsPathHandler(this))
            .build()

        webView.settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true
            useWideViewPort = true
            loadWithOverviewMode = true
        }

        webView.addJavascriptInterface(AndroidBridge(), "AndroidBridge")

        webView.webViewClient = object : WebViewClient() {
            override fun shouldInterceptRequest(
                view: WebView,
                request: WebResourceRequest
            ): WebResourceResponse? {
                val url = request.url
                if (url.host == "appassets.androidplatform.net") {
                    when (url.path) {
                        "/user-fbx" -> {
                            val uri = selectedFbxUri ?: return null
                            val stream = contentResolver.openInputStream(uri) ?: return null
                            return WebResourceResponse("application/octet-stream", null, stream)
                        }
                        "/user-texture" -> {
                            val uri = selectedTextureUri ?: return null
                            val mime = contentResolver.getType(uri) ?: "image/png"
                            val stream = contentResolver.openInputStream(uri) ?: return null
                            return WebResourceResponse(mime, null, stream)
                        }
                    }
                }
                return assetLoader.shouldInterceptRequest(url)
            }
        }

        webView.loadUrl("https://appassets.androidplatform.net/assets/index.html")
    }

    inner class AndroidBridge {
        @JavascriptInterface
        fun pickFbx() {
            runOnUiThread {
                pickFbxLauncher.launch(arrayOf("*/*"))
            }
        }

        @JavascriptInterface
        fun pickTexture() {
            runOnUiThread {
                pickTextureLauncher.launch(arrayOf("image/*"))
            }
        }
    }
}