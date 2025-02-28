package com.example.flutter_ar_plugin

import android.R
import android.app.Activity
import android.content.Context
import com.google.ar.sceneform.Node
import com.google.ar.sceneform.math.Vector3
import com.google.ar.sceneform.math.Quaternion
import com.google.ar.sceneform.assets.RenderableSource

import java.util.concurrent.CompletableFuture
import android.net.Uri
import android.view.Gravity
import android.widget.Toast
import com.google.ar.core.*
import com.google.ar.sceneform.ArSceneView
import com.google.ar.sceneform.FrameTime
import com.google.ar.sceneform.math.MathHelper
import com.google.ar.sceneform.rendering.*
import com.google.ar.sceneform.utilities.Preconditions
import com.google.ar.sceneform.ux.*

import io.flutter.FlutterInjector
import io.flutter.embedding.engine.loader.FlutterLoader
import io.flutter.plugin.common.BinaryMessenger
import io.flutter.plugin.common.MethodCall
import io.flutter.plugin.common.MethodChannel
import java.security.AccessController


// Responsible for creating Renderables and Nodes
class ArModelBuilder {


    // Creates a node form a given glb model path or URL. The gltf asset loading in Sceneform is asynchronous, so the function returns a compleatable future of type Node
    fun makeNodeFromGlb(
        context: Context,
        transformationSystem: TransformationSystem,
        objectManagerChannel: MethodChannel,  // Correct parameter type
        enablePans: Boolean,
        enableRotation: Boolean,
        name: String,
        modelPath: String,
        transformation: ArrayList<Double>
    ): CompletableFuture<CustomTransformableNode> {
        val completableFutureNode = CompletableFuture<CustomTransformableNode>()
        val gltfNode = CustomTransformableNode(transformationSystem, objectManagerChannel, enablePans, enableRotation)

        ModelRenderable.builder()
            .setSource(
                context,
                RenderableSource.builder()
                    .setSource(context, Uri.parse(modelPath), RenderableSource.SourceType.GLB)
                    .build()
            )
            .setRegistryId(modelPath)
            .build()
            .thenAccept { renderable ->
                gltfNode.renderable = renderable
                gltfNode.name = name
                val transform = deserializeMatrix4(transformation)
                gltfNode.worldScale = transform.first
                gltfNode.worldPosition = transform.second
                gltfNode.worldRotation = transform.third
                completableFutureNode.complete(gltfNode)
            }
            .exceptionally { throwable ->
                completableFutureNode.completeExceptionally(throwable)
                null
            }

        return completableFutureNode
    }
}

class CustomTransformableNode(transformationSystem: TransformationSystem, objectManagerChannel: MethodChannel, enablePans: Boolean, enableRotation: Boolean) :
    TransformableNode(transformationSystem) { //

    private lateinit var customTranslationController: CustomTranslationController

    private lateinit var customRotationController: CustomRotationController

    init {
        // Remove standard controllers
        translationController.isEnabled = false
        rotationController.isEnabled = false
        scaleController.isEnabled = false
        removeTransformationController(translationController)
        removeTransformationController(rotationController)
        removeTransformationController(scaleController)


        // Add custom controllers if needed
        if (enablePans) {
            customTranslationController = CustomTranslationController(
                this,
                transformationSystem.dragRecognizer,
                objectManagerChannel
            )
            addTransformationController(customTranslationController)
        }
        if (enableRotation) {
            customRotationController = CustomRotationController(
                this,
                transformationSystem.twistRecognizer,
                objectManagerChannel
            )
            addTransformationController(customRotationController)
        }
    }
}

class CustomTranslationController(transformableNode: BaseTransformableNode, gestureRecognizer: DragGestureRecognizer, objectManagerChannel: MethodChannel) :
    TranslationController(transformableNode, gestureRecognizer) {

    val platformChannel: MethodChannel = objectManagerChannel

    override fun canStartTransformation(gesture: DragGesture): Boolean {
        platformChannel.invokeMethod("onPanStart", transformableNode.name)
        super.canStartTransformation(gesture)
        return transformableNode.isSelected
    }

    override fun onContinueTransformation(gesture: DragGesture) {
        platformChannel.invokeMethod("onPanChange", transformableNode.name)
        super.onContinueTransformation(gesture)
        }

    override fun onEndTransformation(gesture: DragGesture) {
        val serializedLocalTransformation = serializeLocalTransformation(transformableNode)
        platformChannel.invokeMethod("onPanEnd", serializedLocalTransformation)
        super.onEndTransformation(gesture)
     }
}

class CustomRotationController(transformableNode: BaseTransformableNode, gestureRecognizer: TwistGestureRecognizer, objectManagerChannel: MethodChannel) :
    RotationController(transformableNode, gestureRecognizer) {

    val platformChannel: MethodChannel = objectManagerChannel

    override fun canStartTransformation(gesture: TwistGesture): Boolean {
        platformChannel.invokeMethod("onRotationStart", transformableNode.name)
        super.canStartTransformation(gesture)
        return transformableNode.isSelected
    }

    override fun onContinueTransformation(gesture: TwistGesture) {
        platformChannel.invokeMethod("onRotationChange", transformableNode.name)
        super.onContinueTransformation(gesture)
    }

    override fun onEndTransformation(gesture: TwistGesture) {
        val serializedLocalTransformation = serializeLocalTransformation(transformableNode)
        platformChannel.invokeMethod("onRotationEnd", serializedLocalTransformation)
        super.onEndTransformation(gesture)
     }
}