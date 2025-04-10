package velocorner.weather.util

import java.nio.file.Files
import java.nio.file.Paths

object DockerUtil {

    fun configureDockerSocketIfNeeded() {
        val colimaSocket = Paths.get("${System.getProperty("user.home")}/.colima/docker.sock")
        val dockerSocket = Paths.get("/var/run/docker.sock")

        if (Files.exists(colimaSocket)) {
            System.setProperty("DOCKER_HOST", "unix://$colimaSocket")
            println("Colima detected. Using Docker socket: $colimaSocket")
        } else if (Files.exists(dockerSocket)) {
            System.setProperty("DOCKER_HOST", "unix://$dockerSocket")
            println("Regular Docker detected. Using Docker socket: $dockerSocket")
        } else {
            throw IllegalStateException("No Docker socket found. Ensure Docker or Colima is running.")
        }

        // Verify Docker connectivity
        // DockerClientFactory.instance().client().pingCmd().exec()
    }
}