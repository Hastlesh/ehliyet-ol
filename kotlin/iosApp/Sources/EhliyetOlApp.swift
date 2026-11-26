import SwiftUI
import ComposeApp

@main
struct EhliyetOlApp: App {
    @Environment(\.scenePhase) private var evre

    var body: some Scene {
        WindowGroup {
            KotlinEkran().ignoresSafeArea()
                .onChange(of: evre) { yeni in if yeni == .active { Kopru.paylasilan.reklamlariBaslat() } }
        }
    }
}

/// Kotlin (Compose Multiplatform) arayüzünü SwiftUI içine yerleştirir
struct KotlinEkran: UIViewControllerRepresentable {
    func makeUIViewController(context: Context) -> UIViewController { MainViewControllerKt.MainViewController(kopru: Kopru.paylasilan) }
    func updateUIViewController(_ uiViewController: UIViewController, context: Context) {}
}
