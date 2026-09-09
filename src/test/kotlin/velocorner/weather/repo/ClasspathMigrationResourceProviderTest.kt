package velocorner.weather.repo

import org.flywaydb.core.api.FlywayException
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

internal class ClasspathMigrationResourceProviderTest {
    @Test
    fun `generated index includes readable migrations for both databases`() {
        for (database in listOf("oracle", "psql")) {
            val provider = ClasspathMigrationResourceProvider("classpath:migration/$database")
            val resources = provider.getResources("V", arrayOf(".sql"))
            assertEquals(listOf("V1__init.sql", "V2__location.sql"), resources.map { it.filename })
            resources.forEach { resource ->
                assertTrue(resource.absolutePath.startsWith("migration/$database/"))
                assertTrue(resource.read().use { it.readText() }.isNotBlank())
                assertEquals(resource, provider.getResource(resource.absolutePath))
                assertEquals(resource, provider.getResource(resource.relativePath))
                assertEquals(resource, provider.getResource(resource.filename))
            }
        }
    }

    @Test
    fun `filters by database boundary prefix and suffix`() {
        val provider = ClasspathMigrationResourceProvider("classpath:/migration/oracle/")
        assertEquals(1, provider.getResources("V1", arrayOf(".sql")).size)
        assertTrue(provider.getResources("R", arrayOf(".sql")).isEmpty())
        assertTrue(provider.getResources("V", arrayOf(".txt")).isEmpty())
        assertTrue(provider.getResources("V", emptyArray()).isEmpty())
        assertNull(provider.getResource("migration/psql/V1__init.sql"))
        assertNull(provider.getResource("missing.sql"))
        assertTrue(ClasspathMigrationResourceProvider("classpath:migration/ora")
            .getResources("V", arrayOf(".sql")).isEmpty())
    }

    @Test
    fun `reads indexed resources as UTF-8 without requiring classpath URLs`() {
        val path = "migration/oracle/nested/V3__city.sql"
        val sql = "-- Zürich\nSELECT 1;"
        val loader = resourceLoader(mapOf("migration/migrations.txt" to "\n $path \n", path to sql))
        val provider = ClasspathMigrationResourceProvider("classpath:migration/oracle", loader)
        val resource = assertNotNull(provider.getResource("nested/V3__city.sql"))
        assertEquals(sql, resource.read().use { it.readText() })
    }

    @Test
    fun `fails explicitly if generated index is missing`() {
        val provider = ClasspathMigrationResourceProvider("classpath:migration/oracle", resourceLoader(emptyMap()))
        assertFailsWith<FlywayException> { provider.getResources("V", arrayOf(".sql")) }
    }

    @Test
    fun `fails explicitly if indexed SQL is missing`() {
        val loader = resourceLoader(mapOf("migration/migrations.txt" to "migration/oracle/V1__missing.sql"))
        val provider = ClasspathMigrationResourceProvider("classpath:migration/oracle", loader)
        val resource = provider.getResources("V", arrayOf(".sql")).single()
        assertFailsWith<FlywayException> { resource.read() }
    }

    private fun resourceLoader(resources: Map<String, String>): ClassLoader = object : ClassLoader(null) {
        override fun getResourceAsStream(name: String) = resources[name]?.byteInputStream(Charsets.UTF_8)
    }
}
