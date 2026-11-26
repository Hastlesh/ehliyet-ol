import UIKit
import AppTrackingTransparency
import GoogleMobileAds
import ComposeApp

/// Kotlin'in IosKopru arayüzünün iPhone karşılığı: Google Mobile Ads.
final class Kopru: NSObject, IosKopru {
    static let paylasilan = Kopru()

    // Reklam birimleri: "Ehliyet Ol iOS" AdMob'da açılınca gerçek birimler buraya yazılacak. Şimdilik her iki derlemede Google'ın test birimleri.
    private let seritBirimi = "ca-app-pub-3940256099942544/2435281174"
    private let gecisBirimi = "ca-app-pub-3940256099942544/4411468910"
    private var gecis: GADInterstitialAd?
    private var reklamBasladi = false

    /// Gösterim modu (yalnız simülatör testi, CI ortam değişkeni): reklam ve takip izni penceresi yok — mağaza ekran görüntüleri için
    static let gosterim = ProcessInfo.processInfo.environment["EHLIYETOL_GOSTERIM"] == "1"

    /// Önce Apple'ın takip izni sorulur (iOS 14.5+ zorunlu), cevap ne olursa olsun reklamlar başlar (izin yoksa kişiselleştirilmemiş).
    func reklamlariBaslat() {
        guard !reklamBasladi, !Kopru.gosterim else { return }
        reklamBasladi = true
        ATTrackingManager.requestTrackingAuthorization { _ in
            DispatchQueue.main.async {
                GADMobileAds.sharedInstance().start { _ in self.gecisYukle() }
            }
        }
    }
    private func gecisYukle() {
        GADInterstitialAd.load(withAdUnitID: gecisBirimi, request: GADRequest()) { ad, _ in self.gecis = ad }
    }
    func seritGorunum() -> UIView {
        if Kopru.gosterim { return UIView() }
        let b = GADBannerView(adSize: GADCurrentOrientationAnchoredAdaptiveBannerAdSizeWithWidth(UIScreen.main.bounds.width))
        b.adUnitID = seritBirimi
        b.rootViewController = Kopru.ustDenetleyici()
        b.load(GADRequest())
        return b
    }
    func gecisGoster() -> Bool {
        guard let ad = gecis, let kok = Kopru.ustDenetleyici() else { gecisYukle(); return false }
        gecis = nil
        ad.present(fromRootViewController: kok)
        DispatchQueue.main.asyncAfter(deadline: .now() + 2) { self.gecisYukle() }
        return true
    }
    static func ustDenetleyici() -> UIViewController? {
        let sahne = UIApplication.shared.connectedScenes.compactMap { $0 as? UIWindowScene }.first
        var v = sahne?.windows.first(where: { $0.isKeyWindow })?.rootViewController
        while let p = v?.presentedViewController { v = p }
        return v
    }
}
