import 'package:flutter/services.dart';

class FlutterArPlugin {
  static const MethodChannel _channel = MethodChannel('flutter_ar_plugin');

  static Future<void> launchARView({
    required String modelUrl,
    required String imageUrl,
    required double scaleFactor,
  }) async {
    await _channel.invokeMethod('launchARView', {
      'modelUrl': modelUrl,
      'imageUrl': imageUrl,
      'scaleFactor': scaleFactor,
    });
  }
}
