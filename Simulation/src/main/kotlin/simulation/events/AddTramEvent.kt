package simulation.events

import simulation.interfaces.Event
import simulation.logic.models.Tram

data class AddTramEvent(override var time: Int, val tram: Tram) : Event