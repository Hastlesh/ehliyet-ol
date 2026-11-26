package com.celikgames.ehliyetol

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.celikgames.ehliyetol.kaynak.Res
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.min
import kotlin.math.sqrt
import kotlin.random.Random

sealed interface Ekran {
    data object Yukleniyor : Ekran
    data object Giris : Ekran
    data object Eposta : Ekran
    data object Ana : Ekran
    data object Konular : Ekran
    data object Istatistik : Ekran
    data class DersE(val d: Int) : Ekran
    data class KonuE(val id: String) : Ekran
    data object TestE : Ekran
    data object Sonuc : Ekran
}

/** Onay / bilgi penceresi */
data class Uyari(val baslik: String, val mesaj: String, val evet: String? = null, val is_: (() -> Unit)? = null)

class UygulamaDurum(private val kapsam: CoroutineScope) {
    var sorular: List<Soru> = emptyList(); private set
    var konular: List<Konu> = emptyList(); private set
    private var BY: Map<String, Soru> = emptyMap()
    fun soru(id: String): Soru? = BY[id]

    var st by mutableStateOf(Ilerleme()); private set
    var test by mutableStateOf<Test?>(null); private set
    val yigin = mutableStateListOf<Ekran>(Ekran.Yukleniyor)
    val ekran: Ekran get() = yigin.last()

    var uyari by mutableStateOf<Uyari?>(null)
    var toast by mutableStateOf<String?>(null)
    var bekliyor by mutableStateOf(false); private set
    var hesap by mutableStateOf(HesapBilgi()); private set
    var konfeti by mutableIntStateOf(0); private set
    var yuklemeHatasi by mutableStateOf(false); private set

    // sesli okuma
    var sesCalisiyor by mutableStateOf(false); private set
    var okunan by mutableStateOf<String?>(null); private set
    private var sonParca: String? = null

    init {
        st = Depo.oku("ilerleme")?.let { runCatching { JSON.decodeFromString(Ilerleme.serializer(), it) }.getOrNull() } ?: Ilerleme()
        if (st.hiz !in HIZLAR) st = st.copy(hiz = 1.0)
        Ses.olay = { tip, id -> sesOlay(tip, id) }
        kapsam.launch { yukle() }
    }

    private suspend fun yukle() {
        runCatching {
            val s = JSON.decodeFromString<List<Soru>>(Res.readBytes("files/sorular.json").decodeToString())
            sorular = s.filter { it.c.size == 4 && it.k in 0..3 && it.d in 0..3 }
            konular = JSON.decodeFromString<List<Konu>>(Res.readBytes("files/dersler.json").decodeToString())
            BY = sorular.associateBy { it.i }
            test = Depo.oku("test")?.let { runCatching { JSON.decodeFromString(Test.serializer(), it) }.getOrNull() }?.takeIf { t -> t.ids.all { it in BY } }
            hesap = Hesap.durum()
            yigin.clear(); yigin.add(if (st.girisGoruldu) Ekran.Ana else Ekran.Giris)
            Depo.oku("ekranGosterim")?.let { Depo.yaz("ekranGosterim", null); gosterim(it) }
        }.onFailure { yuklemeHatasi = true }
        delay(1500); esitle()   // girişliyse açılışta buluttaki yedekle birleştir
    }

    private fun kaydet() { Depo.yaz("ilerleme", JSON.encodeToString(Ilerleme.serializer(), st)) }
    private fun testKaydet() { Depo.yaz("test", test?.let { JSON.encodeToString(Test.serializer(), it) }) }
    private fun stDegis(f: (Ilerleme) -> Ilerleme) { st = f(st); kaydet() }
    private fun testDegis(t: Test?) { test = t; testKaydet() }

    /* ---------- gezinme ---------- */
    fun git(e: Ekran) {
        sus()
        when (e) {
            Ekran.Ana, Ekran.Konular, Ekran.Istatistik -> { yigin.clear(); yigin.add(e) }
            else -> if (yigin.last() != e) yigin.add(e)
        }
    }
    private fun degistir(e: Ekran) { sus(); yigin.removeAt(yigin.lastIndex); yigin.add(e) }

    /** Geri: yığından bir ekran çıkar; sekme kökündeyse ana sayfaya, ana sayfadaysa false (uygulamadan çık) */
    fun geri(): Boolean {
        if (uyari != null) { uyari = null; return true }
        sus()
        val e = ekran
        if (e == Ekran.TestE && test?.tur == "incele") { incelemeBitti(); return true }
        if (yigin.size > 1) { yigin.removeAt(yigin.lastIndex); return true }
        if (e == Ekran.Konular || e == Ekran.Istatistik || e == Ekran.Sonuc || e is Ekran.DersE || e is Ekran.KonuE || e == Ekran.TestE) { yigin.clear(); yigin.add(Ekran.Ana); return true }
        if (e == Ekran.Eposta) { yigin.clear(); yigin.add(Ekran.Giris); return true }
        return false
    }

    /** Şerit reklam yalnız bu ekranlarda (test/sınav, konu dinleme ve giriş ekranlarında yok) */
    val reklamIzinli: Boolean get() = ekran.let { it == Ekran.Ana || it == Ekran.Konular || it == Ekran.Istatistik || it == Ekran.Sonuc || it is Ekran.DersE }

    /* ---------- istatistik ---------- */
    data class DersIstat(val toplam: Int, val gorulen: Int, val oran: Double, val yanlis: Int)
    fun dersIstat(d: Int): DersIstat {
        var gorulen = 0; var dogru = 0; var yanlis = 0; var toplam = 0
        for (q in sorular) if (q.d == d) {
            toplam++
            st.c[q.i]?.let { gorulen++; if (it.getOrElse(2) { 0 } == 1L) dogru++ else yanlis++ }
        }
        return DersIstat(toplam, gorulen, if (gorulen > 0) dogru.toDouble() / gorulen else 0.0, yanlis)
    }
    fun hazirlik(): Int {
        var t = 0.0
        DERSLER.forEachIndexed { d, D ->
            val s = dersIstat(d)
            val kapsam = min(1.0, s.gorulen / maxOf(1.0, s.toplam * 0.5))
            t += D.sim / 50.0 * s.oran * sqrt(kapsam)
        }
        return kotlin.math.round(t * 100).toInt()
    }
    fun yanlislar(): List<Soru> = sorular.filter { q -> st.c[q.i]?.let { it.getOrElse(2) { 0 } == 0L } == true }
    fun cokCikan(): List<Soru> = sorular.filter { it.n >= 3 }.sortedByDescending { it.n }
    fun kayitlilar(): List<Soru> = st.k.mapNotNull { BY[it] }

    fun konuHavuzu(k: Konu): List<Soru> {
        val re = runCatching { Regex(k.a, RegexOption.IGNORE_CASE) }.getOrNull() ?: return emptyList()
        return sorular.filter { it.d == k.d && re.containsMatchIn(trKucuk(it.s + " " + it.c.joinToString(" "))) }
    }
    private val havuzOnbellek = HashMap<String, Int>()
    fun konuSoruSayisi(k: Konu): Int = havuzOnbellek.getOrPut(k.id) { konuHavuzu(k).size }

    /* ---------- soru seçimi ---------- */
    private fun oncelik(q: Soru): Double {
        val c = st.c[q.i] ?: return 2 + Random.nextDouble()
        return if (c.getOrElse(2) { 0 } == 0L) 1.5 + Random.nextDouble() else Random.nextDouble()
    }
    fun sec(havuz: List<Soru>, n: Int): List<Soru> = havuz.map { oncelik(it) to it }.sortedByDescending { it.first }.take(n).map { it.second }.shuffled()

    fun testBaslat(tur: String, baslik: String, liste: List<Soru>) {
        if (liste.isEmpty()) { bilgi("Bu bölümde henüz soru yok."); return }
        sus()
        val simdi = simdiMs()
        testDegis(Test(tur, baslik, liste.map { it.i }, 0, liste.map { -1 }, simdi, if (tur == "sinav") simdi + SINAV_SURE_SN * 1000L else 0))
        yigin.removeAll { it == Ekran.TestE || it == Ekran.Sonuc }
        git(Ekran.TestE)
    }
    fun simulasyon() {
        val ids = DERSLER.flatMapIndexed { d, D -> sec(sorular.filter { it.d == d }, D.sim) }
        testBaslat("sinav", "Sınav Simülasyonu", ids)
    }
    fun dersTest(d: Int) = testBaslat("pratik", DERSLER[d].ad, sec(sorular.filter { it.d == d }, 20))
    fun dersYanlis(d: Int) = testBaslat("pratik", DERSLER[d].ad + " yanlışları", yanlislar().filter { it.d == d }.shuffled().take(30))
    fun konuTest(k: Konu) = testBaslat("pratik", k.b, sec(konuHavuzu(k), 20))
    fun yanlislarim() { val L = yanlislar(); if (L.isEmpty()) bilgi("Yanlış yaptığın soru yok. Test çözdükçe yanlışların burada toplanır.") else testBaslat("pratik", "Yanlışlarım", L.shuffled().take(30)) }
    fun enCokCikan() = testBaslat("pratik", "En çok çıkanlar", sec(cokCikan(), 30))
    fun kaydettiklerim() { val L = kayitlilar(); if (L.isEmpty()) bilgi("Kaydettiğin soru yok. Soru ekranındaki yıldızla soru kaydedebilirsin.") else testBaslat("pratik", "Kaydettiklerim", L.shuffled().take(50)) }
    fun tekrar() { val t = test ?: return; testBaslat("pratik", t.baslik, t.ids.mapNotNull { BY[it] }.shuffled()) }

    private fun isle(c: MutableMap<String, List<Long>>, id: String, dogru: Boolean) {
        val e = c[id] ?: listOf(0L, 0L, 0L, 0L)
        c[id] = listOf(e[0] + if (dogru) 1 else 0, e[1] + if (dogru) 0 else 1, if (dogru) 1L else 0L, simdiMs())
    }

    fun sikSec(i: Int) {
        val t = test ?: return
        if (t.bitti || t.tur == "incele") return
        val q = BY[t.ids[t.i]] ?: return
        val cevap = t.cevap.toMutableList()
        if (t.tur == "pratik") {
            if (cevap[t.i] >= 0) return
            cevap[t.i] = i
            stDegis { s -> s.copy(c = s.c.toMutableMap().also { isle(it, q.i, i == q.k) }) }
            titret(i == q.k)
        } else cevap[t.i] = if (cevap[t.i] == i) -1 else i
        testDegis(t.copy(cevap = cevap))
    }
    fun gitSoru(i: Int) { val t = test ?: return; if (i in t.ids.indices) { sus(); testDegis(t.copy(i = i)) } }
    fun sonraki() { val t = test ?: return; gitSoru(t.i + 1) }
    fun onceki() { val t = test ?: return; gitSoru(t.i - 1) }

    fun bitir(zorla: Boolean) {
        val t = test ?: return
        if (t.bitti) return
        val bos = t.cevap.count { it < 0 }
        if (!zorla && t.tur == "sinav" && bos > 0) { onay("Sınavı bitir?", "$bos soru boş. Boş sorular yanlış sayılır.", "Bitir") { bitir(true) }; return }
        val ders = MutableList(4) { mutableListOf(0, 0) }
        val c = st.c.toMutableMap()
        t.ids.forEachIndexed { i, id ->
            val q = BY[id] ?: return@forEachIndexed
            val cv = t.cevap[i]
            ders[q.d][1]++; if (cv == q.k) ders[q.d][0]++
            if (t.tur == "sinav" && cv >= 0) isle(c, id, cv == q.k)
        }
        val dogru = ders.sumOf { it[0] }
        val son = simdiMs()
        val puan = kotlin.math.round(dogru * 100.0 / t.ids.size).toInt()
        var yeni = st.copy(c = c)
        if (t.tur == "sinav") yeni = yeni.copy(sim = (yeni.sim + SinavKaydi(son, puan, ders, (son - t.bas) / 1000)).takeLast(50))
        st = yeni; kaydet()
        testDegis(t.copy(bitti = true, son = son, ders = ders, puan = puan))
        yedekGonder()
        yigin.removeAll { it == Ekran.TestE }
        git(Ekran.Sonuc)
        GecisReklami.goster()   // test bitti: geçiş reklamı (en fazla 3 dk'da bir — platform tarafında)
        if (t.tur == "sinav" && puan >= GECME) konfeti++
    }
    fun birak() = onay("Bu test bırakılsın mı?", "Bu testteki cevapların kaydedilmez. Daha önce çözdüğün sorular ve istatistiklerin silinmez.", "Bırak") {
        testDegis(null); yigin.clear(); yigin.add(Ekran.Ana)
    }
    fun incele() { val t = test ?: return; testDegis(t.copy(eskiTur = t.tur, tur = "incele", i = 0)); git(Ekran.TestE) }
    fun incelemeBitti() { val t = test ?: return; if (t.tur == "incele") testDegis(t.copy(tur = t.eskiTur ?: "pratik")); yigin.removeAll { it == Ekran.TestE }; if (ekran != Ekran.Sonuc) git(Ekran.Sonuc) }

    /** Sınav sayacı: süre bitince otomatik biter */
    fun sureDenetle() { val t = test ?: return; if (t.tur == "sinav" && !t.bitti && t.bitis - simdiMs() <= 0) bitir(true) }

    fun kaydetDegis() {
        val t = test ?: return; val id = t.ids[t.i]
        stDegis { s -> s.copy(k = if (id in s.k) s.k - id else s.k + id) }
        yedekGonder()
    }

    /* ---------- ayarlar ---------- */
    fun temaDegis() = stDegis { it.copy(tema = when (it.tema) { "" -> "acik"; "acik" -> "koyu"; else -> "" }) }
    fun temaAyarla(t: String) = stDegis { it.copy(tema = t) }
    fun hizDegis() { sus(); stDegis { it.copy(hiz = HIZLAR[(HIZLAR.indexOf(it.hiz) + 1) % HIZLAR.size]) } }
    fun sifirla() = onay("İlerleme sıfırlansın mı?", "Çözdüğün sorular, yanlışların, kayıtların ve sınav geçmişin silinir. Bu geri alınamaz.", "Sıfırla") {
        stDegis { Ilerleme(tema = it.tema, hiz = it.hiz, girisGoruldu = it.girisGoruldu) }; testDegis(null); git(Ekran.Ana)
    }

    /* ---------- sesli okuma ---------- */
    fun sesVar(): Boolean = Ses.hazir()
    fun oku(parcalar: List<Pair<String, String>>) {
        sus(); if (parcalar.isEmpty()) return
        sesCalisiyor = true; sonParca = parcalar.last().first
        Ses.konus(parcalar, st.hiz)
    }
    fun sus() { if (sesCalisiyor) Ses.sus(); sesCalisiyor = false; okunan = null }
    private fun sesOlay(tip: String, id: String) {
        if (!sesCalisiyor) return
        if (tip == "basla") okunan = id
        if ((tip == "bitti" || tip == "hata") && id == sonParca) { sesCalisiyor = false; okunan = null }
    }
    fun konuDinle(k: Konu) {
        if (sesCalisiyor) { sus(); return }
        oku(listOf("baslik" to k.b + ".") + k.p.mapIndexed { i, p -> "p$i" to p })
    }
    fun soruOku() {
        if (sesCalisiyor) { sus(); return }
        val t = test ?: return; val q = BY[t.ids[t.i]] ?: return
        val siklar = if (q.c.all { it.isBlank() }) listOf("Şıklar resimde.") else q.c.mapIndexed { i, c -> "${HARF[i]} şıkkı: ${c.ifBlank { "resimde" }}." }
        oku(listOf("soru" to q.s) + siklar.mapIndexed { i, m -> "s$i" to m })
    }

    /* ---------- pencereler ---------- */
    fun onay(baslik: String, mesaj: String, evet: String, is_: () -> Unit) { uyari = Uyari(baslik, mesaj, evet, is_) }
    fun bilgi(mesaj: String) { uyari = Uyari("", mesaj) }

    /* ---------- hesap ve bulut yedeği ---------- */
    private val kalici get() = hesap.giris && !hesap.anonim
    fun misafir() {
        stDegis { it.copy(girisGoruldu = true) }
        if (Hesap.destekli) hesapIslem("misafir") else git(Ekran.Ana)
    }
    fun hesapIslem(islem: String, eposta: String = "", sifre: String = "") {
        if (!Hesap.destekli) { bilgi("Hesap işlemleri bu telefonda henüz yok. Uygulamayı giriş yapmadan kullanabilirsin."); return }
        bekliyor = true
        Hesap.islem(islem, eposta.trim(), sifre) { r -> kapsam.launch { hesapSonuc(r) } }
    }
    fun esitle() { if (Hesap.destekli && kalici) Hesap.islem("yedekOku", "", "") { r -> kapsam.launch { hesapSonuc(r) } } }

    private var yedekIs: Job? = null
    fun yedekGonder() {
        if (!Hesap.destekli || !kalici) return
        yedekIs?.cancel()
        yedekIs = kapsam.launch {
            delay(1500)
            val veri = JSON.encodeToString(Yedek.serializer(), Yedek(st.c, st.k, st.sim))
            if (veri.length <= 480_000) Hesap.yedekYaz(veri) { r -> kapsam.launch { hesapSonuc(r) } }
        }
    }

    /** Bulut yedeği birleştirme: her soruda en son cevap esas; sayılar en büyüğü (web sürümüyle aynı) */
    private fun birlestir(b: Yedek) {
        val c = st.c.toMutableMap()
        for ((id, x) in b.c) {
            if (id !in BY || x.size < 4) continue
            val m = c[id]
            if (m == null) { c[id] = x.take(4); continue }
            val son = if (x[3] > m[3]) x else m
            c[id] = listOf(maxOf(m[0], x[0]), maxOf(m[1], x[1]), if (son[2] != 0L) 1L else 0L, son[3])
        }
        val k = (st.k + b.k.filter { it in BY }).distinct()
        val varOlan = st.sim.map { it.t }.toSet()
        val sim = (st.sim + b.sim.filter { it.t !in varOlan }).sortedBy { it.t }.takeLast(50)
        st = st.copy(c = c, k = k, sim = sim)
    }

    private fun hesapSonuc(r: HesapSonuc) {
        hesap = Hesap.durum()
        if (r.islem == "yedekOku") {
            if (!r.ok) return
            r.veri?.takeIf { it.isNotBlank() }?.let { v -> runCatching { birlestir(JSON.decodeFromString(Yedek.serializer(), v)) } }
            st = st.copy(sonYedek = simdiMs()); kaydet(); yedekGonder()
            return
        }
        if (r.islem == "yedekYaz") { if (r.ok) stDegis { it.copy(sonYedek = simdiMs()) }; return }
        bekliyor = false
        if (r.ok && (r.islem == "google" || r.islem == "eposta")) kapsam.launch { delay(300); esitle() }
        if (!r.ok) { if (r.hata.isNotBlank()) bilgi(r.hata); return }
        when (r.islem) {
            "misafir" -> "Misafir olarak devam ediyorsun."; "google" -> "Google hesabınla giriş yaptın."; "eposta" -> "Giriş yapıldı."
            "sifirla" -> "Şifre sıfırlama bağlantısı e-postana gönderildi."; "cikis" -> "Çıkış yapıldı."; "sil" -> "Hesabın silindi."; else -> null
        }?.let { toast = it }
        if (r.islem == "sifirla") return
        stDegis { it.copy(girisGoruldu = true) }
        if (r.islem == "cikis" || r.islem == "sil") return
        yigin.clear(); yigin.add(Ekran.Ana)
    }

    /**
     * Gösterim modu (yalnız simülatör testinde mağaza ekran görüntüleri için; iOS CI ortam değişkeni yazar).
     * Örnek ilerleme doldurur ve istenen ekranı açar. Normal kullanımda hiç çağrılmaz.
     */
    private fun gosterim(e: String) {
        val r = Random(7)
        val c = sorular.filter { r.nextDouble() < 0.55 }.associate { q ->
            val dogru = r.nextDouble() < 0.82
            q.i to listOf(if (dogru) 1L else 0L, if (dogru) 0L else 1L, if (dogru) 1L else 0L, simdiMs())
        }
        st = st.copy(c = c, k = sorular.take(14).map { it.i })
        when (e) {
            "konular" -> git(Ekran.Konular)
            "istatistik" -> git(Ekran.Istatistik)
            "ders" -> git(Ekran.DersE(1))
            "konu" -> konular.firstOrNull { it.d == 1 }?.let { git(Ekran.KonuE(it.id)) }
            "test", "sinav" -> {
                val resimli = sorular.filter { it.g != null && it.t == 1 && it.d == 1 }.take(1)
                if (e == "sinav") simulasyon() else testBaslat("pratik", "Trafik ve Çevre", resimli + sorular.filter { it.d == 1 && it.g == null }.take(19))
                if (e == "test") sikSec(resimli.firstOrNull()?.k ?: 0)
                if (e == "sinav") test?.let { t -> testDegis(t.copy(i = 6, cevap = t.cevap.mapIndexed { i, x -> if (i < 6) BY[t.ids[i]]!!.k else x })) }
            }
        }
    }

    companion object { val HIZLAR = listOf(0.8, 1.0, 1.2, 1.4) }
}

/** "26 Kasım" */
fun gunAy(ms: Long): String { val t = yerelTarih(ms); return "${t[2]} ${AYLAR[t[1] - 1]}" }
fun gunAySaat(ms: Long): String { val t = yerelTarih(ms); return "${t[2]} ${AYLAR[t[1] - 1]} ${t[3].toString().padStart(2, '0')}:${t[4].toString().padStart(2, '0')}" }
