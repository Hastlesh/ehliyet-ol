package com.celikgames.ehliyetol

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
fun App(d: UygulamaDurum) {
    EhliyetTema(d.st.tema) {
        Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
            Column(Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.safeDrawing)) {
            Box(Modifier.weight(1f).fillMaxWidth()) {
                key(d.ekran) {
                    when (val e = d.ekran) {
                        Ekran.Yukleniyor -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            if (d.yuklemeHatasi) Soluk("Sorular yüklenemedi.") else CircularProgressIndicator()
                        }
                        Ekran.Giris -> GirisEkrani(d)
                        Ekran.Eposta -> EpostaEkrani(d)
                        Ekran.Ana -> AnaEkran(d)
                        Ekran.Konular -> KonularEkrani(d)
                        Ekran.Istatistik -> IstatistikEkrani(d)
                        is Ekran.DersE -> DersEkrani(d, e.d)
                        is Ekran.KonuE -> KonuEkrani(d, e.id)
                        Ekran.TestE -> TestEkrani(d)
                        Ekran.Sonuc -> SonucEkrani(d)
                    }
                }
                // Kısa bildirim (toast)
                val mesaj = d.toast
                if (mesaj != null) {
                    LaunchedEffect(mesaj) { kotlinx.coroutines.delay(2500); d.toast = null }
                    Snackbar(Modifier.align(Alignment.BottomCenter).padding(16.dp).padding(bottom = 72.dp)) { Text(mesaj) }
                }
                if (d.bekliyor) Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Surface(shape = MaterialTheme.shapes.large, tonalElevation = 6.dp, shadowElevation = 6.dp) {
                        Row(Modifier.padding(20.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                            CircularProgressIndicator(Modifier.size(28.dp)); Text("Lütfen bekle…")
                        }
                    }
                }
            }
            // Şerit reklam: hep aynı görünüm (ekran değişince yeniden yüklenmez); test/sınav, konu dinleme ve girişte gizli
            Box(if (d.reklamIzinli) Modifier.fillMaxWidth() else Modifier.fillMaxWidth().height(0.dp)) { ReklamSeridi() }
            }
            Konfeti(d.konfeti)
            d.uyari?.let { u ->
                AlertDialog(
                    onDismissRequest = { d.uyari = null },
                    title = if (u.baslik.isNotBlank()) ({ Text(u.baslik, fontWeight = FontWeight.Bold) }) else null,
                    text = { Text(u.mesaj) },
                    confirmButton = {
                        if (u.evet != null) TextButton({ d.uyari = null; u.is_?.invoke() }) { Text(u.evet) }
                        else TextButton({ d.uyari = null }) { Text("Tamam") }
                    },
                    dismissButton = if (u.evet != null) ({ TextButton({ d.uyari = null }) { Text("Vazgeç") } }) else null,
                )
            }
        }
    }
}
