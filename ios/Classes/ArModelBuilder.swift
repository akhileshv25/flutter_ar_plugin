import Foundation
import ARKit
import GLTFSceneKit
import Combine

class ArModelBuilder: NSObject {
    
    func makeNodeFromWebGlb(modelURL: String, progressHandler: @escaping (Float) -> Void) -> Future<SCNNode?, Never> {
        return Future { promise in
            guard let url = URL(string: modelURL) else {
                print("Invalid URL: \(modelURL)")
                promise(.success(nil))
                return
            }
            
            let task = URLSession.shared.downloadTask(with: url) { (fileURL, response, error) in
                guard let fileURL = fileURL, error == nil else {
                    print("Download failed: \(error?.localizedDescription ?? "Unknown error")")
                    promise(.success(nil))
                    return
                }
                
                do {
                    let sceneSource = try GLTFSceneSource(url: fileURL)
                    let scene = try sceneSource.scene()
                    let node = SCNNode()
                    
                    let children = scene.rootNode.childNodes
                    let totalChildren = Float(children.count)
                    var loadedChildren: Float = 0
                    
                    if totalChildren > 0 {
                        for child in children {
                            child.scale = SCNVector3(0.01, 0.01, 0.01)
                            node.addChildNode(child.flattenedClone())

                            loadedChildren += 1
                            let progress = loadedChildren / totalChildren
                            
                            DispatchQueue.main.async {
                                progressHandler(progress)
                            }
                        }
                    }
                    
                    // Ensure progress reaches 100% at the end
                    DispatchQueue.main.async {
                        progressHandler(1.0)
                    }
                    
                    promise(.success(node))
                } catch {
                    print("Failed to load GLB: \(error.localizedDescription)")
                    promise(.success(nil))
                }
            }
            task.resume()
        }
    }
}
