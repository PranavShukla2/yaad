import BackgroundTasks
import SwiftUI
import YaadShared

/// Hosts the shared Compose UI (`App()` in app/shared) full screen, and keeps notifications scheduled: at launch,
/// whenever the app comes to the foreground, and on background app refresh.
@main
struct YaadApp: App {
    @Environment(\.scenePhase) private var scenePhase

    init() {
        let arguments = ProcessInfo.processInfo.arguments
        #if DEBUG
        YaadIos.shared.start(provisional: arguments.contains("-yaadProvisional"))
        DebugArguments.apply(arguments)
        #else
        YaadIos.shared.start(provisional: false)
        #endif
    }

    var body: some Scene {
        WindowGroup {
            ComposeView().ignoresSafeArea()
        }
        .onChange(of: scenePhase) { phase in
            switch phase {
            case .active: YaadIos.shared.onBecameActive()
            case .background:
                YaadIos.shared.onBackground()
                BackgroundRefresh.schedule()
            default: break
            }
        }
        .backgroundTask(.appRefresh(BackgroundRefresh.identifier)) {
            BackgroundRefresh.schedule()
            await withCheckedContinuation { continuation in
                YaadIos.shared.refresh { continuation.resume() }
            }
        }
    }
}

/// Refills the 64-notification window a few times a day even if Yaad is not opened (iOS decides exactly when).
enum BackgroundRefresh {
    static let identifier = "com.pranavshukla.yaad.refresh"

    static func schedule() {
        let request = BGAppRefreshTaskRequest(identifier: identifier)
        request.earliestBeginDate = Date(timeIntervalSinceNow: 4 * 60 * 60)
        try? BGTaskScheduler.shared.submit(request)
    }
}

#if DEBUG
/// Simulator checks: `xcrun simctl launch <device> com.pranavshukla.yaad -yaadProvisional -yaadAdd "Title|90|0|0"`
/// (title | due in seconds | nag minutes | repeat minutes), `-yaadAct Done` (acts on the latest delivered
/// notification as if the action were tapped) and `-yaadStatus`.
enum DebugArguments {
    static func apply(_ arguments: [String]) {
        if let index = arguments.firstIndex(of: "-yaadAdd"), index + 1 < arguments.count {
            let parts = arguments[index + 1].split(separator: "|").map(String.init)
            let number = { (i: Int) -> Int32 in i < parts.count ? Int32(parts[i]) ?? 0 : 0 }
            YaadIos.shared.debugAdd(title: parts.first ?? "Test", inSeconds: number(1), nagMinutes: number(2), everyMinutes: number(3))
        }
        if let index = arguments.firstIndex(of: "-yaadAct"), index + 1 < arguments.count {
            YaadIos.shared.debugAct(action: arguments[index + 1])
        }
        if arguments.contains("-yaadStatus") {
            YaadIos.shared.debugStatus()
        }
    }
}
#endif

private struct ComposeView: UIViewControllerRepresentable {
    func makeUIViewController(context: Context) -> UIViewController {
        MainViewControllerKt.MainViewController()
    }

    func updateUIViewController(_ uiViewController: UIViewController, context: Context) {}
}
