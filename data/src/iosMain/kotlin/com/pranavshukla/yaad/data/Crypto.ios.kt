package com.pranavshukla.yaad.data

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.alloc
import kotlinx.cinterop.convert
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.ptr
import kotlinx.cinterop.refTo
import kotlinx.cinterop.value
import platform.CoreCrypto.CCCrypt
import platform.CoreCrypto.CCHmac
import platform.CoreCrypto.CCKeyDerivationPBKDF
import platform.CoreCrypto.kCCAlgorithmAES
import platform.CoreCrypto.kCCDecrypt
import platform.CoreCrypto.kCCEncrypt
import platform.CoreCrypto.kCCHmacAlgSHA256
import platform.CoreCrypto.kCCOptionPKCS7Padding
import platform.CoreCrypto.kCCPBKDF2
import platform.CoreCrypto.kCCPRFHmacAlgSHA256
import platform.CoreCrypto.kCCSuccess
import platform.Security.SecRandomCopyBytes
import platform.Security.kSecRandomDefault
import platform.posix.size_tVar

@OptIn(ExperimentalForeignApi::class)
internal actual fun pbkdf2Sha256(password: String, salt: ByteArray, iterations: Int, length: Int): ByteArray {
    val bytes = password.encodeToByteArray()
    val out = ByteArray(length)
    val status = CCKeyDerivationPBKDF(
        kCCPBKDF2, password, bytes.size.convert(), salt.asUByteArray().refTo(0), salt.size.convert(),
        kCCPRFHmacAlgSHA256, iterations.convert(), out.asUByteArray().refTo(0), length.convert(),
    )
    check(status == kCCSuccess) { "PBKDF2 failed: $status" }
    return out
}

@OptIn(ExperimentalForeignApi::class)
private fun aes(operation: UInt, key: ByteArray, iv: ByteArray, data: ByteArray): ByteArray = memScoped {
    val out = ByteArray(data.size + 16)
    val moved = alloc<size_tVar>()
    val status = CCCrypt(
        operation, kCCAlgorithmAES, kCCOptionPKCS7Padding, key.refTo(0), key.size.convert(), iv.refTo(0),
        if (data.isEmpty()) null else data.refTo(0), data.size.convert(), out.refTo(0), out.size.convert(), moved.ptr,
    )
    require(status == kCCSuccess) { "Does not decrypt: $status" }
    out.copyOf(moved.value.toInt())
}

internal actual fun aesCbcEncrypt(key: ByteArray, iv: ByteArray, data: ByteArray): ByteArray = aes(kCCEncrypt, key, iv, data)

internal actual fun aesCbcDecrypt(key: ByteArray, iv: ByteArray, data: ByteArray): ByteArray = aes(kCCDecrypt, key, iv, data)

@OptIn(ExperimentalForeignApi::class)
internal actual fun hmacSha256(key: ByteArray, data: ByteArray): ByteArray {
    val out = ByteArray(32)
    CCHmac(kCCHmacAlgSHA256, key.refTo(0), key.size.convert(), if (data.isEmpty()) null else data.refTo(0), data.size.convert(), out.refTo(0))
    return out
}

@OptIn(ExperimentalForeignApi::class)
internal actual fun secureRandom(size: Int): ByteArray {
    val out = ByteArray(size)
    check(SecRandomCopyBytes(kSecRandomDefault, size.convert(), out.refTo(0)) == 0) { "No randomness" }
    return out
}
