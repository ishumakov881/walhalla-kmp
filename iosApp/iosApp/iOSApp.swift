import SwiftUI
import UserNotifications
import FirebaseCore
import FirebaseMessaging
import ComposeApp

class AppDelegate: NSObject, UIApplicationDelegate, UNUserNotificationCenterDelegate {
    func application(
        _ application: UIApplication,
        didFinishLaunchingWithOptions launchOptions: [UIApplication.LaunchOptionsKey: Any]? = nil
    ) -> Bool {
        FirebaseApp.configure()

        NotifierManager.shared.initialize(
            configuration: NotificationPlatformConfigurationIos(
                showPushNotification: true,
                askNotificationPermissionOnStart: true,
                notificationSoundName: nil
            )
        )

        UNUserNotificationCenter.current().delegate = self

        if let remote = launchOptions?[.remoteNotification] as? [AnyHashable: Any] {
            NotifierManager.shared.onApplicationDidReceiveRemoteNotification(userInfo: remote)
            savePush(userInfo: remote, openInbox: true)
        }

        return true
    }

    func application(
        _ application: UIApplication,
        didRegisterForRemoteNotificationsWithDeviceToken deviceToken: Data
    ) {
        Messaging.messaging().apnsToken = deviceToken
    }

    func application(
        _ application: UIApplication,
        didReceiveRemoteNotification userInfo: [AnyHashable: Any]
    ) async -> UIBackgroundFetchResult {
        NotifierManager.shared.onApplicationDidReceiveRemoteNotification(userInfo: userInfo)
        savePush(userInfo: userInfo, openInbox: false)
        return UIBackgroundFetchResult.newData
    }

    func userNotificationCenter(
        _ center: UNUserNotificationCenter,
        willPresent notification: UNNotification,
        withCompletionHandler completionHandler: @escaping (UNNotificationPresentationOptions) -> Void
    ) {
        let userInfo = notification.request.content.userInfo
        NotifierManager.shared.onApplicationDidReceiveRemoteNotification(userInfo: userInfo)
        savePush(userInfo: userInfo, openInbox: false)
        completionHandler([.banner, .sound, .badge])
    }

    func userNotificationCenter(
        _ center: UNUserNotificationCenter,
        didReceive response: UNNotificationResponse,
        withCompletionHandler completionHandler: @escaping () -> Void
    ) {
        let userInfo = response.notification.request.content.userInfo
        NotifierManager.shared.onApplicationDidReceiveRemoteNotification(userInfo: userInfo)
        savePush(userInfo: userInfo, openInbox: true)
        completionHandler()
    }

    private func savePush(userInfo: [AnyHashable: Any], openInbox: Bool) {
        var sentTimeSec = Int64(Date().timeIntervalSince1970)
        if let raw = userInfo["google.sent_time"] as? NSNumber {
            let value = raw.int64Value
            sentTimeSec = value > 1_000_000_000_000 ? value / 1000 : value
        }
        PushIosBridgeKt.handleRemotePushUserInfo(
            userInfo: PushUserInfoMapper.toKotlinMap(userInfo),
            sentTimeSec: sentTimeSec
        )
        if openInbox {
            PushIosBridgeKt.requestOpenPushInbox()
        }
    }
}

private enum PushUserInfoMapper {
    static func toKotlinMap(_ userInfo: [AnyHashable: Any]) -> [String: Any] {
        var map: [String: Any] = [:]
        for (key, value) in userInfo {
            guard let key = key as? String else { continue }
            map[key] = normalize(value)
        }
        return map
    }

    private static func normalize(_ value: Any) -> Any {
        switch value {
        case let dict as [AnyHashable: Any]:
            var nested: [String: Any] = [:]
            for (key, nestedValue) in dict {
                guard let key = key as? String else { continue }
                nested[key] = normalize(nestedValue)
            }
            return nested
        case let number as NSNumber:
            return number
        default:
            return value
        }
    }
}

@main
struct iOSApp: App {
    @UIApplicationDelegateAdaptor(AppDelegate.self) var delegate

    var body: some Scene {
        WindowGroup {
            ContentView()
        }
    }
}
