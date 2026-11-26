package com.celikgames.ehliyetol

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedCallback
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.lifecycleScope
import java.lang.ref.WeakReference

class MainActivity : ComponentActivity() {
    private lateinit var durum: UygulamaDurum

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        AndroidBaglam.ctx = applicationContext
        AndroidBaglam.etkinlik = WeakReference(this)
        Ses.kur()
        AdMob.baslat(this)
        durum = UygulamaDurum(lifecycleScope)
        // Geri tuşu: uygulama içi ekran yığını; en baştaysa uygulamadan çık
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (!durum.geri()) { isEnabled = false; onBackPressedDispatcher.onBackPressed(); isEnabled = true }
            }
        })
        setContent { App(durum) }
    }

    override fun onResume() { super.onResume(); AndroidBaglam.etkinlik = WeakReference(this) }
    override fun onPause() { super.onPause(); durum.sus() }   // uygulamadan çıkınca okuma durur
    override fun onDestroy() { if (isFinishing) Ses.kapat(); super.onDestroy() }
}
