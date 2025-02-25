import Flutter
import UIKit

public class FlutterArPlugin: NSObject, FlutterPlugin {
    
    public static func register(with registrar: FlutterPluginRegistrar) {
        let channel = FlutterMethodChannel(name: "flutter_ar_plugin", binaryMessenger: registrar.messenger())
        let instance = FlutterArPlugin()
        registrar.addMethodCallDelegate(instance, channel: channel)
    }

    public func handle(_ call: FlutterMethodCall, result: @escaping FlutterResult) {
        if call.method == "launchARView" {
            guard let args = call.arguments as? [String: Any],
                  let modelUrl = args["modelUrl"] as? String,
                  let imageUrl = args["imageUrl"] as? String,
                  let loadingUrl = args["loadingUrl"] as? String,
                  let scaleFactor = args["scaleFactor"] as? Double else {
                result(FlutterError(code: "INVALID_ARGUMENTS", message: "Invalid arguments", details: nil))
                return
            }

            DispatchQueue.main.async { // Ensure UI updates happen on the main thread
                if let topController = self.getTopViewController() {
                    let arViewController = ARViewController(
                        modelUrl: modelUrl,
                        imageUrl: imageUrl,
                        scaleFactor: scaleFactor,
                        loadingUrl: loadingUrl
                    )
                    topController.present(arViewController, animated: true, completion: nil)
                    result(nil)
                } else {
                    result(FlutterError(code: "NO_ROOT_VIEW_CONTROLLER", message: "Unable to find root view controller", details: nil))
                }
            }
        } else {
            result(FlutterMethodNotImplemented)
        }
    }

    /// Helper function to get the top-most view controller
    private func getTopViewController() -> UIViewController? {
        guard let rootViewController = UIApplication.shared.keyWindow?.rootViewController else {
            return nil
        }
        var topController: UIViewController = rootViewController
        while let presentedViewController = topController.presentedViewController {
            topController = presentedViewController
        }
        return topController
    }
}
