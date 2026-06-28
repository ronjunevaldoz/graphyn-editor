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
            url: "https://github.com/ronjunevaldoz/graphyn-editor/releases/download/v0.7.6/GraphynEditor.xcframework.zip",
            checksum: "4930106b6b8dbf732aa128efc9854242d935ab3caa3d5db70902a2d6a7879b8d"
        ),
    ]
)
