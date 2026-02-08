package pl.kvgx12.toon

import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.shouldBe
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import java.io.File

@Serializable
data class SpecFixture(
    val version: String,
    val category: String,
    val description: String,
    val tests: List<SpecTest>
) {
    @Serializable
    data class SpecTest(
        val name: String,
        val input: JsonElement,
        val expected: String,
        val specSection: String,
        val options: Options = Options(),
    )

    @Serializable
    data class Options(
        val delimiter: String = ",",
        val keyFolding: String = "off",  // "off", "safe
        val flattenDepth: Int = Int.MAX_VALUE,
        val indent: Int = 2,
    )
}

class SpecTest : FreeSpec({
    val json = Json { ignoreUnknownKeys = true }
    val resource = SpecTest::class.java.getResource("/toon/tests/fixtures/encode")
    val fixturesDir = File(resource.toURI())

    fixturesDir.walk()
        .filter { it.isFile && it.extension == "json" }
        .sortedBy { it.name }
        .forEach { file ->
            val fixture = json.decodeFromString<SpecFixture>(file.readText())

            "${fixture.category} - ${file.nameWithoutExtension}" - {
                fixture.tests.forEach { testCase ->
                    testCase.name {
                        withClue("Input: ${testCase.input}, Section: ${testCase.specSection}") {
                            val toon = Toon(
                                delimiter = testCase.options.delimiter.first(),
                                keyFolding = when (testCase.options.keyFolding.lowercase()) {
                                    "off" -> Toon.KeyFolding.Off
                                    "safe" -> Toon.KeyFolding.Safe
                                    else -> throw IllegalArgumentException("Invalid key folding option: ${testCase.options.keyFolding}")
                                },
                                flattenDepth = testCase.options.flattenDepth,
                                indent = testCase.options.indent
                            )
                            val result = toon.encodeToToonString(testCase.input)
                            result shouldBe testCase.expected
                        }
                    }
                }
            }
        }
})
