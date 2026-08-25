# Room
-keep class com.kasirpintar.kaspintest.data.local.entity.** { *; }

# Retrofit / Gson models used via reflection
-keep class com.kasirpintar.kaspintest.data.remote.** { *; }
-keepattributes Signature
-keepattributes *Annotation*

# WorkManager workers instantiated via reflection
-keep class com.kasirpintar.kaspintest.sync.** { *; }
