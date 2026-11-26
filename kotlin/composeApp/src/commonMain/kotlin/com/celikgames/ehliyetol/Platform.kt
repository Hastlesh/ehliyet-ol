package com.celikgames.ehliyetol

import androidx.compose.runtime.Composable

/** Küçük anahtar-değer deposu (yalnız bu telefonda). Android: SharedPreferences · iOS: NSUserDefaults. */
expect object Depo {
    fun oku(anahtar: String): String?
    fun yaz(anahtar: String, deger: String?)
}

/** Şu an (ms) */
expect fun simdiMs(): Long

/** ms → yerel tarih: [yıl, ay(1-12), gün, saat, dakika] */
expect fun yerelTarih(ms: Long): IntArray

/** Kısa titreşim: dogru=true hafif, false iki kez */
expect fun titret(dogru: Boolean)

/** Telefonun metin okuma motoru (Türkçe). Android: TextToSpeech · iOS: AVSpeechSynthesizer */
expect object Ses {
    /** Türkçe ses var mı */
    fun hazir(): Boolean
    /** Parçaları sırayla okur (id, metin). Her parça başlayınca/bitince olay(tip, id) çağrılır: basla | bitti | hata */
    fun konus(parcalar: List<Pair<String, String>>, hiz: Double)
    fun sus()
    var olay: ((String, String) -> Unit)?
}

/** Ekranın altındaki şerit reklam (yalnız izinli ekranlarda çağrılır). */
@Composable
expect fun ReklamSeridi()

/** Tam ekran (geçiş) reklamı: hazırsa gösterir, gösterdiyse true. */
expect object GecisReklami { fun goster(): Boolean }

/** Hesap durumu (Firebase Authentication) */
data class HesapBilgi(val giris: Boolean = false, val anonim: Boolean = false, val eposta: String = "", val ad: String = "", val yontem: String = "")

/** islem: misafir | google | giris | kayit | sifirla | cikis | sil | yedekOku | yedekYaz */
data class HesapSonuc(val islem: String, val ok: Boolean, val hata: String = "", val veri: String? = null)

/** Hesap ve bulut yedeği. iOS'ta şimdilik yok (destekli=false): uygulama giriş yapmadan tam çalışır. */
expect object Hesap {
    val destekli: Boolean
    fun durum(): HesapBilgi
    fun islem(islem: String, eposta: String, sifre: String, sonuc: (HesapSonuc) -> Unit)
    fun yedekYaz(veri: String, sonuc: (HesapSonuc) -> Unit)
}
