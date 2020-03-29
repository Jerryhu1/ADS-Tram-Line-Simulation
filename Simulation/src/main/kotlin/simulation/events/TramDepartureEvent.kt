package simulation.events

import simulation.interfaces.Event
import simulation.logic.models.Tram
import simulation.logic.models.TramStop

data class TramDepartureEvent(override var time: Int, val tramStop: TramStop, val tram: Tram, val lane: Int = -1, val scheduledTime: Int = 0) : Event