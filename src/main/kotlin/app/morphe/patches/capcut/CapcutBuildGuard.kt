package app.morphe.patches.capcut

import app.morphe.patcher.patch.bytecodePatch

internal val capcutExactBuildGuard = bytecodePatch {
    execute {
        check(packageMetadata.packageName == "com.lemon.lvoverseas" &&
            packageMetadata.versionName == "19.9.0" && packageMetadata.versionCode == "19900002") {
            "This experimental bundle requires com.lemon.lvoverseas 19.9.0 / 19900002."
        }
        println("[CapCut build guard] Exact package/version/versionCode accepted.")
    }
}

