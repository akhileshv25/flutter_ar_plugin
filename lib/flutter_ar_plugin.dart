import 'package:flutter/services.dart';

class FlutterArPlugin {
  static const MethodChannel _channel = MethodChannel('flutter_ar_plugin');

  static Future<void> launchARView(
      {required String modelUrl,
      required String imageUrl,
      required double scaleFactor, // Add scale factor
      String loadingUrl =
          "https://firebasestorage.googleapis.com/v0/b/arsample-595f2.appspot.com/o/Food%2Flodingicream.json?alt=media&token=2519ffa6-2c51-4cfb-9bc8-10841cbf6c23"}) async {
    await _channel.invokeMethod('launchARView', {
      'modelUrl': modelUrl,
      'imageUrl': imageUrl,
      'scaleFactor': scaleFactor,
      'loadingUrl': loadingUrl,
    });
  }
}
