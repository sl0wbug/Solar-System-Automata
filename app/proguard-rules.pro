# -----------------------------------------------------------------------------
# Solar System Automata - ProGuard / R8 Release Hardening Rules
# -----------------------------------------------------------------------------

# Keep Kotlin reflection metadata and annotations
-keepattributes *Annotation*,Signature,InnerClasses,EnclosingMethod

# -----------------------------------------------------------------------------
# Room Persistence Library Rules
# -----------------------------------------------------------------------------
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class * { *; }
-keep @androidx.room.Dao interface * { *; }
-keepclassmembers class * extends androidx.room.RoomDatabase {
    *;
}

# Preserve Room entities and DAOs explicitly
-keep class com.droidlinkstd.solarsystemautomata.AppDatabase { *; }
-keep class com.droidlinkstd.solarsystemautomata.Planet { *; }
-keep class com.droidlinkstd.solarsystemautomata.PlanetDao { *; }
-keep class com.droidlinkstd.solarsystemautomata.data.** { *; }

-dontwarn androidx.room.paging.**

# -----------------------------------------------------------------------------
# Domain Physics & Simulation Engine Models
# -----------------------------------------------------------------------------
# Keep high-precision domain models, Structure of Arrays (SoA), and integrator structures
-keep class com.droidlinkstd.solarsystemautomata.domain.physics.** {
    <fields>;
    <methods>;
}

# -----------------------------------------------------------------------------
# Jetpack Compose Runtime & UI
# -----------------------------------------------------------------------------
-keepclassmembers class * {
    @androidx.compose.runtime.Composable *;
}
-dontwarn androidx.compose.**
