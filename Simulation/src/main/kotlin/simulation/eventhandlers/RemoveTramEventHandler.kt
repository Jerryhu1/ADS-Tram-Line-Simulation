package simulation.eventhandlers

import simulation.events.RemoveTramEvent
import simulation.interfaces.Event
import simulation.interfaces.EventHandler
import simulation.logic.StateLogic

object RemoveTramEventHandler: EventHandler {
    override fun handleEvent(event: Event, logic: StateLogic) {
        // Cast this Event to a AddTramEvent
        event as RemoveTramEvent

        // Tell P+R that a Tram should be removed
        logic.state.tramStops[0].tramsToRemove++
        //println("${Timer.convertSimulationTime(event.time+21600)} RemoveTramEvent. Trams still to be removed by the TramEndpointArrivalHandler: ${logic.state.tramStops[0].tramsToRemove}")
    }
}