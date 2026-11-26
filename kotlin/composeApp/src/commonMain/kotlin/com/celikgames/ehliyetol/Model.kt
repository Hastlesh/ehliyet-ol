package com.celikgames.ehliyetol

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

val JSON = Json { ignoreUnknownKeys = true; explicitNulls = false; encodeDefaults = true; coerceInputValues = true; isLenient = true }

/**
 * Soru (sorular.json, web sürümüyle aynı biçim).
 * i: kimlik · d: ders (0-3) · s: soru metni · c: şıklar · k: doğru şık · n: kaç sınavda çıktı · y: yıl
 * g: resim dosyası (r/…) · t: 1 = resim yalnız şekil (metin ayrıca yazılır) · o: 1 = uygulamanın kendi alıştırma sorusu
 */
@Serializable
data class Soru(
    val i: String, val d: Int, val s: String, val c: List<String>, val k: Int,
    val n: Int = 1, val y: Int = 0, val g: String? = null, val t: Int = 0, val o: Int = 0,
)

/** Konu anlatımı (dersler.json). a: ilgili soruları bulan düzenli ifade · p: paragraflar */
@Serializable
data class Konu(val id: String, val d: Int, val b: String, val a: String, val p: List<String>)

/** Sınav kaydı: t zaman · p puan · d ders başına [doğru, toplam] · s süre (sn) */
@Serializable
data class SinavKaydi(val t: Long, val p: Int, val d: List<List<Int>> = emptyList(), val s: Long = 0)

/**
 * Telefondaki ilerleme. c: soru → [doğru sayısı, yanlış sayısı, son cevap doğru mu (1/0), zaman ms]
 * Bulut yedeği {c, k, sim} alanlarıyla web/eski Android sürümüyle birebir uyumlu.
 */
@Serializable
data class Ilerleme(
    val c: Map<String, List<Long>> = emptyMap(),
    val k: List<String> = emptyList(),
    val sim: List<SinavKaydi> = emptyList(),
    val hiz: Double = 1.0,
    val tema: String = "",
    val girisGoruldu: Boolean = false,
    val sonYedek: Long = 0,
)

@Serializable
data class Yedek(val c: Map<String, List<Long>> = emptyMap(), val k: List<String> = emptyList(), val sim: List<SinavKaydi> = emptyList())

/** tur: pratik | sinav | incele */
@Serializable
data class Test(
    val tur: String, val baslik: String, val ids: List<String>, val i: Int = 0, val cevap: List<Int>,
    val bas: Long, val bitis: Long = 0, val bitti: Boolean = false, val son: Long = 0,
    val ders: List<List<Int>> = emptyList(), val puan: Int = 0, val eskiTur: String? = null,
)

data class Ders(val ad: String, val sim: Int, val renk: Int)

val DERSLER = listOf(
    Ders("İlk Yardım", 12, 0), Ders("Trafik ve Çevre", 23, 1),
    Ders("Motor ve Araç Tekniği", 9, 2), Ders("Trafik Adabı", 6, 3),
)
const val HARF = "ABCD"
const val SINAV_SURE_SN = 45 * 60
const val GECME = 70

/** Türkçe küçük harf (I→ı, İ→i): konu düzenli ifadeleri web sürümünde toLocaleLowerCase('tr') ile çalışıyordu */
fun trKucuk(s: String): String = s.replace('I', 'ı').replace('İ', 'i').lowercase()

val AYLAR = listOf("Ocak", "Şubat", "Mart", "Nisan", "Mayıs", "Haziran", "Temmuz", "Ağustos", "Eylül", "Ekim", "Kasım", "Aralık")

fun sureYaz(sn: Long): String { val s = sn.coerceAtLeast(0); return "${(s / 60).toString().padStart(2, '0')}:${(s % 60).toString().padStart(2, '0')}" }

fun yuzde(a: Int, b: Int): Int = if (b == 0) 0 else kotlin.math.round(a * 100.0 / b).toInt()
