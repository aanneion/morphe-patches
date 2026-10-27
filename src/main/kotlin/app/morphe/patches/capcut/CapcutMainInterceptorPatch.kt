package app.morphe.patches.capcut

import app.morphe.patches.shared.Constants
import app.morphe.patcher.Fingerprint
import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.patch.bytecodePatch
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.builder.MutableMethodImplementation
import com.android.tools.smali.dexlib2.immutable.ImmutableMethod
import app.morphe.patcher.util.proxy.mutableTypes.MutableMethod.Companion.toMutable

private object MainCrackingInterceptorFingerprint : Fingerprint(
    definingClass = "Lcom/vega/launcher/network/interceptors/CrackingInterceptor;",
    name = "intercept",
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.FINAL, AccessFlags.NATIVE),
    returnType = "Lcom/bytedance/retrofit2/SsResponse;",
    parameters = listOf("Lcom/bytedance/retrofit2/intercept/Interceptor\$Chain;"),
)

/**
 * Experimental; statically targeted against 19.9.0 / 19900002 only.
 * Native registration during class initialization may reject this replacement.
 * Requires physical-device testing; does not override server responses.
 */
@Suppress("unused")
val capcutMainInterceptorPatch = bytecodePatch(
    name = "Experimental main security interceptor passthrough",
    description = "Replaces the native main-app cracking interceptor with a transparent request/response passthrough. Experimental: may conflict with native registration; does not unlock server-side content.",
    default = false,
) {
    compatibleWith(Constants.COMPATIBILITY_CAPCUT)
    dependsOn(capcutExactBuildGuard)

    execute {
        val method = MainCrackingInterceptorFingerprint.method
        check(method.implementation == null) {
            "Expected the native interceptor from the inspected 19900002 APK; refusing to modify a different implementation."
        }
        check(method.accessFlags and AccessFlags.NATIVE.value != 0) {
            "Expected a native method."
        }
        check(method.accessFlags and (AccessFlags.STATIC.value or AccessFlags.ABSTRACT.value) == 0) {
            "Expected a concrete instance native method; refusing this APK."
        }
        // v0: request then response; p0=v1: this; p1=v2: chain.
        val replacement = ImmutableMethod(
            method.definingClass, method.name, method.parameters, method.returnType,
            method.accessFlags and AccessFlags.NATIVE.value.inv(),
            method.annotations, method.hiddenApiRestrictions, MutableMethodImplementation(3),
        ).toMutable()
        replacement.addInstructions(
            0,
            """
                invoke-interface {p1}, Lcom/bytedance/retrofit2/intercept/Interceptor${'$'}Chain;->request()Lcom/bytedance/retrofit2/client/Request;
                move-result-object v0
                invoke-interface {p1, v0}, Lcom/bytedance/retrofit2/intercept/Interceptor${'$'}Chain;->proceed(Lcom/bytedance/retrofit2/client/Request;)Lcom/bytedance/retrofit2/SsResponse;
                move-result-object v0
                return-object v0
            """,
        )
        val owner = mutableClassDefBy(method.definingClass)
        check(owner.methods.remove(method)) { "Original interceptor missing from mutable class." }
        owner.methods.add(replacement)
        println("[Experimental main interceptor] Replaced one native method. Device validation REQUIRED; server responses remain unchanged.")
    }
}
