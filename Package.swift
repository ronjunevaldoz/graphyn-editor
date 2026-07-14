// swift-tools-version:5.9
// Updated automatically by the xcframework CI workflow on each release.
import PackageDescription

let package = Package(
    name: "GraphynEditor",
    platforms: [
        .iOS(.v16),
    ],
    products: [
        .library(
            name: "GraphynEditor",
            targets: ["GraphynEditor"]
        ),
    ],
    targets: [
        .binaryTarget(
            name: "GraphynEditor",
            url: "https://github.com/ronjunevaldoz/graphyn-editor/releases/download/v0.9.2/GraphynEditor.xcframework.zip",
            checksum: "1d3ef32c970c071dc4ea28bbfb0c74944693a8ed856de1a00fec64efec521a87"
        ),
    ]
)
