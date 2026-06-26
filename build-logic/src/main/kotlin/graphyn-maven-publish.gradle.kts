// Single source of truth for Maven Central publishing.
//
// Every published Graphyn module applies this plugin. It owns the three settings
// that previously drifted across 11 hand-copied blocks and silently broke releases:
//
//   1. automaticRelease = true   — without it, bundles upload to the Central Portal
//      but never release to repo1.maven.org (the build still goes green).
//   2. signingInMemoryKey guard  — CI exports ORG_GRADLE_PROJECT_signingInMemoryKey;
//      guarding on the never-set `signingKey` skips signing and the portal rejects
//      the unsigned bundle.
//   3. groupId + version          — pulled from the root VERSION property in one place.
//
// Modules supply only what is genuinely unique via their own `mavenPublishing { }`
// block: coordinates(artifactId), pom.name, pom.description. The two blocks merge.

plugins {
    id("com.vanniktech.maven.publish")
    id("org.jetbrains.dokka")
}

group = "io.github.ronjunevaldoz"
version = (findProperty("VERSION") as? String) ?: "0.0.0"

mavenPublishing {
    // automaticRelease = true is the whole point — never remove it.
    publishToMavenCentral(automaticRelease = true)

    // CI sets ORG_GRADLE_PROJECT_signingInMemoryKey → Gradle property signingInMemoryKey.
    // Do NOT change this to `signingKey` — that property is never set and signing
    // would be silently skipped, producing unsigned bundles the portal rejects.
    if (project.hasProperty("signing.keyId") || project.hasProperty("signingInMemoryKey")) {
        signAllPublications()
    }

    pom {
        url.set("https://github.com/ronjunevaldoz/graphyn-editor")
        licenses {
            license {
                name.set("Apache License, Version 2.0")
                url.set("https://www.apache.org/licenses/LICENSE-2.0.txt")
                distribution.set("repo")
            }
        }
        developers {
            developer {
                id.set("ronjunevaldoz")
                name.set("Ron June Valdoz")
                email.set("ronjune.valdoz@gmail.com")
            }
        }
        scm {
            url.set("https://github.com/ronjunevaldoz/graphyn-editor")
            connection.set("scm:git:git://github.com/ronjunevaldoz/graphyn-editor.git")
            developerConnection.set("scm:git:ssh://git@github.com/ronjunevaldoz/graphyn-editor.git")
        }
    }
}
