package simulation.events

import simulation.interfaces.TramArrivalInterface
import simulation.logic.models.Tram
import simulation.logic.models.TramStop

data class TramArrivalEvent(override var time: Int, override val tramStop: TramStop, override val tram: Tram) : TramArrivalInterface