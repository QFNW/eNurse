# eNurse ProGuard Rules
-keepattributes Signature
-keepattributes *Annotation*

# Retrofit
-dontwarn retrofit2.**
-keep class retrofit2.** { *; }

# Gson
-keep class com.enurse.app.data.remote.dto.** { *; }
-keep class com.enurse.app.data.local.datastore.SavedLogin { *; }

# Room
-keep class * extends androidx.room.RoomDatabase
-dontwarn androidx.room.paging.**
