package velocorner.weather.util

import java.nio.file.Files
import java.nio.file.Paths

object DockerUtil {

    fun configureDockerSocketIfNeeded() {
        System.setProperty("DOCKER_HOST", detectDockerSocket())
        // Verify Docker connectivity
        // DockerClientFactory.instance().client().pingCmd().exec()
    }

    /**
     * Detects the Docker socket and returns its path in the format "unix://<path>".
     *
     * @return the Docker socket path
     * @throws IllegalStateException if no Docker socket is found
     */
    fun detectDockerSocket(): String {
        val possibleSockets = listOf(
            Paths.get("${System.getProperty("user.home")}/.colima/docker.sock"), // Colima socket
            Paths.get("/var/run/docker.sock") // Regular Docker socket
        )

        val dockerSocket = possibleSockets.find { Files.exists(it) }

        return when (dockerSocket) {
            null -> throw IllegalStateException("No Docker socket found. Ensure Docker or Colima is running.")
            else -> {
                println("Using Docker socket: $dockerSocket")
                "unix://$dockerSocket"
            }
        }
    }
}