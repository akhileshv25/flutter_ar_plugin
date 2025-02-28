import 'package:flutter_test/flutter_test.dart';
import 'package:flutter_ar_plugin/flutter_ar_plugin.dart';
import 'package:flutter_ar_plugin/flutter_ar_plugin_platform_interface.dart';
import 'package:flutter_ar_plugin/flutter_ar_plugin_method_channel.dart';
import 'package:plugin_platform_interface/plugin_platform_interface.dart';

class MockFlutterArPluginPlatform
    with MockPlatformInterfaceMixin
    implements FlutterArPluginPlatform {
  @override
  Future<String?> getPlatformVersion() => Future.value('42');
}

void main() {
  final FlutterArPluginPlatform initialPlatform =
      FlutterArPluginPlatform.instance;

  test('$MethodChannelFlutterArPlugin is the default instance', () {
    expect(initialPlatform, isInstanceOf<MethodChannelFlutterArPlugin>());
  });

  test('getPlatformVersion', () async {
    FlutterArPlugin flutterArPlugin = FlutterArPlugin();
    MockFlutterArPluginPlatform fakePlatform = MockFlutterArPluginPlatform();
    FlutterArPluginPlatform.instance = fakePlatform;

    // expect(await flutterArPlugin.getPlatformVersion(), '42');
  });
}
