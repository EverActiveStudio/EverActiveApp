package pl.everactive.shared.serialization

import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.polymorphic
import kotlinx.serialization.modules.subclass
import pl.everactive.shared.ApiPayload
import pl.everactive.shared.LoginResponse
import pl.everactive.shared.PushEventsResponse
import pl.everactive.shared.UserDataResponse

val ApiPayloadSerializersModule: SerializersModule = SerializersModule {
    polymorphic(ApiPayload::class) {
        subclass(PushEventsResponse.serializer())
        subclass(LoginResponse.serializer())
        subclass(UserDataResponse.serializer())
    }
}
