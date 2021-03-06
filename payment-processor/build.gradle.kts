import org.ostelco.prime.gradle.Version

plugins {
  kotlin("jvm")
  `java-library`
  idea
}

dependencies {
  implementation(project(":prime-modules"))
  implementation(project(":data-store"))
  implementation(project(":publisher-extensions"))

  implementation("com.stripe:stripe-java:${Version.stripe}")

  implementation("com.google.cloud:google-cloud-pubsub:${Version.googleCloudPubSub}")
  implementation("com.google.cloud:google-cloud-datastore:${Version.googleCloudDataStore}")

  testImplementation(kotlin("test"))
  testImplementation(kotlin("test-junit"))
}

sourceSets.create("integration") {
  java.srcDirs("src/integration-test/kotlin")
  resources.srcDirs("src/integration-test/resources")
  compileClasspath += sourceSets.main.get().output + sourceSets.test.get().output
  runtimeClasspath += sourceSets.main.get().output + sourceSets.test.get().output
}

val integration = tasks.create("integration", Test::class.java) {
  description = "Runs the integration tests."
  group = "Verification"
  testClassesDirs = sourceSets.getByName("integration").output.classesDirs
  classpath = sourceSets.getByName("integration").runtimeClasspath
}

configurations.named("integrationImplementation") {
  extendsFrom(configurations["implementation"])
  extendsFrom(configurations["runtime"])
  extendsFrom(configurations["runtimeOnly"])
  extendsFrom(configurations["testImplementation"])
}

tasks.build.get().dependsOn(integration)

apply(from = "../gradle/jacoco.gradle.kts")

idea {
  module {
    testSourceDirs = testSourceDirs + file("src/integration-test/kotlin")
  }
}