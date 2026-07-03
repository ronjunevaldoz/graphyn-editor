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
            url: "https://github.com/ronjunevaldoz/graphyn-editor/releases/download/v0.7.8/GraphynEditor.xcframework.zip",
            checksum: "859a06051600f10e5f4561f68f55ad7212df725c9a2de082c577e0f2f5536557"
        ),
    ]
)
