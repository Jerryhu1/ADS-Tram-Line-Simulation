package simulation.interfaces

import simulation.logic.models.Tram
import simulation.logic.models.TramStop

interface TramArrivalInterface : Event {

    override var time: Int // The time at which this Event is scheduled to occur
    val tramStop: TramStop // The TramStop at which a Tram is arriving
    val tram: Tram         // The Tram that is arriving

}