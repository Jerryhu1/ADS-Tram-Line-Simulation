package simulation.eventhandlers

import simulation.events.AddTramEvent
import simulation.events.TramEndpointArrivalEvent
import simulation.interfaces.Event
import simulation.interfaces.EventHandler
import simulation.logic.StateLogic

object AddTramEventHandler : EventHandler {

    override fun handleEvent(event: Event, logic: StateLogic) {
        // Cast this Event to a AddTramEvent
        event as AddTramEvent

        // Add the new Tram to the list of all Trams
        logic.addTram(event.tram)

        // Schedule the TramEndpointArrivalEvent at P+R
        logic.addEvent(TramEndpointArrivalEvent(
                time = event.time,
                tramStop = logic.state.tramStops[0],
                tram = event.tram,
                isStarting = true
        ))
        //println("${Timer.convertSimulationTime(event.time+21600)} AddTramEvent. ${event.tram} added")

//        println("Tram added at P+R at ${event.time}")
    }
}