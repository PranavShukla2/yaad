package com.pranavshukla.yaad.data

import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi

/**
 * A backup file sealed with the user's password: PBKDF2-HMAC-SHA256 (600,000 rounds, random salt) gives an AES-256
 * key and an HMAC-SHA256 key; the backup is encrypted with AES-256-CBC, then the salt, IV and ciphertext are
 * authenticated (encrypt-then-MAC). A wrong password or a changed byte is rejected before anything is decrypted.
 */
@OptIn(ExperimentalEncodingApi::class)
object EncryptedBackup {
    private const val Format = "yaad-backup-encrypted"
    private const val Version = 1
    private const val Rounds = 600_000
    private val json = Json { encodeDefaults = true }

    @Serializable
    private data class Sealed(
        val format: String = Format,
        val version: Int = Version,
        val iterations: Int,
        val salt: String,
        val iv: String,
        val mac: String,
        val data: String,
    )

    fun seal(backup: String, password: String): String = seal(backup, password, secureRandom(16), secureRandom(16), Rounds)

    internal fun seal(backup: String, password: String, salt: ByteArray, iv: ByteArray, iterations: Int): String {
        val (encryptKey, macKey) = keys(password, salt, iterations)
        val data = aesCbcEncrypt(encryptKey, iv, backup.encodeToByteArray())
        val mac = hmacSha256(macKey, salt + iv + data)
        return json.encodeToString(Sealed.serializer(), Sealed(
            iterations = iterations, salt = Base64.encode(salt), iv = Base64.encode(iv), mac = Base64.encode(mac), data = Base64.encode(data),
        ))
    }

    /** @throws BackupException if [text] is not a sealed backup, or the password is wrong or the file was changed. */
    fun open(text: String, password: String): String {
        val sealed = try {
            json.decodeFromString(Sealed.serializer(), text)
        } catch (e: SerializationException) {
            throw BackupException("Not an encrypted Yaad backup", e)
        } catch (e: IllegalArgumentException) {
            throw BackupException("Not an encrypted Yaad backup", e)
        }
        if (sealed.format != Format || sealed.version > Version || sealed.iterations !in 1..10_000_000) {
            throw BackupException("Not an encrypted Yaad backup")
        }
        val (salt, iv, mac, data) = try {
            listOf(sealed.salt, sealed.iv, sealed.mac, sealed.data).map { Base64.decode(it) }
        } catch (e: IllegalArgumentException) {
            throw BackupException("The backup file is damaged", e)
        }
        val (encryptKey, macKey) = keys(password, salt, sealed.iterations)
        if (!constantTimeEquals(hmacSha256(macKey, salt + iv + data), mac)) throw BackupException("Wrong password, or the file was changed")
        return try {
            aesCbcDecrypt(encryptKey, iv, data).decodeToString()
        } catch (e: IllegalArgumentException) {
            throw BackupException("The backup file is damaged", e)
        }
    }

    private fun keys(password: String, salt: ByteArray, iterations: Int): Pair<ByteArray, ByteArray> {
        val both = pbkdf2Sha256(password, salt, iterations, 64)
        return both.copyOfRange(0, 32) to both.copyOfRange(32, 64)
    }

    private fun constantTimeEquals(a: ByteArray, b: ByteArray): Boolean {
        if (a.size != b.size) return false
        var diff = 0
        for (i in a.indices) diff = diff or (a[i].toInt() xor b[i].toInt())
        return diff == 0
    }
}
