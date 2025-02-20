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
              let scaleFactor = args["scaleFactor"] as? Double else {
            result(FlutterError(code: "INVALID_ARGUMENTS", message: "Invalid arguments", details: nil))
            return
        }

        if let rootViewController = UIApplication.shared.delegate?.window??.rootViewController {
            let arViewController = ARViewController(modelUrl: modelUrl, imageUrl: imageUrl, scaleFactor: scaleFactor)
            rootViewController.present(arViewController, animated: true, completion: nil)
        }
        result(nil)
    } else {
        result(FlutterMethodNotImplemented)
    }
}

}
