package pl.everactive.backend.config

import kotlinx.serialization.json.Json
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.converter.json.KotlinSerializationJsonHttpMessageConverter
import pl.everactive.shared.serialization.ApiPayloadSerializersModule

@Configuration
class SerializationConfig {
    @Bean
    fun kotlinxJson(): Json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
        serializersModule = ApiPayloadSerializersModule
    }

    @Bean
    fun kotlinSerializationJsonHttpMessageConverter(json: Json): KotlinSerializationJsonHttpMessageConverter =
        KotlinSerializationJsonHttpMessageConverter(json)
}
