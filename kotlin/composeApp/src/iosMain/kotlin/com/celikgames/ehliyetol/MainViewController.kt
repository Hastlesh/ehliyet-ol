package com.celikgames.ehliyetol

import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.window.ComposeUIViewController
import platform.Foundation.NSProcessInfo

/** iOS uygulamasının giriş noktası (iosApp/Sources/EhliyetOlApp.swift). kopru: Swift'teki iPhone kütüphaneleri köprüsü. */
fun MainViewController(kopru: IosKopru?) = ComposeUIViewController {
    IosAyar.kopru = kopru
    val kapsam = rememberCoroutineScope()
    val durum = remember {
        gosterimHazirla()
        UygulamaDurum(kapsam).also { d -> ortam("EHLIYETOL_TEMA")?.let { d.temaAyarla(it) } }
    }
    App(durum)
}

/**
 * Gösterim modu — YALNIZ simülatör testinde (CI: SIMCTL_CHILD_* ortam değişkenleri). Normal açılışta bu değişkenler yoktur.
 *  EHLIYETOL_ORNEK=1  → örnek ilerleme yazılır (ekran görüntüleri dolu görünsün)
 *  EHLIYETOL_EKRAN=ana|konular|istatistik|test|konu → o ekran açılır (UygulamaDurum yüklenince)
 */
private fun ortam(ad: String): String? = NSProcessInfo.processInfo.environment[ad] as? String

private fun gosterimHazirla() {
    if (ortam("EHLIYETOL_ORNEK") != "1") return
    val simdi = simdiMs()
    // Örnek ilerleme: soru kimlikleri yüklenince eşleşmeyenler zaten sayılmaz; birkaç sınav kaydı
    Depo.yaz("ilerleme", JSON.encodeToString(Ilerleme.serializer(), Ilerleme(
        girisGoruldu = true,
        sim = listOf(SinavKaydi(simdi - 3 * 86_400_000L, 64, s = 2210), SinavKaydi(simdi - 86_400_000L, 78, s = 1980), SinavKaydi(simdi - 3_600_000L, 86, s = 1745)),
    )))
    Depo.yaz("ekranGosterim", ortam("EHLIYETOL_EKRAN") ?: "ana")
}
