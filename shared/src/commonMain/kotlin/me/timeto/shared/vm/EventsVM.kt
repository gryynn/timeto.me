package me.timeto.shared.vm

import kotlinx.coroutines.flow.*

class EventsVM : __VM<EventsVM.State>() {

    data class State(
        val isCalendarOrList: Boolean,
    )

    override val state = MutableStateFlow(
        State(
            isCalendarOrList = true,
        )
    )

    fun setIsCalendarOrList(isCalendarOrList: Boolean) {
        state.update { it.copy(isCalendarOrList = isCalendarOrList) }
    }
}
