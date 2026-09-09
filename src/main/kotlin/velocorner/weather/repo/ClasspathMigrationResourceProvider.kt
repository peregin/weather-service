package velocorner.weather.repo

import org.flywaydb.core.api.FlywayException
import org.flywaydb.core.api.ResourceProvider
import org.flywaydb.core.api.resource.LoadableResource
import java.io.InputStreamReader
import java.io.Reader
import java.nio.charset.StandardCharsets

private const val MIGRATION_INDEX = "migration/migrations.txt"

/**
 * Supplies the known SQL migrations without scanning classpath URLs. GraalVM
 * exposes embedded resources with the `resource:` protocol, which Flyway does
 * not currently scan.
 */
internal class ClasspathMigrationResourceProvider(
    migrationLocation: String,
    private val classLoader: ClassLoader = Thread.currentThread().contextClassLoader
) : ResourceProvider {

    private val rootPath = migrationLocation.removePrefix("classpath:").trim('/')

    private val resources: List<LoadableResource> by lazy {
        val paths = classLoader.getResourceAsStream(MIGRATION_INDEX)?.bufferedReader()?.use { reader ->
            reader.lineSequence()
                .map(String::trim)
                .filter(String::isNotEmpty)
                .filter { it.startsWith("$rootPath/") }
                .toList()
        } ?: throw FlywayException("Migration index $MIGRATION_INDEX was not found")

        paths.map { path -> ClasspathMigrationResource(path, rootPath, classLoader) }
    }

    override fun getResource(name: String): LoadableResource? =
        resources.firstOrNull { resource ->
            resource.absolutePath == name ||
                resource.relativePath == name ||
                resource.filename == name
        }

    override fun getResources(prefix: String, suffixes: Array<out String>): Collection<LoadableResource> =
        resources.filter { resource ->
            resource.filename.startsWith(prefix) && suffixes.any(resource.filename::endsWith)
        }
}

private class ClasspathMigrationResource(
    private val path: String,
    private val rootPath: String,
    private val classLoader: ClassLoader
) : LoadableResource() {

    override fun getAbsolutePath(): String = path

    override fun getAbsolutePathOnDisk(): String = path

    override fun getFilename(): String = path.substringAfterLast('/')

    override fun getRelativePath(): String = path.removePrefix("$rootPath/")

    override fun read(): Reader {
        val stream = classLoader.getResourceAsStream(path)
            ?: throw FlywayException("Migration resource $path was not found")
        return InputStreamReader(stream, StandardCharsets.UTF_8)
    }
}
