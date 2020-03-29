package simulation.events

import simulation.interfaces.Event
import simulation.logic.models.TramStop

data class TramStopAvailableEvent(override var time: Int, val tramStop: TramStop, val lane: Int = -1) : Event