package com.celikgames.ehliyetol

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import java.lang.ref.WeakReference
import java.util.Calendar
import java.util.Locale

/** Uygulama bağlamı ve açık ekran: MainActivity açılırken atanır. */
@SuppressLint("StaticFieldLeak")
object AndroidBaglam {
    lateinit var ctx: Context
    var etkinlik = WeakReference<Activity>(null)
}

actual object Depo {
    private val sp get() = AndroidBaglam.ctx.getSharedPreferences("ehliyetol", Context.MODE_PRIVATE)
    actual fun oku(anahtar: String): String? = sp.getString(anahtar, null)
    actual fun yaz(anahtar: String, deger: String?) { sp.edit().apply { if (deger == null) remove(anahtar) else putString(anahtar, deger) }.apply() }
}

actual fun simdiMs(): Long = System.currentTimeMillis()

actual fun yerelTarih(ms: Long): IntArray {
    val c = Calendar.getInstance().apply { timeInMillis = ms }
    return intArrayOf(c.get(Calendar.YEAR), c.get(Calendar.MONTH) + 1, c.get(Calendar.DAY_OF_MONTH), c.get(Calendar.HOUR_OF_DAY), c.get(Calendar.MINUTE))
}

actual fun titret(dogru: Boolean) {
    runCatching {
        val v = if (Build.VERSION.SDK_INT >= 31) (AndroidBaglam.ctx.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager).defaultVibrator
        else @Suppress("DEPRECATION") (AndroidBaglam.ctx.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator)
        v.vibrate(if (dogru) VibrationEffect.createOneShot(15, VibrationEffect.DEFAULT_AMPLITUDE) else VibrationEffect.createWaveform(longArrayOf(0, 40, 60, 40), -1))
    }
}

/**
 * Telefonun metin okuma motoru (Türkçe). Varsayılan motorda Türkçe yoksa (ör. Samsung motorunda Türkçe veri inmemiş)
 * Google motoru denenir — eski sürümle aynı davranış.
 */
actual object Ses {
    private var tts: TextToSpeech? = null
    @Volatile private var durum = "yok"   // yukleniyor | hazir | yok
    private val ana = Handler(Looper.getMainLooper())
    actual var olay: ((String, String) -> Unit)? = null

    fun kur(motor: String? = null) {
        durum = "yukleniyor"
        runCatching {
            val dinle = TextToSpeech.OnInitListener { s ->
                val t = tts
                val dil = if (s == TextToSpeech.SUCCESS && t != null) t.setLanguage(Locale("tr", "TR")) else TextToSpeech.LANG_NOT_SUPPORTED
                val yok = dil == TextToSpeech.LANG_MISSING_DATA || dil == TextToSpeech.LANG_NOT_SUPPORTED
                if (yok && motor == null) { t?.shutdown(); tts = null; ana.post { kur("com.google.android.tts") }; return@OnInitListener }
                if (yok) { durum = "yok"; return@OnInitListener }
                durum = "hazir"
                t?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                    override fun onStart(id: String?) { bildir("basla", id) }
                    override fun onDone(id: String?) { bildir("bitti", id) }
                    @Deprecated("eski") override fun onError(id: String?) { bildir("hata", id) }
                })
            }
            tts = if (motor == null) TextToSpeech(AndroidBaglam.ctx, dinle) else TextToSpeech(AndroidBaglam.ctx, dinle, motor)
        }.onFailure { durum = "yok" }
    }

    private fun bildir(tip: String, id: String?) { if (id != null) ana.post { olay?.invoke(tip, id) } }

    /** Motor hâlâ yükleniyorsa da "var" sayılır (düğme görünür; ilk dokunuşta hazır olur) */
    actual fun hazir(): Boolean = durum != "yok"

    actual fun konus(parcalar: List<Pair<String, String>>, hiz: Double) {
        val t = tts ?: return
        if (durum != "hazir") return
        t.setSpeechRate(hiz.coerceIn(0.5, 2.0).toFloat())
        parcalar.forEachIndexed { i, (id, metin) ->
            t.speak(metin.take(3000), if (i == 0) TextToSpeech.QUEUE_FLUSH else TextToSpeech.QUEUE_ADD, Bundle(), id)
        }
    }

    actual fun sus() { tts?.stop() }

    fun kapat() { tts?.stop(); tts?.shutdown(); tts = null; durum = "yok" }
}
