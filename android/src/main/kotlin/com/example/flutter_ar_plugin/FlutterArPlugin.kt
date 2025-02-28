package com.example.flutter_ar_plugin

import android.app.Activity
import android.content.Context
import android.content.Intent
import androidx.annotation.NonNull
import io.flutter.embedding.engine.plugins.FlutterPlugin
import io.flutter.embedding.engine.plugins.activity.ActivityAware
import io.flutter.embedding.engine.plugins.activity.ActivityPluginBinding
import io.flutter.plugin.common.MethodCall
import io.flutter.plugin.common.MethodChannel
import io.flutter.plugin.common.MethodChannel.MethodCallHandler
import io.flutter.plugin.common.MethodChannel.Result

/** FlutterArPlugin */
class FlutterArPlugin : FlutterPlugin, MethodCallHandler, ActivityAware {

    private lateinit var channel: MethodChannel
    private var context: Context? = null
    private var activity: Activity? = null

    override fun onAttachedToEngine(@NonNull flutterPluginBinding: FlutterPlugin.FlutterPluginBinding) {
        channel = MethodChannel(flutterPluginBinding.binaryMessenger, "flutter_ar_plugin")
        channel.setMethodCallHandler(this)
        context = flutterPluginBinding.applicationContext
    }

    override fun onMethodCall(@NonNull call: MethodCall, @NonNull result: Result) {
        when (call.method) {
            "launchARView" -> {
                val modelUrl = call.argument<String>("modelUrl")
                val imageUrl = call.argument<String>("imageUrl")
                val scaleFactor = call.argument<Double>("scaleFactor") ?: 1.0

                if (modelUrl.isNullOrEmpty() || imageUrl.isNullOrEmpty()) {
                    result.error("INVALID_ARGUMENTS", "Model URL or Image URL is missing", null)
                    return
                }

                if (activity == null) {
                    result.error("NO_ACTIVITY", "Activity is null, cannot launch AR view", null)
                    return
                }

                launchARActivity(modelUrl, imageUrl, scaleFactor)
                result.success("AR Activity launched")
            }
            else -> {
                result.notImplemented()
            }
        }
    }

    private fun launchARActivity(modelUrl: String, imageUrl: String, scaleFactor: Double) {
        activity?.let {
            val intent = Intent(it, ARViewController::class.java).apply {
                putExtra("MODEL_URL", modelUrl)
                putExtra("IMAGE_URL", imageUrl)
                putExtra("SCALE_FACTOR", scaleFactor.toFloat()) // Ensured correct type conversion
            }
            it.startActivity(intent)
        } ?: run {
            println("Error: Activity is null, cannot launch ARActivity")
        }
    }

    override fun onDetachedFromEngine(@NonNull binding: FlutterPlugin.FlutterPluginBinding) {
        channel.setMethodCallHandler(null)
        context = null
    }

    override fun onAttachedToActivity(@NonNull binding: ActivityPluginBinding) {
        activity = binding.activity
    }

    override fun onDetachedFromActivityForConfigChanges() {
        activity = null
    }

    override fun onReattachedToActivityForConfigChanges(@NonNull binding: ActivityPluginBinding) {
        activity = binding.activity
    }

    override fun onDetachedFromActivity() {
        activity = null
    }
}
