package com.pranavshukla.yaad.data

import kotlin.test.Test
import kotlin.test.assertEquals

/** Known answers from Python's hashlib and OpenSSL, so every platform reads the others' backups. */
class CryptoTest {
    private fun ByteArray.hex() = joinToString("") { (it.toInt() and 0xff).toString(16).padStart(2, '0') }

    @Test
    fun pbkdf2MatchesThePublishedVectors() {
        assertEquals("120fb6cffcf8b32c43e7225256c4f837a86548c92ccc35480805987cb70be17b", pbkdf2Sha256("password", "salt".encodeToByteArray(), 1, 32).hex())
        assertEquals("c5e478d59288c841aa530db6845c4c8d962893a001ce4e11a4963873aa98134a", pbkdf2Sha256("password", "salt".encodeToByteArray(), 4096, 32).hex())
    }

    @Test
    fun aesAndHmacMatchOpenSsl() {
        val key = ByteArray(32) { it.toByte() }
        val iv = ByteArray(16) { (it + 16).toByte() }
        val data = "Yaad backup test: ₹15,000".encodeToByteArray()
        val sealed = aesCbcEncrypt(key, iv, data)
        assertEquals("0e72b09d3f0e2f2808da6a6615ecb776e646f0185871f44e672d41d19b437c19", sealed.hex())
        assertEquals(data.toList(), aesCbcDecrypt(key, iv, sealed).toList())
        assertEquals("23696ec7a85a72053c4fb0c87c693a56d1fb5385001418a3ae8694f5bb74b1a2", hmacSha256(ByteArray(32) { (it + 32).toByte() }, "Yaad".encodeToByteArray()).hex())
    }

    @Test
    fun aWrongKeyNeverGivesBackTheSecret() {
        // A wrong key may fail on padding (Java) or give garbage with valid padding by chance (CommonCrypto): either
        // way it must not give the plaintext. Backups detect wrong passwords by their HMAC, not by padding.
        val secret = "secret".encodeToByteArray()
        val sealed = aesCbcEncrypt(ByteArray(32), ByteArray(16), secret)
        val opened = runCatching { aesCbcDecrypt(ByteArray(32) { 1 }, ByteArray(16), sealed) }.getOrNull()
        assertEquals(false, opened?.contentEquals(secret) ?: false)
    }

    @Test
    fun randomBytesDiffer() {
        assertEquals(16, secureRandom(16).size)
        assertEquals(false, secureRandom(16).contentEquals(secureRandom(16)))
    }
}
