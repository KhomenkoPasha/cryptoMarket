package app.khom.pavlo.crypto.model.backup

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class BackupPortfoliosCodecTest {

    private val codec = BackupJsonCodec()

    private fun transaction(portfolioId: Long, id: Long = portfolioId) = TransactionBackup(
        id = id,
        from = "BTC",
        to = "USD",
        quantity = "1",
        purchasePrice = "100",
        dateEpochMillis = 1L,
        portfolioId = portfolioId
    )

    @Test
    fun `portfolios and the portfolio of each transaction round trip`() {
        val document = AppBackupDocument(
            portfolios = listOf(
                PortfolioBackup(id = 1L, name = "", createdAtEpochMillis = 10L, sortOrder = 0),
                PortfolioBackup(id = 2L, name = "Cold wallet", createdAtEpochMillis = 20L, sortOrder = 1)
            ),
            transactions = listOf(transaction(1L), transaction(2L))
        )

        val decoded = codec.decode(codec.encode(document))

        assertEquals(document, decoded)
        assertEquals(BACKUP_SCHEMA_VERSION, decoded.schemaVersion)
    }

    @Test
    fun `older backups without portfolios put everything in the default one`() {
        val legacy = """
            {"format":"$BACKUP_FORMAT","schemaVersion":2,"favorites":[],"preferences":[],
             "transactions":[{"id":3,"from":"ETH","to":"USD","quantity":"2","purchasePrice":"1500","dateEpochMillis":5}]}
        """.trimIndent()

        val decoded = codec.decode(legacy)

        assertEquals(1L, decoded.transactions.single().portfolioId)
        assertTrue(decoded.portfolios.isEmpty())
        assertTrue(decoded.schemaVersion < BACKUP_PORTFOLIOS_SINCE_VERSION)
    }

    @Test
    fun `transactions pointing at an unknown portfolio are rejected`() {
        val document = AppBackupDocument(
            portfolios = listOf(PortfolioBackup(id = 1L)),
            transactions = listOf(transaction(portfolioId = 9L, id = 1L))
        )

        assertThrows(InvalidBackupException::class.java) { codec.encode(document) }
    }

    @Test
    fun `invalid portfolios are rejected`() {
        listOf(
            AppBackupDocument(portfolios = listOf(PortfolioBackup(id = 0L))),
            AppBackupDocument(portfolios = listOf(PortfolioBackup(id = 1L), PortfolioBackup(id = 1L))),
            AppBackupDocument(portfolios = listOf(PortfolioBackup(id = 1L, name = "x".repeat(100)))),
            AppBackupDocument(portfolios = listOf(PortfolioBackup(id = 1L, createdAtEpochMillis = -5L)))
        ).forEach { document ->
            assertThrows(InvalidBackupException::class.java) { codec.encode(document) }
        }
    }
}
