package util

import app.morphe.patcher.apk.ApkUtils
import java.io.File
import java.security.MessageDigest

fun main(args: Array<String>) {
    require(args.size == 3) { "unsigned.apk signed.apk test-keystore.bks" }
    ApkUtils.signApk(
        File(args[0]), File(args[1]), "CapCut experimental test",
        ApkUtils.KeyStoreDetails(keyStore = File(args[2]), alias = "morphe", password = "morphepassword"),
    )
    for (api in listOf(23, 28, 36)) {
        val certificates = ApkUtils.verifiedSigningCertificates(File(args[1]), api)
        check(!certificates.isNullOrEmpty()) { "APK signature verification failed for API $api" }
        println("PASS signature verification API $api; certificates=${certificates.size}")
        certificates.forEach { cert -> println("Certificate SHA-256: " + MessageDigest.getInstance("SHA-256").digest(cert.encoded).joinToString("") { "%02x".format(it) }) }
    }
}
