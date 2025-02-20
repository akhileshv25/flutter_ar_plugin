import UIKit
import ARKit
import SceneKit
import Combine

class ARViewController: UIViewController, ARSCNViewDelegate {
    
    var sceneView = ARSCNView()
    var referenceImages = Set<ARReferenceImage>()
    var modelBuilder: ArModelBuilder!
    var cancellables: Set<AnyCancellable> = []
    
    var modelUrl: String
    var imageUrl: String
    var scaleFactor: Float

    let loadingLabel = UILabel()

    init(modelUrl: String, imageUrl: String, scaleFactor: Double) {
        self.modelUrl = modelUrl
        self.imageUrl = imageUrl
        self.scaleFactor = Float(scaleFactor)
        super.init(nibName: nil, bundle: nil)
    }

    required init?(coder: NSCoder) {
        fatalError("init(coder:) has not been implemented")
    }

    override func viewDidLoad() {
        super.viewDidLoad()
        modelBuilder = ArModelBuilder()
        setupSceneView()
        setupLoadingUI()
        loadReferenceImage()
    }
    
    private func setupSceneView() {
        sceneView.delegate = self
        sceneView.frame = view.bounds
        view.addSubview(sceneView)
        sceneView.autoenablesDefaultLighting = true
        sceneView.automaticallyUpdatesLighting = true
    }

    private func setupLoadingUI() {
        loadingLabel.text = "Loading... 0%"
        loadingLabel.textAlignment = .center
        loadingLabel.backgroundColor = UIColor.black.withAlphaComponent(0.7)
        loadingLabel.textColor = .white
        loadingLabel.frame = CGRect(x: 0, y: view.frame.height - 100, width: view.frame.width, height: 50)
        loadingLabel.isHidden = true // Hide initially
        view.addSubview(loadingLabel)
    }

    private func loadReferenceImage() {
        downloadImage(from: imageUrl) { image in
            guard let image = image,
                  let referenceImage = self.createARReferenceImage(from: image, physicalWidth: 0.15) else { return }
            
            self.referenceImages.insert(referenceImage)
            self.setupARSession()
        }
    }
    
    private func setupARSession() {
        let configuration = ARWorldTrackingConfiguration()
        configuration.detectionImages = referenceImages
        sceneView.session.run(configuration, options: [.resetTracking, .removeExistingAnchors])
    }

    func renderer(_ renderer: SCNSceneRenderer, didAdd node: SCNNode, for anchor: ARAnchor) {
        guard let imageAnchor = anchor as? ARImageAnchor else { return }

        DispatchQueue.main.async {
            self.loadingLabel.isHidden = false // Show label when model starts loading
            self.loadingLabel.text = "Loading... 0%"
        }

        modelBuilder.makeNodeFromWebGlb(modelURL: modelUrl) { progress in
            DispatchQueue.main.async {
                self.loadingLabel.text = "Loading... \(Int(progress * 100))%"
            }
        }
        .receive(on: DispatchQueue.main) // Ensure UI updates happen on the main thread
        .sink(receiveCompletion: { completion in
            switch completion {
            case .failure(let error):
                print("Model loading failed: \(error)")
                DispatchQueue.main.async {
                    self.loadingLabel.text = "Failed to load"
                }
            case .finished:
                DispatchQueue.main.async {
                    self.loadingLabel.isHidden = true
                }
            }
        }, receiveValue: { [weak self] modelNode in
            guard let self = self, let modelNode = modelNode else { return }

            // Apply dynamic scale factor
            modelNode.scale = SCNVector3(self.scaleFactor, self.scaleFactor, self.scaleFactor)
            modelNode.position = SCNVector3Zero
            modelNode.eulerAngles = SCNVector3(-Float.pi/2, 0, 0)

            node.addChildNode(modelNode)

            DispatchQueue.main.async {
                self.loadingLabel.isHidden = true // Hide label when loading completes
            }
        })
        .store(in: &cancellables)
    }

    private func downloadImage(from url: String, completion: @escaping (UIImage?) -> Void) {
        guard let url = URL(string: url) else {
            completion(nil)
            return
        }
        
        URLSession.shared.dataTask(with: url) { data, _, error in
            guard let data = data, error == nil else {
                completion(nil)
                return
            }
            completion(UIImage(data: data))
        }.resume()
    }
    
    private func createARReferenceImage(from image: UIImage, physicalWidth: CGFloat) -> ARReferenceImage? {
        guard let cgImage = image.cgImage else { return nil }
        let referenceImage = ARReferenceImage(cgImage, orientation: .up, physicalWidth: physicalWidth)
        referenceImage.name = "TargetQR"
        return referenceImage
    }
}
