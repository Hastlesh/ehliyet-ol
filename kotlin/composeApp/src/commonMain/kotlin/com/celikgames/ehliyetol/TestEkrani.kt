package com.celikgames.ehliyetol

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.*
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun TestEkrani(d: UygulamaDurum) {
    val t = d.test ?: run { LaunchedEffect(Unit) { d.git(Ekran.Ana) }; return }
    if (t.bitti && t.tur != "incele") { LaunchedEffect(Unit) { d.incelemeBitti() }; return }
    val q = d.soru(t.ids[t.i]) ?: return
    val R = LocalRenk.current
    val n = t.ids.size
    val secim = t.cevap[t.i]
    val sinav = t.tur == "sinav"; val incele = t.tur == "incele"
    val goster = incele || (t.tur == "pratik" && secim >= 0)
    val kayitli = q.i in d.st.k
    val resimli = q.g != null
    val ses = remember { d.sesVar() }

    // Sınav sayacı: her saniye; süre bitince sınav kendiliğinden biter
    var simdi by remember { mutableLongStateOf(simdiMs()) }
    if (sinav) LaunchedEffect(t.bas) { while (true) { simdi = simdiMs(); d.sureDenetle(); delay(1000) } }
    val kalan = (t.bitis - simdi) / 1000

    val kaydirma = key(t.i) { rememberScrollState() }
    Sayfa(d, t.baslik, geri = true, kaydirma = kaydirma, eylemler = {
        if (sinav) Text(sureYaz(kalan), Modifier.padding(end = 4.dp).clip(RoundedCornerShape(50))
            .background(if (kalan < 300) R.yanlisZemin else MaterialTheme.colorScheme.surfaceVariant).padding(horizontal = 10.dp, vertical = 4.dp),
            color = if (kalan < 300) R.yanlis else MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold, fontSize = 14.sp)
        if (ses) IconButton({ d.soruOku() }) { Icon(if (d.sesCalisiyor) Icons.Rounded.StopCircle else Icons.AutoMirrored.Rounded.VolumeUp, "Soruyu sesli oku") }
        IconButton({ d.kaydetDegis() }) { Icon(if (kayitli) Icons.Rounded.Star else Icons.Rounded.StarBorder, if (kayitli) "Kaydı kaldır" else "Soruyu kaydet", tint = R.uyari) }
    }, alt = {
        Surface(color = MaterialTheme.colorScheme.surface, shadowElevation = 4.dp) {
            Row(Modifier.fillMaxWidth().padding(12.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                if (t.i > 0 && (sinav || incele)) OutlinedButton({ d.onceki() }, Modifier.weight(1f).height(48.dp)) { Text("Önceki") }
                val bekle = t.tur == "pratik" && secim < 0
                when {
                    t.i < n - 1 -> Button({ d.sonraki() }, Modifier.weight(1f).height(48.dp), enabled = !bekle) { Text("Sonraki"); Icon(Icons.Rounded.ChevronRight, null) }
                    incele -> Button({ d.incelemeBitti() }, Modifier.weight(1f).height(48.dp)) { Text("Sonuca dön") }
                    else -> Button({ d.bitir(false) }, Modifier.weight(1f).height(48.dp), enabled = !bekle,
                        colors = ButtonDefaults.buttonColors(containerColor = R.dogru, contentColor = Color.White)) { Text(if (sinav) "Sınavı bitir" else "Testi bitir") }
                }
            }
        }
    }) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Etiket(DERSLER[q.d].ad, dersRenk(q.d))
            Soluk("${t.i + 1} / $n")
            if (q.n > 1) Soluk("· ${q.n} sınavda çıktı", boyut = 12)
            if (q.o == 1) Soluk("· alıştırma sorusu", boyut = 12)
        }
        LinearProgressIndicator({ (t.i + 1f) / n }, Modifier.fillMaxWidth().height(5.dp).clip(CircleShape), trackColor = MaterialTheme.colorScheme.surfaceVariant, drawStopIndicator = {})
        if (resimli && q.t == 0) SoruResmi(q.g!!, q.s)
        else Text(q.s, fontSize = 17.sp, lineHeight = 25.sp, fontWeight = FontWeight.Medium, modifier = Modifier.padding(vertical = 4.dp))
        if (resimli && q.t == 1) SoruResmi(q.g!!, null)
        q.c.forEachIndexed { i, c ->
            val durum = when {
                goster && i == q.k -> 1
                goster && i == secim -> 2
                !goster && i == secim -> 3
                else -> 0
            }
            Sik(i, c, durum, okunuyor = d.okunan == "s$i") { d.sikSec(i) }
        }
        if (goster) {
            val dogru = secim == q.k
            Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(if (dogru) R.dogruZemin else R.yanlisZemin).padding(12.dp),
                verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(if (dogru) Icons.Rounded.CheckCircle else Icons.Rounded.Cancel, null, tint = if (dogru) R.dogru else R.yanlis)
                Text(if (dogru) "Doğru cevap" else "${if (secim < 0) "Boş bırakıldı" else "Yanlış"} — doğru cevap ${HARF[q.k]}${q.c[q.k].takeIf { it.isNotBlank() }?.let { ": $it" } ?: ""}",
                    color = if (dogru) R.dogru else R.yanlis, fontWeight = FontWeight.SemiBold)
            }
        }
        if (sinav || incele) {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.padding(top = 6.dp)) {
                t.ids.forEachIndexed { i, id ->
                    val c = t.cevap[i]
                    val (zemin, yazi) = when {
                        incele && c == d.soru(id)?.k -> R.dogruZemin to R.dogru
                        incele -> R.yanlisZemin to R.yanlis
                        c >= 0 -> MaterialTheme.colorScheme.primary to MaterialTheme.colorScheme.onPrimary
                        else -> MaterialTheme.colorScheme.surface to MaterialTheme.colorScheme.onSurface
                    }
                    val s = RoundedCornerShape(10.dp)
                    Box(Modifier.size(40.dp).clip(s).background(zemin)
                        .border(if (i == t.i) 2.dp else 1.dp, if (i == t.i) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.outline, s)
                        .clickable { d.gitSoru(i) }, contentAlignment = Alignment.Center) {
                        Text("${i + 1}", color = yazi, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }
        if (sinav) TextButton({ d.bitir(false) }, Modifier.fillMaxWidth()) { Text("Sınavı bitir", color = R.yanlis) }
    }
}

/** durum: 0 normal · 1 doğru · 2 yanlış · 3 seçili */
@Composable
private fun Sik(i: Int, metin: String, durum: Int, okunuyor: Boolean, tikla: () -> Unit) {
    val R = LocalRenk.current
    val cs = MaterialTheme.colorScheme
    val (zemin, kenar) = when (durum) {
        1 -> R.dogruZemin to R.dogru
        2 -> R.yanlisZemin to R.yanlis
        3 -> R.secili to cs.primary
        else -> (if (okunuyor) cs.primaryContainer else cs.surface) to cs.outline
    }
    val s = RoundedCornerShape(14.dp)
    Row(Modifier.fillMaxWidth().clip(s).background(zemin).border(if (durum == 0) 1.dp else 2.dp, kenar, s).clickable(onClick = tikla)
        .padding(horizontal = 14.dp, vertical = 12.dp).semantics { contentDescription = "${HARF[i]} şıkkı: ${metin.ifBlank { "resimde" }}" },
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Box(Modifier.size(30.dp).clip(CircleShape).background(if (durum == 0) cs.surfaceVariant else kenar), contentAlignment = Alignment.Center) {
            Text(HARF[i].toString(), fontWeight = FontWeight.Bold, color = if (durum == 0) cs.onSurface else Color.White)
        }
        if (metin.isBlank()) Soluk("Resimdeki ${HARF[i]} şıkkı", boyut = 15) else Text(metin, fontSize = 16.sp, lineHeight = 22.sp)
    }
}

/* ---------- sonuç ---------- */
@Composable
fun SonucEkrani(d: UygulamaDurum) {
    val t = d.test?.takeIf { it.bitti } ?: run { LaunchedEffect(Unit) { d.git(Ekran.Ana) }; return }
    val R = LocalRenk.current
    val dogru = t.ders.sumOf { it[0] }; val n = t.ids.size; val sinav = t.tur == "sinav"
    val p = if (sinav) t.puan else yuzde(dogru, n)
    val gecti = p >= GECME
    Sayfa(d, "Sonuç", geri = true) {
        Kart {
            Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                Halka(p / 100f, if (gecti) R.dogru else R.yanlis, 150.dp, 14.dp) {
                    Text(if (sinav) "$p" else "$dogru/$n", fontSize = 36.sp, fontWeight = FontWeight.ExtraBold)
                }
            }
            Text(if (sinav) (if (gecti) "Tebrikler, geçtin!" else "Geçemedin — geçme puanı 70") else "%$p doğru",
                Modifier.fillMaxWidth(), textAlign = TextAlign.Center, fontSize = 20.sp, fontWeight = FontWeight.Bold,
                color = if (!sinav) Color.Unspecified else if (gecti) R.dogru else R.yanlis)
            Soluk("$dogru doğru · ${n - dogru} yanlış/boş · ${sureYaz((t.son - t.bas) / 1000)}", Modifier.fillMaxWidth().wrapContentWidth())
            t.ders.forEachIndexed { i, x ->
                if (x[1] > 0) {
                    Row(Modifier.fillMaxWidth().padding(top = 6.dp)) { Text(DERSLER[i].ad, Modifier.weight(1f), fontSize = 14.sp); Text("${x[0]}/${x[1]}", fontWeight = FontWeight.Bold) }
                    Cubuk(x[0].toFloat() / x[1], dersRenk(i))
                }
            }
        }
        Button({ d.incele() }, Modifier.fillMaxWidth().height(48.dp)) { Text("Cevapları incele") }
        OutlinedButton({ if (sinav) d.simulasyon() else d.tekrar() }, Modifier.fillMaxWidth().height(48.dp)) {
            Icon(Icons.Rounded.Refresh, null); Spacer(Modifier.width(6.dp)); Text(if (sinav) "Yeni sınav" else "Bu soruları tekrar çöz")
        }
        TextButton({ d.git(Ekran.Ana) }, Modifier.fillMaxWidth()) { Text("Ana sayfa") }
    }
}

/* ---------- istatistik ---------- */
@Composable
fun IstatistikEkrani(d: UygulamaDurum) {
    val st = d.st
    val R = LocalRenk.current
    val top = remember(st.c) { st.c.values.fold(0L to 0L) { a, c -> (a.first + c[0]) to (a.second + c[1]) } }
    Sayfa(d, "İstatistikler", sekme = Ekran.Istatistik) {
        Kart {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceAround) {
                Sayi("${st.c.size}", "farklı soru"); Sayi("${top.first + top.second}", "cevap"); Sayi("%${yuzde(top.first.toInt(), (top.first + top.second).toInt())}", "doğru")
            }
            DERSLER.indices.forEach { i ->
                val s = remember(st.c) { d.dersIstat(i) }
                Row(Modifier.fillMaxWidth().padding(top = 6.dp)) { Text(DERSLER[i].ad, Modifier.weight(1f), fontSize = 14.sp); Text("%${kotlin.math.round(s.oran * 100).toInt()}", fontWeight = FontWeight.Bold) }
                Cubuk(s.oran.toFloat(), dersRenk(i))
            }
            Soluk("Oran, her sorudaki son cevabına göre hesaplanır.", Modifier.padding(top = 6.dp), 12)
        }
        Bolum("Sınav geçmişi")
        Liste {
            if (st.sim.isEmpty()) Satir("Henüz sınav simülasyonu çözmedin.", ok = false, baslikRenk = MaterialTheme.colorScheme.onSurfaceVariant)
            st.sim.reversed().take(15).forEachIndexed { i, s ->
                if (i > 0) Ayrac()
                val gecti = s.p >= GECME
                Satir(gunAy(s.t), "${sureYaz(s.s)} sürdü", if (gecti) Icons.Rounded.EmojiEvents else Icons.Rounded.Schedule,
                    if (gecti) R.uyari else MaterialTheme.colorScheme.onSurfaceVariant, ok = false, sag = {
                        Text("${s.p}", Modifier.clip(RoundedCornerShape(50)).background(if (gecti) R.dogru else R.yanlis).padding(horizontal = 10.dp, vertical = 3.dp),
                            color = Color.White, fontWeight = FontWeight.Bold)
                    })
            }
        }
        if (Hesap.destekli) HesapBolumu(d)
        Bolum("Ayarlar")
        Liste {
            Satir("Görünüm", ikon = Icons.Rounded.Contrast, ok = false, sag = { Soluk(when (st.tema) { "acik" -> "Açık"; "koyu" -> "Koyu"; else -> "Telefona uy" }, boyut = 14) }) { d.temaDegis() }
            Ayrac()
            Satir("İlerlemeyi sıfırla", ikon = Icons.Rounded.RestartAlt, ikonRenk = R.yanlis, baslikRenk = R.yanlis, ok = false) { d.sifirla() }
        }
        Soluk("Gizlilik politikası: hastlesh.github.io/ehliyet-ol", Modifier.padding(4.dp), 12)
    }
}

@Composable
private fun Sayi(deger: String, ad: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) { Text(deger, fontSize = 24.sp, fontWeight = FontWeight.ExtraBold); Soluk(ad, boyut = 12) }
}

@Composable
private fun HesapBolumu(d: UygulamaDurum) {
    val h = d.hesap
    val R = LocalRenk.current
    val misafir = !h.giris || h.anonim
    Bolum("Hesap")
    Liste {
        Satir(if (!h.giris) "Giriş yapılmadı" else if (h.anonim) "Misafir" else h.eposta.ifBlank { h.ad.ifBlank { "Hesap" } },
            if (misafir) "Giriş yapmadan kullanıyorsun." else if (h.yontem == "google") "Google hesabı" else "E-posta hesabı",
            Icons.Rounded.AccountCircle, ok = false)
        Ayrac()
        if (misafir) {
            Satir("Giriş yap / hesap oluştur", "Giriş yaparsan ilerlemen buluta yedeklenir; telefon değiştirince kaldığın yerden devam edersin.", Icons.AutoMirrored.Rounded.Login) {
                d.yigin.clear(); d.yigin.add(Ekran.Giris)
            }
        } else {
            Satir("Bulut yedeği açık", (if (d.st.sonYedek > 0) "Son eşitleme: " + gunAySaat(d.st.sonYedek) else "Henüz eşitlenmedi") + " · şimdi eşitle",
                Icons.Rounded.CloudDone, R.dogru, ok = false) { d.toast = "Eşitleniyor…"; d.esitle() }
            Ayrac()
            Satir("Çıkış yap", ikon = Icons.AutoMirrored.Rounded.Logout, ok = false) {
                d.onay("Çıkış yapılsın mı?", "Telefondaki ilerlemen silinmez.", "Çıkış yap") { d.hesapIslem("cikis") }
            }
        }
        if (h.giris) {
            Ayrac()
            Satir("Hesabımı sil", ikon = Icons.Rounded.Delete, ikonRenk = R.yanlis, baslikRenk = R.yanlis, ok = false) {
                d.onay("Hesabın silinsin mi?", "Hesabın ve sunucudaki bilgilerin kalıcı olarak silinir. Telefondaki ilerlemen kalır. Bu geri alınamaz.", "Hesabımı sil") { d.hesapIslem("sil") }
            }
        }
    }
}
