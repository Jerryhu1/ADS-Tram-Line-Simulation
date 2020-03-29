package simulation.events

import simulation.interfaces.Event
import simulation.interfaces.TramArrivalInterface
import simulation.logic.models.Tram
import simulation.logic.models.TramStop

data class TramEndpointArrivalEvent(override var time: Int, override val tramStop: TramStop, override val tram: Tram, val isStarting: Boolean = false,
                                     var causedBy: Event? = null) : TramArrivalInterface{
}