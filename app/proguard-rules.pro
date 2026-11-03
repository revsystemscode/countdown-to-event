# Project-specific R8 rules. Libraries ship their own consumer rules; these cover gaps.

# WorkManager (used by Glance widgets) instantiates these by reflection. Without the
# constructors, widgets stay on their loading spinner in release builds.
-keep class * extends androidx.work.InputMerger { public <init>(); }
-keep class * extends androidx.work.ListenableWorker {
    public <init>(android.content.Context, androidx.work.WorkerParameters);
}
