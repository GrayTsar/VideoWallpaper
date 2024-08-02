plugins {
    id("com.android.application") version "8.5.1" apply false
    id("com.android.library") version "8.5.1" apply false
    id("org.jetbrains.kotlin.android") version "2.0.0" apply false
    id("com.google.devtools.ksp") version "2.0.0-1.0.22"
    id("com.google.gms.google-services") version "4.4.1" apply false
    id("com.google.firebase.crashlytics") version "3.0.2" apply false
    id("com.google.dagger.hilt.android") version "2.51.1" apply false
}

tasks.register("clean", Delete::class) {
    delete(rootProject.buildDir)
}