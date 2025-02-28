package com.example.flutter_ar_plugin

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.lifecycleScope
import com.google.ar.core.*
import com.google.ar.sceneform.AnchorNode
import com.google.ar.sceneform.FrameTime
import com.google.ar.sceneform.Scene
import com.google.ar.sceneform.math.Vector3
import com.google.ar.sceneform.rendering.ModelRenderable
import com.google.ar.sceneform.ux.ArFragment
import com.google.ar.sceneform.ux.TransformableNode
import kotlinx.coroutines.*
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL
import javax.net.ssl.HttpsURLConnection
import com.google.ar.sceneform.assets.RenderableSource
import io.flutter.plugin.common.MethodChannel
import io.flutter.plugin.common.BinaryMessenger
import io.flutter.embedding.engine.FlutterEngine
import io.flutter.embedding.engine.dart.DartExecutor
import android.widget.FrameLayout



class ARViewController : FragmentActivity(), Scene.OnUpdateListener {

    private lateinit var arFragment: ArFragment
    private lateinit var modelUrl: String
    private lateinit var imageUrl: String
    private var scaleFactor: Float = 1.0f
    private var coachingOverlay: View? = null
    private var loadingIndicator: ProgressBar? = null
    private var loadingLabel: TextView? = null
    private var modelLoaded = false
    private var logFile: File? = null
// Model builder
    private var modelBuilder = ArModelBuilder()
    private lateinit var objectManagerChannel: MethodChannel

   


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
   logFile = File(getExternalFilesDir(null), "ar_plugin_logs.txt") 
    val messenger = FlutterEngine(this).dartExecutor.binaryMessenger
        objectManagerChannel = MethodChannel(messenger, "ar_plugin/object_manager")

    logMessage("ARViewController","onCreate: ARViewController started")

        Log.d("ARViewController", "onCreate: Retrieving parameters from intent")
        modelUrl = intent.getStringExtra("MODEL_URL") ?: ""
        imageUrl = intent.getStringExtra("IMAGE_URL") ?: ""
        scaleFactor = intent.getFloatExtra("SCALE_FACTOR", 1.0f)
        logMessage("ARViewController","Model URL: $modelUrl, Image URL: $imageUrl, Scale Factor: $scaleFactor")


        Log.d("ARViewController", "Model URL: $modelUrl, Image URL: $imageUrl, Scale Factor: $scaleFactor")

        val arLayout = FrameLayout(this).apply {
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
        }

        arFragment = ArFragment()
        supportFragmentManager.beginTransaction()
            .replace(android.R.id.content, arFragment)
            .commit()

       coachingOverlay = View(this).apply {
    layoutParams = FrameLayout.LayoutParams(
        FrameLayout.LayoutParams.MATCH_PARENT,
        FrameLayout.LayoutParams.MATCH_PARENT
    )
    setBackgroundColor(resources.getColor(android.R.color.darker_gray))
    visibility = View.GONE
}

arLayout.addView(coachingOverlay)

        setContentView(arLayout)

        supportFragmentManager.executePendingTransactions()

        lifecycleScope.launch {
            waitForArSceneView()
            arFragment.arSceneView.scene.addOnUpdateListener(this@ARViewController)
            loadReferenceImage()
        }
    }

    private suspend fun waitForArSceneView() {
        Log.d("ARViewController", "Waiting for AR SceneView to initialize")
        logMessage("ARViewController", "Waiting for AR SceneView to initialize")
        while (!this@ARViewController::arFragment.isInitialized || arFragment.arSceneView == null) {
            delay(100)
        }
        Log.d("ARViewController", "AR SceneView is ready")
        logMessage("ARViewController", "AR SceneView is ready")
    }

    private fun loadReferenceImage() {
        Log.d("ARViewController", "Downloading reference image")
        logMessage("ARViewController"," Downloading reference image")
        downloadImage(imageUrl) { bitmap ->
            if (bitmap != null) {
                Log.d("ARViewController", "Reference image downloaded successfully")
                
                logMessage("ARViewController", "Reference image downloaded successfully")
                setupARSession(bitmap)
            } else {
                Log.e("ARViewController", "Failed to download reference image")
                logMessage("ARViewController", "Failed to download reference image")
            }
        }
    }

    private fun setupARSession(bitmap: Bitmap) {
    Log.d("ARViewController", "Setting up AR session with augmented image database")
    logMessage("ARViewController", "Setting up AR session with augmented image database")

    val session = arFragment.arSceneView.session ?: return
    val config = Config(session)

    val augmentedImageDb = AugmentedImageDatabase(session)
    
    if (bitmap.width > 0 && bitmap.height > 0) {
        augmentedImageDb.addImage("TargetQR", bitmap)
    } else {
        Log.e("ARViewController", "Invalid bitmap. Augmented Image Database setup failed.")
        logMessage("ARViewController", "Invalid bitmap. Augmented Image Database setup failed.")
        return
    }

    config.augmentedImageDatabase = augmentedImageDb
    config.planeFindingMode = Config.PlaneFindingMode.DISABLED
    config.updateMode = Config.UpdateMode.LATEST_CAMERA_IMAGE

    session.configure(config)
    arFragment.arSceneView.setupSession(session)

    Log.d("ARViewController", "AR session configured successfully")
    logMessage("ARViewController", "AR session configured successfully")
}


  override fun onUpdate(frameTime: FrameTime) {
    val frame = arFragment.arSceneView.arFrame ?: return
    val updatedImages = frame.getUpdatedTrackables(AugmentedImage::class.java)

    for (image in updatedImages) {
        Log.d("ARViewController", "Tracking state: ${image.trackingState}")
        logMessage("ARViewController", "QR Code detected! Loading model...")

        if (image.trackingState == TrackingState.TRACKING && image.name == "TargetQR") {
            if (!modelLoaded) {
                Log.d("ARViewController", "QR Code detected! Loading model...")
                logMessage("ARViewController", "QR Code detected! Loading model...")
                modelLoaded = true
                runOnUiThread { coachingOverlay?.visibility = View.GONE }
                runOnUiThread { showLoadingIndicator() }

                // Ensure scaleFactor is applied correctly
               val transformation = arrayListOf(
    scaleFactor.toDouble(), 0.0, 0.0, 0.0,
    0.0, scaleFactor.toDouble(), 0.0, 0.0,
    0.0, 0.0, scaleFactor.toDouble(), 0.0,
    0.0, 0.0, 0.0, 1.0
)



                // Validate the model URL before attempting to load
                if (modelUrl.isNotEmpty() && Uri.parse(modelUrl) != null) {
                    modelBuilder.makeNodeFromGlb(
                        this,
                        arFragment.transformationSystem,
                        objectManagerChannel,
                        true,   // Can transform
                        true,   // Allow collision
                        "ModelNode",
                        modelUrl,
                        transformation
                    ).thenAccept { node ->
                        runOnUiThread {
                            val anchor = image.createAnchor(image.centerPose)
                            val anchorNode = AnchorNode(anchor).apply {
                                setParent(arFragment.arSceneView.scene)
                            }
                            anchorNode.addChild(node)
                            hideLoadingIndicator()
                            Log.d("ARViewController", "Model placed successfully")
                            logMessage("ARViewController", "Model placed successfully")
                        }
                    }.exceptionally {
                        Log.e("ARViewController", "Model loading failed: ${it.message}")
                        logMessage("ARViewController", "Model loading failed: ${it.message}")
                        modelLoaded = false
                        hideLoadingIndicator()
                        null
                    }
                } else {
                    Log.e("ARViewController", "Invalid model URL: $modelUrl")
                    logMessage("ARViewController", "Invalid model URL: $modelUrl")
                    modelLoaded = false
                    hideLoadingIndicator()
                }
            }
        } else if (image.trackingState == TrackingState.STOPPED) {
            Log.d("ARViewController", "QR Code lost")
            logMessage("ARViewController", "QR Code lost")
            modelLoaded = false
        }
    }
}






    private fun downloadImage(url: String, callback: (Bitmap?) -> Unit) {
    lifecycleScope.launch(Dispatchers.IO) {
        try {
            Log.d("ARViewController", "Downloading image from: $url")
            logMessage("ARViewController", "Downloading image from: $url")
            val connection = URL(url).openConnection() as HttpsURLConnection
            connection.doInput = true
            connection.connect()
            val bitmap = BitmapFactory.decodeStream(connection.inputStream)

            if (bitmap != null) {
                Log.d("ARViewController", "Image downloaded successfully. Width: ${bitmap.width}, Height: ${bitmap.height}")
                logMessage("ARViewController", "Image downloaded successfully. Width: ${bitmap.width}, Height: ${bitmap.height}")
            } else {
                Log.e("ARViewController", "Failed to decode bitmap")
                logMessage("ARViewController", "Failed to decode bitmap")
            }

            withContext(Dispatchers.Main) { callback(bitmap) }
        } catch (e: Exception) {
            Log.e("ARViewController", "Image download failed", e)
            logMessage("ARViewController", "Image download failed ${e.message}")
            withContext(Dispatchers.Main) { callback(null) }
        }
    }
}



   private fun showLoadingIndicator() {
    runOnUiThread {
        val layout = arFragment.view as ViewGroup

        // Ensure views are removed before re-adding
        loadingIndicator?.parent?.let { (it as ViewGroup).removeView(loadingIndicator) }
        loadingLabel?.parent?.let { (it as ViewGroup).removeView(loadingLabel) }

        if (loadingIndicator == null) {
            loadingIndicator = ProgressBar(this@ARViewController).apply {
                layoutParams = FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.WRAP_CONTENT,
                    FrameLayout.LayoutParams.WRAP_CONTENT
                ).apply {
                    gravity = android.view.Gravity.CENTER
                }
            }
        }

        if (loadingLabel == null) {
            loadingLabel = TextView(this@ARViewController).apply {
                text = "Loading..."
                setTextColor(resources.getColor(android.R.color.white))
                layoutParams = FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.WRAP_CONTENT,
                    FrameLayout.LayoutParams.WRAP_CONTENT
                ).apply {
                    gravity = android.view.Gravity.CENTER_HORIZONTAL or android.view.Gravity.BOTTOM
                    bottomMargin = 50
                }
            }
        }

        layout.addView(loadingIndicator)
        layout.addView(loadingLabel)

        loadingIndicator?.visibility = View.VISIBLE
        loadingLabel?.visibility = View.VISIBLE
    }
}





    private fun hideLoadingIndicator() {
    runOnUiThread {
        loadingIndicator?.visibility = View.GONE
        loadingLabel?.visibility = View.GONE
    }
}

    private fun logMessage(tag: String, message: String) {
    try {
        logFile?.appendText("${System.currentTimeMillis()} - [$tag] $message\n")
    } catch (e: Exception) {
        Log.e(tag, "Failed to write log: $message", e)
    }
}
}
