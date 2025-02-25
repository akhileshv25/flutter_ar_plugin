import Foundation
import ARKit
import GLTFSceneKit
import Combine

class ArModelBuilder: NSObject {
    
    func makeNodeFromWebGlb(modelURL: String) -> Future<SCNNode?, Never> {
        return Future { promise in
            guard let url = URL(string: modelURL) else {
                print("Invalid URL: \(modelURL)")
                promise(.success(nil))
                return
            }
            
            let task = URLSession.shared.downloadTask(with: url) { (fileURL, _, error) in
                guard let fileURL = fileURL, error == nil else {
                    print("Download failed: \(error?.localizedDescription ?? "Unknown error")")
                    promise(.success(nil))
                    return
                }
                
                do {
                    let sceneSource = try GLTFSceneSource(url: fileURL)
                    let scene = try sceneSource.scene()
                    let node = SCNNode()
                    
                    for child in scene.rootNode.childNodes {
                        child.scale = SCNVector3(0.01, 0.01, 0.01)
                        node.addChildNode(child.flattenedClone())
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
