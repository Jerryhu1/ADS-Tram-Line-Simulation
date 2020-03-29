package simulation.events

import simulation.interfaces.Event
import simulation.logic.models.TramStop

data class TramSwitchAvailableEvent(override var time: Int, val tramStop: TramStop) : Event