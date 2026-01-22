package pl.everactive.shared

import kotlinx.serialization.Serializable

@Serializable
data class UserDataResponse(val users: List<UserDataDto>)

@Serializable
data class UserDataDto(
    val state: State,
    val ruleStatus: List<RuleStatus>,
    val group: Group?,
) {
    @Serializable
    data class Group(val name: String)

    @Serializable
    data class State(
        val lastLocation: EventDto.Location?,
        val lastMoveTime: Long?,
        val lastEventTime: Long?,
        val isSos: Boolean,
        val fellRecently: Boolean,
    )

    @Serializable
    data class RuleStatus(
        val rule: Rule,
        val isViolated: Boolean,
    )
}
