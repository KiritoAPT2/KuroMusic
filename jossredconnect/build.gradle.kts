plugins {
    id("org.jetbrains.kotlin.jvm")
}

kotlin {
    jvmToolchain(17)
}

dependencies {

    // OkHttp (to JossRedClient)
    implementation(libs.okhttp) // o la versión más reciente

    // If you also need interceptors for logging (optional)
    implementation(libs.logging.interceptor)

}
