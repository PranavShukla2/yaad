package com.pranavshukla.yaad.data

/** The few standard primitives backups need, from each platform's own crypto (javax.crypto, CommonCrypto). */
internal expect fun pbkdf2Sha256(password: String, salt: ByteArray, iterations: Int, length: Int): ByteArray

internal expect fun aesCbcEncrypt(key: ByteArray, iv: ByteArray, data: ByteArray): ByteArray

/** @throws IllegalArgumentException if [data] does not decrypt (bad padding). */
internal expect fun aesCbcDecrypt(key: ByteArray, iv: ByteArray, data: ByteArray): ByteArray

internal expect fun hmacSha256(key: ByteArray, data: ByteArray): ByteArray

internal expect fun secureRandom(size: Int): ByteArray
