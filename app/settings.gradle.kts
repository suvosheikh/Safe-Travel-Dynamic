pluginManagement {
  repositories {
    google()
    mavenCentral()
    gradlePluginPortal()
  }
}

dependencyResolutionManagement {
  repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
  repositories {
    google()
    mavenCentral()
    maven {
      url = uri("https://api.mapbox.com/downloads/v2/releases/maven")
      credentials {
        username = "mapbox"
        val envFile = file(".env")
        val localPropsFile = file("local.properties")
        var secretToken = System.getenv("MAPBOX_DOWNLOADS_TOKEN") ?: ""
        if (secretToken.isEmpty() && envFile.exists()) {
          val props = java.util.Properties()
          envFile.inputStream().use { props.load(it) }
          secretToken = props.getProperty("MAPBOX_DOWNLOADS_TOKEN") ?: ""
        }
        if (secretToken.isEmpty() && localPropsFile.exists()) {
          val props = java.util.Properties()
          localPropsFile.inputStream().use { props.load(it) }
          secretToken = props.getProperty("MAPBOX_DOWNLOADS_TOKEN") ?: ""
        }
        password = secretToken
      }
    }
  }
}

rootProject.name = "My Application"

include(":app")
