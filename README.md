# flutter_ar_plugin


`flutter_ar_plugin` is a Flutter plugin that allows users to load and display 3D models dynamically using ARKit. The plugin supports loading models and reference images from URLs and provides options for setting the model's scale factor.

## Features

- Load 3D models (`.glb` format) dynamically from a URL.

- Detect and track images as AR anchors.

- Apply a dynamic scale factor to the loaded model.

- Simple API to launch AR from Flutter.

## Getting started

To use this package, add flutter_ar_plugin as a dependency in your `pubspec.yaml` file:
```dart
    dependencies:
  flutter:
    sdk: flutter
  flutter_ar_plugin: latest
```

## Usage


Import the Plugin
```dart
    import 'package:flutter_ar_plugin/flutter_ar_plugin.dart';
```
Minimal example:
Launch AR View

Use the `FlutterArPlugin.launchARView` method to load a model dynamically:
```dart
FlutterArPlugin.launchARView(
  modelUrl: "https://yourstorage.com/path-to-model.glb",
  imageUrl: "https://yourstorage.com/path-to-image.jpg",
  scaleFactor: 5.0, // Adjust the scale as needed
);
```

## Example

```dart
    import 'package:flutter/material.dart';
import 'package:flutter_ar_plugin/flutter_ar_plugin.dart';

void main() {
  runApp(MyApp());
}

class MyApp extends StatelessWidget {
  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      home: Scaffold(
        appBar: AppBar(title: Text("AR Plugin Example")),
        body: Center(
          child: ElevatedButton(
            onPressed: () {
              String modelUrl = "https://yourstorage.com/path-to-model.glb";
              String imageUrl = "https://yourstorage.com/path-to-image.jpg";
              FlutterArPlugin.launchARView(
                  modelUrl: modelUrl, imageUrl: imageUrl, scaleFactor: 5.0);
            },
            child: Text("Launch AR"),
          ),
        ),
      ),
    );
  }
}
```

## See also

 - [github repo](https://github.com/akhileshv25/flutter_ar_plugin)
 - [pub.dev package](https://pub.dev/packages/flutter_ar_plugin)
 - [api reference](https://pub.dev/documentation/flutter_ar_plugin)
