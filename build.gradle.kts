plugins {
    id("com.android.application") version "8.1.1" apply false
    id("com.android.library") version "8.1.1" apply false
    id("org.jetbrains.kotlin.android") version "1.9.0" apply false
    id("com.google.gms.google-services") version "4.3.15" apply false
    id("com.google.firebase.crashlytics") version "2.9.8" apply false
    id("com.google.dagger.hilt.android") version "2.47" apply false
}

tasks.register("clean", Delete::class) {
    delete(rootProject.buildDir)
}