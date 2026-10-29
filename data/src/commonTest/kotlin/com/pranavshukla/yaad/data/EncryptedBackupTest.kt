package com.pranavshukla.yaad.data

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class EncryptedBackupTest {
    private val backup = """{"format":"yaad-backup","version":3,"reminders":[{"title":"Rent ₹15,000"}]}"""

    @Test
    fun aSealedBackupOpensWithItsPassword() {
        val sealed = EncryptedBackup.seal(backup, "correct horse")
        assertTrue("Rent" !in sealed, "nothing readable in the file")
        assertEquals(backup, EncryptedBackup.open(sealed, "correct horse"))
    }

    @Test
    fun aWrongPasswordOrAChangedFileIsRejected() {
        val sealed = EncryptedBackup.seal(backup, "correct horse")
        assertFailsWith<BackupException> { EncryptedBackup.open(sealed, "wrong horse") }
        val data = Regex(""""data":"([^"]+)"""").find(sealed)!!.groupValues[1]
        val flipped = (if (data[5] == 'A') 'B' else 'A') + ""
        assertFailsWith<BackupException> { EncryptedBackup.open(sealed.replace(data, data.substring(0, 5) + flipped + data.substring(6)), "correct horse") }
        assertFailsWith<BackupException> { EncryptedBackup.open("not a backup", "correct horse") }
        assertFailsWith<BackupException> { EncryptedBackup.open(backup, "correct horse") }
    }

    @Test
    fun everyPlatformWritesTheSameFile() {
        // The same inputs give the same file on Android, iOS and desktop, so a backup moves between them.
        val sealed = EncryptedBackup.seal("Yaad", "pw", ByteArray(16) { 1 }, ByteArray(16) { 2 }, 1000)
        assertEquals(FINGERPRINT, sealed)
    }

    private companion object {
        /** Checked independently with Python (hashlib, hmac) and OpenSSL (aes-256-cbc). */
        const val FINGERPRINT = "{\"format\":\"yaad-backup-encrypted\",\"version\":1,\"iterations\":1000," +
            "\"salt\":\"AQEBAQEBAQEBAQEBAQEBAQ==\",\"iv\":\"AgICAgICAgICAgICAgICAg==\"," +
            "\"mac\":\"yJgW709Ulr+ky5qPVJJVRLIU6Pme+SZTtVqEDJKX4Kw=\",\"data\":\"6lQQqFRQEljRiVe/58qcCw==\"}"
    }
}
