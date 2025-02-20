import 'package:plugin_platform_interface/plugin_platform_interface.dart';

import 'flutter_ar_plugin_method_channel.dart';

abstract class FlutterArPluginPlatform extends PlatformInterface {
  /// Constructs a FlutterArPluginPlatform.
  FlutterArPluginPlatform() : super(token: _token);

  static final Object _token = Object();

  static FlutterArPluginPlatform _instance = MethodChannelFlutterArPlugin();

  /// The default instance of [FlutterArPluginPlatform] to use.
  ///
  /// Defaults to [MethodChannelFlutterArPlugin].
  static FlutterArPluginPlatform get instance => _instance;

  /// Platform-specific implementations should set this with their own
  /// platform-specific class that extends [FlutterArPluginPlatform] when
  /// they register themselves.
  static set instance(FlutterArPluginPlatform instance) {
    PlatformInterface.verifyToken(instance, _token);
    _instance = instance;
  }

  Future<String?> getPlatformVersion() {
    throw UnimplementedError('platformVersion() has not been implemented.');
  }
}
