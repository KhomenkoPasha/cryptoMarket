package app.khom.pavlo.crypto.architecture

import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class ArchitectureBoundaryTest {

    @Test
    fun `model layer does not depend on UI or widgets`() {
        val violations = sourceRoot()
            .resolve("app/khom/pavlo/crypto/model")
            .walkTopDown()
            .filter { it.isFile && it.extension == "kt" }
            .mapNotNull { file ->
                val forbiddenImports = file.readLines()
                    .filter { line ->
                        line.startsWith("import app.khom.pavlo.crypto.ui.") ||
                            line.startsWith("import app.khom.pavlo.crypto.widget.")
                    }
                forbiddenImports.takeIf(List<String>::isNotEmpty)
                    ?.let { "${file.relativeTo(sourceRoot())}: ${it.joinToString()}" }
            }
            .toList()

        assertTrue(
            "Model layer must stay independent from Android presentation layers:\n" +
                violations.joinToString("\n"),
            violations.isEmpty()
        )
    }

    @Test
    fun `screen modules are scoped below singleton component`() {
        val violations = sourceRoot()
            .resolve("app/khom/pavlo/crypto/ui")
            .walkTopDown()
            .filter { it.isFile && it.name.endsWith("Module.kt") }
            .filter { it.readText().contains("SingletonComponent::class") }
            .map { it.relativeTo(sourceRoot()).path }
            .toList()

        assertTrue(
            "Screen modules must not retain Activity or Fragment views in SingletonComponent:\n" +
                violations.joinToString("\n"),
            violations.isEmpty()
        )
    }

    private fun sourceRoot(): File {
        val workingDirectory = File(
            System.getProperty("user.dir") ?: error("Gradle working directory is unavailable")
        )
        return sequenceOf(
            workingDirectory.resolve("src/main/java"),
            workingDirectory.resolve("app/src/main/java")
        ).firstOrNull(File::isDirectory)
            ?: error("Android source root was not found from $workingDirectory")
    }
}