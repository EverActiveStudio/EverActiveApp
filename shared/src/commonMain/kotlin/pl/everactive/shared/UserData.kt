package pl.everactive.shared

import kotlinx.serialization.Serializable

@Serializable
data class UserDataResponse(val users: List<UserDataDto>) : ApiPayload

@Serializable
data class UserDataDto(
    val name: String,
    val email: String,
    val role: String,

    val state: State,
    val ruleStatus: List<RuleStatus>,
    val group: Group?,
) {
    @Serializable
    data class Group(val name: String)

    @Serializable
    data class State(
        val lastLocation: Location?,
        // epoch milliseconds
        val lastMoveTime: Long?,
        val lastEventTime: Long?,
        val isSos: Boolean,
        val fellRecently: Boolean,
    ) {
        @Serializable
        data class Location(val latitude: Double, val longitude: Double)
    }

    @Serializable
    data class RuleStatus(
        val rule: Rule,
        val timestamp: Long,
        val isViolated: Boolean,
    )
}
