import SwiftUI
import YaadShared

/// Hosts the shared Compose UI (`App()` in app/shared) full screen; the Compose side handles safe areas.
@main
struct YaadApp: App {
    var body: some Scene {
        WindowGroup {
            ComposeView().ignoresSafeArea()
        }
    }
}

private struct ComposeView: UIViewControllerRepresentable {
    func makeUIViewController(context: Context) -> UIViewController {
        MainViewControllerKt.MainViewController()
    }

    func updateUIViewController(_ uiViewController: UIViewController, context: Context) {}
}
