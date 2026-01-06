package pl.everactive.backend

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.context.properties.ConfigurationPropertiesScan
import org.springframework.boot.runApplication

@SpringBootApplication(proxyBeanMethods = false)
@ConfigurationPropertiesScan("pl.everactive.backend.config")
class BackendApplication

fun main(args: Array<String>) {
    runApplication<BackendApplication>(*args)
}
