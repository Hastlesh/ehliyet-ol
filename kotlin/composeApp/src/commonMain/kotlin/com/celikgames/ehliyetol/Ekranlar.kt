package com.celikgames.ehliyetol

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.celikgames.ehliyetol.kaynak.Res
import com.celikgames.ehliyetol.kaynak.logo
import org.jetbrains.compose.resources.painterResource

/* ---------- giriş ---------- */
@Composable
fun GirisEkrani(d: UygulamaDurum) {
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(14.dp, Alignment.CenterVertically),
    ) {
        Image(painterResource(Res.drawable.logo), null, Modifier.size(104.dp).clip(RoundedCornerShape(24.dp)))
        Text("Ehliyet Ol'a hoş geldin", fontSize = 26.sp, fontWeight = FontWeight.ExtraBold, textAlign = TextAlign.Center)
        Soluk(
            if (Hesap.destekli) "MEB'in çıkmış sorularıyla e-sınava hazırlan. İstersen giriş yap, istersen giriş yapmadan her şeyi kullan."
            else "MEB'in çıkmış sorularıyla e-sınava hazırlan. Giriş gerekmez; ilerlemen bu telefonda saklanır.",
            Modifier.padding(bottom = 8.dp), 15,
        )
        if (Hesap.destekli) {
            Button({ d.hesapIslem("google") }, Modifier.fillMaxWidth().height(50.dp)) { Icon(Icons.Rounded.AccountCircle, null); Spacer(Modifier.width(8.dp)); Text("Google ile devam et") }
            OutlinedButton({ d.git(Ekran.Eposta) }, Modifier.fillMaxWidth().height(50.dp)) { Icon(Icons.Rounded.Mail, null); Spacer(Modifier.width(8.dp)); Text("E-posta ile devam et") }
            TextButton({ d.misafir() }, Modifier.fillMaxWidth()) { Text("Misafir olarak devam et") }
        } else {
            Button({ d.misafir() }, Modifier.fillMaxWidth().height(50.dp)) { Text("Başla") }
        }
        Soluk("Gizlilik politikası: hastlesh.github.io/ehliyet-ol", Modifier.padding(top = 8.dp), 12)
    }
}

private val EPOSTA_RE = Regex("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$")

@Composable
fun EpostaEkrani(d: UygulamaDurum) {
    var ep by remember { mutableStateOf("") }
    var sf by remember { mutableStateOf("") }
    var goster by remember { mutableStateOf(false) }
    fun gonder(islem: String) {
        when {
            !EPOSTA_RE.matches(ep.trim()) -> d.bilgi("Geçerli bir e-posta adresi yaz.")
            sf.length < 6 -> d.bilgi("Şifre en az 6 karakter olmalı.")
            else -> d.hesapIslem(islem, ep, sf)
        }
    }
    Sayfa(d, "E-posta ile giriş", geri = true) {
        OutlinedTextField(ep, { ep = it.take(200) }, Modifier.fillMaxWidth(), label = { Text("E-posta") }, singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email))
        OutlinedTextField(sf, { sf = it.take(128) }, Modifier.fillMaxWidth(), label = { Text("Şifre (en az 6 karakter)") }, singleLine = true,
            visualTransformation = if (goster) VisualTransformation.None else PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            trailingIcon = { IconButton({ goster = !goster }) { Icon(if (goster) Icons.Rounded.VisibilityOff else Icons.Rounded.Visibility, if (goster) "Şifreyi gizle" else "Şifreyi göster") } })
        Button({ gonder("giris") }, Modifier.fillMaxWidth().height(48.dp)) { Text("Giriş yap") }
        OutlinedButton({ gonder("kayit") }, Modifier.fillMaxWidth().height(48.dp)) { Text("Yeni hesap oluştur") }
        TextButton({ if (!EPOSTA_RE.matches(ep.trim())) d.bilgi("Önce e-posta adresini yaz.") else d.hesapIslem("sifirla", ep) }, Modifier.fillMaxWidth()) { Text("Şifremi unuttum") }
    }
}

/* ---------- ana sayfa ---------- */
@Composable
fun AnaEkran(d: UygulamaDurum) {
    val st = d.st
    val h = remember(st.c) { d.hazirlik() }
    val son = st.sim.lastOrNull()
    val yn = remember(st.c) { d.yanlislar().size }
    Sayfa(d, "Ehliyet Ol", sekme = Ekran.Ana) {
        d.test?.takeIf { !it.bitti }?.let { t ->
            Kart {
                Soluk("Devam eden")
                Text(t.baslik, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                Soluk("${t.cevap.count { it >= 0 }}/${t.ids.size} soru cevaplandı")
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton({ d.birak() }, Modifier.weight(1f)) { Text("Bırak") }
                    Button({ d.git(Ekran.TestE) }, Modifier.weight(1f)) { Text("Devam et") }
                }
            }
        }
        Kart {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                Halka(h / 100f, MaterialTheme.colorScheme.primary, 92.dp) { Text("%$h", fontWeight = FontWeight.ExtraBold, fontSize = 22.sp) }
                Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    Text("Tahmini hazırlık", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                    Soluk((if (st.c.isNotEmpty()) "${st.c.size} farklı soru çözdün" else "Henüz soru çözmedin") + (son?.let { " · son sınavın ${it.p}" } ?: ""))
                    Soluk("Geçme puanı 70 · 50 soru · 45 dakika")
                }
            }
            Button({ d.simulasyon() }, Modifier.fillMaxWidth().height(50.dp)) { Icon(Icons.Rounded.Description, null); Spacer(Modifier.width(8.dp)); Text("Sınav simülasyonu başlat") }
        }
        Bolum("Dersler")
        DERSLER.indices.chunked(2).forEach { satir ->
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.height(IntrinsicSize.Max)) {
                satir.forEach { i ->
                    val s = remember(st.c, d.sorular.size) { d.dersIstat(i) }
                    val r = dersRenk(i)
                    Kart(Modifier.weight(1f).fillMaxHeight(), tikla = { d.git(Ekran.DersE(i)) }) {
                        Box(Modifier.size(40.dp).clip(RoundedCornerShape(12.dp)).background(r.copy(alpha = 0.14f)), contentAlignment = Alignment.Center) {
                            Icon(DERS_IKON[i], null, tint = r)
                        }
                        Text(DERSLER[i].ad, fontWeight = FontWeight.Bold, fontSize = 15.sp, lineHeight = 19.sp)
                        Spacer(Modifier.weight(1f))
                        Soluk("${s.gorulen}/${s.toplam} soru · %${kotlin.math.round(s.oran * 100).toInt()} doğru", boyut = 12)
                        Cubuk(s.gorulen.toFloat() / maxOf(1, s.toplam), r)
                    }
                }
            }
        }
        Bolum("Çalış")
        Liste {
            Satir("Yanlışlarım", "Yanlış yaptığın sorularla test", Icons.Rounded.Cancel, LocalRenk.current.yanlis, rozet = if (yn > 0) "$yn" else null) { d.yanlislarim() }
            Ayrac()
            Satir("En çok çıkan sorular", "Sınavlarda 3+ kez sorulanlar", Icons.Rounded.LocalFireDepartment, Color(0xFFE8590C)) { d.enCokCikan() }
            Ayrac()
            Satir("Kaydettiklerim", "${st.k.size} soru", Icons.Rounded.Star, LocalRenk.current.uyari) { d.kaydettiklerim() }
            Ayrac()
            Satir("Konu anlatımı", "${d.konular.size} konu · sesli dinle", Icons.Rounded.Headphones) { d.git(Ekran.Konular) }
        }
        Soluk("Sorular, Millî Eğitim Bakanlığının 2012–2016 yıllarında yayımladığı sürücü sınavlarından alınmıştır; güncelliği şüpheli sorular çıkarılmıştır. " +
            "Konu anlatımları ve Trafik Adabı alıştırma soruları uygulamaya aittir. Bu uygulama resmî değildir; MEB ile bağlantısı yoktur.", Modifier.padding(4.dp), 12)
    }
}

/* ---------- ders ---------- */
@Composable
fun DersEkrani(d: UygulamaDurum, i: Int) {
    val D = DERSLER.getOrNull(i) ?: return
    val s = remember(d.st.c) { d.dersIstat(i) }
    val r = dersRenk(i)
    Sayfa(d, D.ad, geri = true) {
        Kart {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                Box(Modifier.size(44.dp).clip(RoundedCornerShape(12.dp)).background(r.copy(alpha = 0.14f)), contentAlignment = Alignment.Center) { Icon(DERS_IKON[i], null, tint = r) }
                Column {
                    Text("${s.gorulen}/${s.toplam} soru çözüldü", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    Soluk("%${kotlin.math.round(s.oran * 100).toInt()} doğru · sınavda ${D.sim} soru")
                }
            }
            Cubuk(s.gorulen.toFloat() / maxOf(1, s.toplam), r, Modifier.padding(top = 6.dp))
        }
        Button({ d.dersTest(i) }, Modifier.fillMaxWidth().height(48.dp)) { Icon(Icons.Rounded.PlayArrow, null); Spacer(Modifier.width(6.dp)); Text("20 soruluk test çöz") }
        if (s.yanlis > 0) OutlinedButton({ d.dersYanlis(i) }, Modifier.fillMaxWidth().height(48.dp)) { Text("Bu dersteki yanlışlarım (${s.yanlis})") }
        Bolum("Konular")
        Liste {
            d.konular.filter { it.d == i }.forEachIndexed { n, k ->
                if (n > 0) Ayrac()
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.padding(start = 16.dp).size(32.dp).clip(RoundedCornerShape(10.dp)).background(r.copy(alpha = 0.14f)), contentAlignment = Alignment.Center) {
                        Text("${n + 1}", color = r, fontWeight = FontWeight.Bold)
                    }
                    Box(Modifier.weight(1f)) { Satir(k.b, "Dinle · ${d.konuSoruSayisi(k)} ilgili soru") { d.git(Ekran.KonuE(k.id)) } }
                }
            }
        }
    }
}

@Composable
fun KonularEkrani(d: UygulamaDurum) {
    Sayfa(d, "Konu anlatımı", sekme = Ekran.Konular) {
        DERSLER.forEachIndexed { i, D ->
            Bolum(D.ad, dersRenk(i), DERS_IKON[i])
            Liste {
                d.konular.filter { it.d == i }.forEachIndexed { n, k ->
                    if (n > 0) Ayrac()
                    Satir(k.b) { d.git(Ekran.KonuE(k.id)) }
                }
            }
        }
    }
}

@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
fun KonuEkrani(d: UygulamaDurum, id: String) {
    val k = d.konular.find { it.id == id } ?: return
    val dk = d.konular.filter { it.d == k.d }
    val sonraki = dk.getOrNull(dk.indexOf(k) + 1)
    val havuz = remember(k.id) { d.konuSoruSayisi(k) }
    val ses = remember { d.sesVar() }
    val r = dersRenk(k.d)
    val istekler = remember(k.id) { k.p.map { BringIntoViewRequester() } }
    LaunchedEffect(d.okunan) { d.okunan?.removePrefix("p")?.toIntOrNull()?.let { istekler.getOrNull(it)?.bringIntoView() } }
    Sayfa(d, k.b, geri = true, alt = if (!ses) null else ({
        Surface(color = MaterialTheme.colorScheme.surface, shadowElevation = 4.dp) {
            Row(Modifier.fillMaxWidth().padding(12.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Button({ d.konuDinle(k) }, Modifier.weight(1f).height(48.dp)) {
                    Icon(if (d.sesCalisiyor) Icons.Rounded.Pause else Icons.Rounded.PlayArrow, null); Spacer(Modifier.width(6.dp))
                    Text(if (d.sesCalisiyor) "Durdur" else "Dinle")
                }
                OutlinedButton({ d.hizDegis() }, Modifier.height(48.dp)) { Text("${hizYaz(d.st.hiz)}×") }
            }
        }
    })) {
        Kart {
            Etiket(DERSLER[k.d].ad, r)
            k.p.forEachIndexed { i, p ->
                val okunuyor = d.okunan == "p$i"
                Text(p, Modifier.bringIntoViewRequester(istekler[i]).clip(RoundedCornerShape(8.dp))
                    .background(if (okunuyor) MaterialTheme.colorScheme.primaryContainer else Color.Transparent).padding(horizontal = 4.dp, vertical = 2.dp),
                    fontSize = 16.sp, lineHeight = 25.sp)
            }
            if (!ses) Soluk("Sesli okuma için telefonda Türkçe ses motoru gerekli (Ayarlar › Metin okuma).")
        }
        if (havuz > 0) Button({ d.konuTest(k) }, Modifier.fillMaxWidth().height(48.dp)) { Icon(Icons.Rounded.PlayArrow, null); Spacer(Modifier.width(6.dp)); Text("Bu konudan soru çöz (${minOf(20, havuz)})") }
        if (sonraki != null) TextButton({ d.sus(); d.geri(); d.git(Ekran.KonuE(sonraki.id)) }, Modifier.fillMaxWidth()) { Text("Sonraki: ${sonraki.b}", textAlign = TextAlign.Center); Icon(Icons.Rounded.ChevronRight, null) }
    }
}

fun hizYaz(h: Double): String = if (h == 1.0) "1" else h.toString()
