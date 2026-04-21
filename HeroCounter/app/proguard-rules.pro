# Hero Counter - ProGuard Rules
# Keep Room entities
-keep class com.herocounter.app.Task { *; }
-keep class com.herocounter.app.CountEntry { *; }
-keep class com.herocounter.app.PeriodTotal { *; }

# Keep MPAndroidChart
-keep class com.github.mikephil.charting.** { *; }
