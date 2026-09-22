// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
  alias(libs.plugins.android.application) apply false
}

tasks.register("healthCheck") {
  group = "volkan"
  description = "Runs the deterministic baseline health check on the Volkan Master Shell"
  dependsOn(":app:healthCheck")
}

tasks.register("importWebApp") {
  group = "volkan"
  description = "Imports a web app ZIP (-PwebAppZip) or directory (-PwebAppDir) into assets"
  dependsOn(":app:importWebApp")
}

tasks.register("importIcon") {
  group = "volkan"
  description = "Generates multi-density launcher icons from source image (-PiconPath)"
  dependsOn(":app:importIcon")
}

tasks.register("validateWebApp") {
  group = "volkan"
  description = "Statically validates packaged web application assets"
  dependsOn(":app:validateWebApp")
}

