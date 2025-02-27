import UIKit
import ARKit
import SceneKit
import Combine
import AudioToolbox

class ARViewController: UIViewController, ARSCNViewDelegate, ARCoachingOverlayViewDelegate {
    
    var sceneView = ARSCNView()
    var referenceImages = Set<ARReferenceImage>()
    var modelBuilder: ArModelBuilder!
    var cancellables: Set<AnyCancellable> = []
    var coachingOverlay: ARCoachingOverlayView?
    var loadingIndicator: UIActivityIndicatorView?
    var loadingLabel: UILabel?

    var modelUrl: String
    var imageUrl: String
    var scaleFactor: Float
    
    init(modelUrl: String, imageUrl: String, scaleFactor: Double) {
        self.modelUrl = modelUrl
        self.imageUrl = imageUrl
        self.scaleFactor = Float(scaleFactor)
        super.init(nibName: nil, bundle: nil)
        modalPresentationStyle = .fullScreen // Ensures full-screen AR view
    }
    
    required init?(coder: NSCoder) {
        fatalError("init(coder:) has not been implemented")
    }
    
    override func viewDidLoad() {
        super.viewDidLoad()
        modelBuilder = ArModelBuilder()
        setupSceneView()
        setupBackButton()
        loadReferenceImage()
    }
    
    override func viewWillDisappear(_ animated: Bool) {
        super.viewWillDisappear(animated)
        sceneView.session.pause() // Free resources when leaving the view
    }
    
    private func setupSceneView() {
        sceneView.delegate = self
        sceneView.frame = view.bounds
        view.addSubview(sceneView)
        sceneView.autoenablesDefaultLighting = true
        sceneView.automaticallyUpdatesLighting = true

        let coachingOverlay = ARCoachingOverlayView()
        self.coachingOverlay = coachingOverlay
        coachingOverlay.delegate = self
        coachingOverlay.session = sceneView.session
        coachingOverlay.translatesAutoresizingMaskIntoConstraints = false
        sceneView.addSubview(coachingOverlay)
        
        NSLayoutConstraint.activate([
            coachingOverlay.topAnchor.constraint(equalTo: sceneView.topAnchor),
            coachingOverlay.bottomAnchor.constraint(equalTo: sceneView.bottomAnchor),
            coachingOverlay.leadingAnchor.constraint(equalTo: sceneView.leadingAnchor),
            coachingOverlay.trailingAnchor.constraint(equalTo: sceneView.trailingAnchor)
        ])
        
        coachingOverlay.goal = .tracking
        coachingOverlay.activatesAutomatically = true
        
        DispatchQueue.main.asyncAfter(deadline: .now() + 1) {
            self.coachingOverlay?.setActive(true, animated: true)
        }
    }
    
    private func loadReferenceImage() {
        downloadImage(from: imageUrl) { [weak self] image in
            guard let self = self, let image = image,
                  let referenceImage = self.createARReferenceImage(from: image, physicalWidth: 0.15) else {
                print("⚠️ Reference image creation failed!")
                return
            }
            self.referenceImages.insert(referenceImage)
            self.setupARSession()
        }
    }
    
    private func setupARSession() {
        let configuration = ARWorldTrackingConfiguration()
        configuration.detectionImages = referenceImages
        configuration.maximumNumberOfTrackedImages = 1
        sceneView.session.run(configuration, options: [.resetTracking, .removeExistingAnchors])

        DispatchQueue.main.asyncAfter(deadline: .now() + 1) {
            self.coachingOverlay?.setActive(true, animated: true)
        }
    }
    
    func renderer(_ renderer: SCNSceneRenderer, didAdd node: SCNNode, for anchor: ARAnchor) {
        guard let imageAnchor = anchor as? ARImageAnchor else { return }
        
        triggerVibration()
        showLoadingIndicator()

        modelBuilder.makeNodeFromWebGlb(modelURL: modelUrl)
            .receive(on: DispatchQueue.main)
            .sink(receiveCompletion: { completion in
                switch completion {
                case .failure(let error):
                    print("⚠️ Model loading failed: \(error)")
                    DispatchQueue.main.async {
                        self.loadingLabel?.text = "Failed to load"
                    }
                case .finished:
                    DispatchQueue.main.async {
                        self.loadingLabel?.isHidden = true
                    }
                }
            }, receiveValue: { [weak self] modelNode in
                guard let self = self, let modelNode = modelNode else { return }
                DispatchQueue.main.async {
                    self.hideLoadingIndicator()
                }
                
                let referenceSize = imageAnchor.referenceImage.physicalSize
                let fixedScale = self.scaleFactor / Float(referenceSize.width)
                modelNode.scale = SCNVector3(fixedScale, fixedScale, fixedScale)

                modelNode.position = SCNVector3(0, -Float(referenceSize.height) / 2, 0)
                modelNode.eulerAngles = SCNVector3(0, 0, 0)
                modelNode.opacity = 0
                
                let fadeIn = SCNAction.fadeIn(duration: 1.0)
                modelNode.runAction(fadeIn)
                
                let rotateAction = SCNAction.rotateBy(x: 0, y: CGFloat.pi * 2, z: 0, duration: 30)
                let repeatRotation = SCNAction.repeatForever(rotateAction)
                modelNode.runAction(repeatRotation)
                
                node.addChildNode(modelNode)
            })
            .store(in: &cancellables)
    }
    
    private func triggerVibration() {
        AudioServicesPlaySystemSound(kSystemSoundID_Vibrate)
    }
    
    private func setupBackButton() {
        let backButton = UIButton(type: .system)
        backButton.setTitle("Back", for: .normal)
        backButton.setTitleColor(.white, for: .normal)
        backButton.backgroundColor = UIColor.black.withAlphaComponent(0.5)
        backButton.layer.cornerRadius = 10
        backButton.addTarget(self, action: #selector(dismissARView), for: .touchUpInside)

        backButton.translatesAutoresizingMaskIntoConstraints = false
        view.addSubview(backButton)

        NSLayoutConstraint.activate([
            backButton.topAnchor.constraint(equalTo: view.safeAreaLayoutGuide.topAnchor, constant: 10),
            backButton.leadingAnchor.constraint(equalTo: view.leadingAnchor, constant: 10),
            backButton.widthAnchor.constraint(equalToConstant: 60),
            backButton.heightAnchor.constraint(equalToConstant: 30)
        ])
    }

    @objc private func dismissARView() {
        sceneView.session.pause() // Pause AR session before dismissing
        dismiss(animated: true, completion: nil)
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

    private func showLoadingIndicator() {
        DispatchQueue.main.async {
            self.loadingIndicator = UIActivityIndicatorView(style: .large)
            self.loadingIndicator?.center = self.view.center
            self.loadingIndicator?.hidesWhenStopped = true
            self.view.addSubview(self.loadingIndicator!)
            self.loadingIndicator?.startAnimating()
            self.loadingLabel = UILabel()
            self.loadingLabel?.text = "Loading..."
            self.loadingLabel?.textColor = .white
            self.loadingLabel?.textAlignment = .center
            self.loadingLabel?.frame = CGRect(x: 0, y: self.view.center.y + 30, width: self.view.frame.width, height: 30)
            self.view.addSubview(self.loadingLabel!)
        }
    }

    private func hideLoadingIndicator() {
        DispatchQueue.main.async {
            self.loadingIndicator?.stopAnimating()
            self.loadingIndicator?.removeFromSuperview()
            self.loadingIndicator = nil
            self.loadingLabel?.removeFromSuperview()
            self.loadingLabel = nil
        }
    }
    
    private func createARReferenceImage(from image: UIImage, physicalWidth: CGFloat) -> ARReferenceImage? {
        guard let cgImage = image.cgImage else { return nil }
        let referenceImage = ARReferenceImage(cgImage, orientation: .up, physicalWidth: physicalWidth)
        referenceImage.name = "TargetQR"
        return referenceImage
    }
}
