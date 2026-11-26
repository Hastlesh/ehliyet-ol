package com.celikgames.ehliyetol

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.UIKitView
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.ObjCSignatureOverride
import platform.AVFAudio.AVSpeechBoundary
import platform.AVFAudio.AVSpeechSynthesisVoice
import platform.AVFAudio.AVSpeechSynthesizer
import platform.AVFAudio.AVSpeechSynthesizerDelegateProtocol
import platform.AVFAudio.AVSpeechUtterance
import platform.AVFAudio.AVSpeechUtteranceDefaultSpeechRate
import platform.Foundation.NSCalendar
import platform.Foundation.NSCalendarUnitDay
import platform.Foundation.NSCalendarUnitHour
import platform.Foundation.NSCalendarUnitMinute
import platform.Foundation.NSCalendarUnitMonth
import platform.Foundation.NSCalendarUnitYear
import platform.Foundation.NSDate
import platform.Foundation.NSProcessInfo
import platform.Foundation.NSUserDefaults
import platform.Foundation.dateWithTimeIntervalSince1970
import platform.Foundation.timeIntervalSince1970
import platform.UIKit.UIImpactFeedbackGenerator
import platform.UIKit.UIImpactFeedbackStyle
import platform.UIKit.UINotificationFeedbackGenerator
import platform.UIKit.UINotificationFeedbackType
import platform.UIKit.UIView
import platform.darwin.NSObject

/**
 * iPhone'a özgü kütüphaneler (Google Mobile Ads) Swift tarafında.
 * Swift (iosApp/Sources/Kopru.swift) bu arayüzü uygular ve açılışta IosAyar.kopru'ya verir.
 */
interface IosKopru {
    /** Alt şerit reklam görünümü (GADBannerView) */
    fun seritGorunum(): UIView
    fun gecisGoster(): Boolean
}

object IosAyar { var kopru: IosKopru? = null }

actual object Depo {
    private val d get() = NSUserDefaults.standardUserDefaults
    actual fun oku(anahtar: String): String? = d.stringForKey("ehliyetol.$anahtar")
    actual fun yaz(anahtar: String, deger: String?) { if (deger == null) d.removeObjectForKey("ehliyetol.$anahtar") else d.setObject(deger, "ehliyetol.$anahtar") }
}

actual fun simdiMs(): Long = (NSDate().timeIntervalSince1970 * 1000).toLong()

actual fun yerelTarih(ms: Long): IntArray {
    val t = NSCalendar.currentCalendar.components(
        NSCalendarUnitYear or NSCalendarUnitMonth or NSCalendarUnitDay or NSCalendarUnitHour or NSCalendarUnitMinute,
        NSDate.dateWithTimeIntervalSince1970(ms / 1000.0),
    )
    return intArrayOf(t.year.toInt(), t.month.toInt(), t.day.toInt(), t.hour.toInt(), t.minute.toInt())
}

actual fun titret(dogru: Boolean) {
    if (dogru) UIImpactFeedbackGenerator(UIImpactFeedbackStyle.UIImpactFeedbackStyleLight).impactOccurred()
    else UINotificationFeedbackGenerator().notificationOccurred(UINotificationFeedbackType.UINotificationFeedbackTypeError)
}

/** iPhone'un kendi Türkçe sesi (AVSpeechSynthesizer). Her parça ayrı okunur; başlayınca/bitince olay bildirilir. */
actual object Ses {
    private val sentez = AVSpeechSynthesizer()
    private val kimlikler = mutableMapOf<AVSpeechUtterance, String>()
    actual var olay: ((String, String) -> Unit)? = null
    private val ses: AVSpeechSynthesisVoice? by lazy { AVSpeechSynthesisVoice.voiceWithLanguage("tr-TR") }

    private class Temsilci : NSObject(), AVSpeechSynthesizerDelegateProtocol {
        @ObjCSignatureOverride
        override fun speechSynthesizer(synthesizer: AVSpeechSynthesizer, didStartSpeechUtterance: AVSpeechUtterance) {
            kimlikler[didStartSpeechUtterance]?.let { olay?.invoke("basla", it) }
        }
        @ObjCSignatureOverride
        override fun speechSynthesizer(synthesizer: AVSpeechSynthesizer, didFinishSpeechUtterance: AVSpeechUtterance) {
            kimlikler.remove(didFinishSpeechUtterance)?.let { olay?.invoke("bitti", it) }
        }
        @ObjCSignatureOverride
        override fun speechSynthesizer(synthesizer: AVSpeechSynthesizer, didCancelSpeechUtterance: AVSpeechUtterance) {
            kimlikler.remove(didCancelSpeechUtterance)?.let { olay?.invoke("hata", it) }
        }
    }
    private val temsilci = Temsilci()
    init { sentez.delegate = temsilci }

    actual fun hazir(): Boolean = ses != null

    actual fun konus(parcalar: List<Pair<String, String>>, hiz: Double) {
        sus()
        parcalar.forEach { (id, metin) ->
            val u = AVSpeechUtterance.speechUtteranceWithString(metin.take(3000))
            u.voice = ses
            u.rate = (AVSpeechUtteranceDefaultSpeechRate * hiz.toFloat()).coerceIn(0.1f, 0.9f)
            kimlikler[u] = id
            sentez.speakUtterance(u)
        }
    }

    actual fun sus() {
        if (sentez.isSpeaking()) sentez.stopSpeakingAtBoundary(AVSpeechBoundary.AVSpeechBoundaryImmediate)
        kimlikler.clear()
    }
}

/** Gösterim modu (yalnız simülatör testi, CI): reklam alanı hiç yer kaplamaz */
internal fun gosterimModu(): Boolean = NSProcessInfo.processInfo.environment["EHLIYETOL_GOSTERIM"] == "1"

@OptIn(ExperimentalForeignApi::class)
@Composable
actual fun ReklamSeridi() {
    val k = IosAyar.kopru ?: return
    if (gosterimModu()) return
    UIKitView(factory = { k.seritGorunum() }, modifier = Modifier.fillMaxWidth().height(50.dp))
}

actual object GecisReklami {
    private var son = 0L
    /** Test bitince: en fazla 3 dakikada bir */
    actual fun goster(): Boolean {
        if (simdiMs() - son < 3 * 60 * 1000L) return false
        val ok = IosAyar.kopru?.gecisGoster() ?: false
        if (ok) son = simdiMs()
        return ok
    }
}

/** iPhone'da hesap/bulut yedeği şimdilik yok: uygulama giriş yapmadan tam çalışır, ilerleme telefonda saklanır. */
actual object Hesap {
    actual val destekli: Boolean = false
    actual fun durum(): HesapBilgi = HesapBilgi()
    actual fun islem(islem: String, eposta: String, sifre: String, sonuc: (HesapSonuc) -> Unit) = sonuc(HesapSonuc(islem, false))
    actual fun yedekYaz(veri: String, sonuc: (HesapSonuc) -> Unit) = sonuc(HesapSonuc("yedekYaz", false))
}
