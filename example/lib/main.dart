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
              // Replace with your dynamic URLs
              String modelUrl =
                  "https://firebasestorage.googleapis.com/v0/b/arsample-595f2.appspot.com/o/Food%2FModels%2Fhandpainted_watercolor_cake.glb?alt=media&token=74a6a7d9-2c4d-4807-833c-073ae8ba69d4";
              String imageUrl =
                  "https://firebasestorage.googleapis.com/v0/b/arsample-595f2.appspot.com/o/Furniture%2Fqr.jpeg?alt=media&token=b39cb577-6f5e-42b6-8172-c2194da5ec27";

              FlutterArPlugin.launchARView(
                  modelUrl: modelUrl, imageUrl: imageUrl, scaleFactor: 5);
            },
            child: Text("Launch AR"),
          ),
        ),
      ),
    );
  }
}
