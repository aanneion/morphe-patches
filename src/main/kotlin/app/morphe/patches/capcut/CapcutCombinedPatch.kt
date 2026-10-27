package app.morphe.patches.capcut

import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patches.shared.Constants

@Suppress("unused")
val capcutCombinedPatch = bytecodePatch(
    name = "CapCut Combined - core",
    description = "Combines the ten functional/privacy/region patches. Preserves speech libraries and all languages. Server-side features and device stability are not guaranteed.",
    default = true,
) {
    compatibleWith(Constants.COMPATIBILITY_CAPCUT)
    dependsOn(
        capcutExactBuildGuard,
        capcutBlockTelemetryPatch,
        capcutBypassEffectsPatch,
        capcutBypassGeoBlockPatch,
        capcutBypassSignaturePatch,
        capcutBypassUnsafeModalPatch,
        capcutRedirectApiBootPatch,
        capcutRemoveWatermarkPatch,
        capcutUnlockExportPatch,
        capcutUnlockPremiumPatch,
        capcutUnlockPremiumTemplatesPatch,
    )
    execute { println("[CapCut Combined core] Dependency patches applied; runtime validation still required.") }
}

@Suppress("unused")
val capcutCombinedNativePatch = bytecodePatch(
    name = "CapCut Combined - experimental native fix",
    description = "Core combination plus native interceptor replacement. Unverified native registration risk: may crash. Does not fabricate successful server responses. Test only after backing up drafts.",
    default = false,
) {
    compatibleWith(Constants.COMPATIBILITY_CAPCUT)
    dependsOn(capcutCombinedPatch, capcutMainInterceptorPatch)
    execute { println("[CapCut Combined native] Experimental native candidate included; test on device before release.") }
}
