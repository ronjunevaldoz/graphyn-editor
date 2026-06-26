
plugins {
    id("com.vanniktech.maven.publish")
}

mavenPublishing {
    publishToMavenCentral(automaticRelease = true)
    if (project.hasProperty("signing.keyId") || project.hasProperty("signingKey")) signAllPublications()

    pom {
        url = "https://github.com/ronjunevaldoz/graphyn-editor"
        licenses {
            license {
                name = "MIT License"
                url = "https://opensource.org/licenses/MIT"
                distribution = "repo"
            }
        }
        developers {
            developer {
                id = "ronjunevaldoz"
                name = "Ron June Valdoz"
                email = "ronjune.lopez@gmail.com"
            }
        }
        scm {
            connection = "scm:git:git://github.com/ronjunevaldoz/graphyn-editor.git"
            developerConnection = "scm:git:ssh://github.com/ronjunevaldoz/graphyn-editor.git"
            url = "https://github.com/ronjunevaldoz/graphyn-editor"
        }
    }
}
