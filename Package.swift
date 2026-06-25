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
            url: "https://github.com/ronjunevaldoz/graphyn-editor/releases/download/v0.4.1/GraphynEditor.xcframework.zip",
            checksum: "1fcf9ef38a733c58b94ebbae457d7f94008a1e6d6c753b444463cc8cf321efad"
        ),
    ]
)
