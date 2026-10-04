package app.khom.pavlo.crypto.model.backup

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class BackupTradesCodecTest {

    private val codec = BackupJsonCodec()

    private fun transaction(
        id: Long = 1L,
        type: String = "SELL",
        fee: String = "2.5"
    ) = TransactionBackup(
        id = id,
        from = "BTC",
        to = "USD",
        quantity = "0.5",
        purchasePrice = "90000",
        dateEpochMillis = 1_700_000_000_000L,
        type = type,
        fee = fee
    )

    @Test
    fun `trade type and fee round trip through the backup file`() {
        val document = AppBackupDocument(
            transactions = listOf(transaction(1L), transaction(2L, type = "TRANSFER_IN", fee = "0"))
        )

        val decoded = codec.decode(codec.encode(document))

        assertEquals(document, decoded)
        assertEquals("SELL", decoded.transactions.first().type)
        assertEquals("2.5", decoded.transactions.first().fee)
    }

    @Test
    fun `older backups without type or fee read as free purchases`() {
        val legacy = """
            {"format":"$BACKUP_FORMAT","schemaVersion":1,"favorites":[],"preferences":[],
             "transactions":[{"id":7,"from":"ETH","to":"USD","quantity":"2","purchasePrice":"1500","dateEpochMillis":5}]}
        """.trimIndent()

        val decoded = codec.decode(legacy)

        assertEquals("BUY", decoded.transactions.single().type)
        assertEquals("0", decoded.transactions.single().fee)
    }

    @Test
    fun `invalid trade types and fees are rejected`() {
        listOf(
            AppBackupDocument(transactions = listOf(transaction(type = "SWAP"))),
            AppBackupDocument(transactions = listOf(transaction(fee = "-1"))),
            AppBackupDocument(transactions = listOf(transaction(fee = "abc")))
        ).forEach { document ->
            assertThrows(InvalidBackupException::class.java) { codec.encode(document) }
        }
    }
}
