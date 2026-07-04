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
            url: "https://github.com/ronjunevaldoz/graphyn-editor/releases/download/v0.7.9/GraphynEditor.xcframework.zip",
            checksum: "d6af56e2c8261e46cb3af035bc0b11bf8d875afe20c5713aadd2075fdb695eed"
        ),
    ]
)
