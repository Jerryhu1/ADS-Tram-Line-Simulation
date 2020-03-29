package simulation.eventhandlers

import simulation.events.TramEndpointArrivalEvent
import simulation.events.TramStopAvailableEvent
import simulation.interfaces.Event
import simulation.interfaces.EventHandler
import simulation.logging.LoggingData
import simulation.logic.StateLogic

/**
 * Event handler for the TramStopAvaiableEvent
 */
object TramStopAvailableHandler : EventHandler {
    override fun handleEvent(event: Event, logic: StateLogic) {
        // Cast this Event to a TramStopAvailableEvent
        event as TramStopAvailableEvent

        // This TramStop is no longer busy, this only affects non-endpoints
        event.tramStop.isBusy = false

        // If this is CS or P+R, free the used lane
        if (event.lane != -1) {
            event.tramStop.freeLanes.add(event.lane)
        }

        // If the Tram queue is not empty, schedule the arrival of the first Tram in the queue at this time
        if (event.tramStop.tramQueue.isNotEmpty()) {
            val queuedEvent = event.tramStop.tramQueue.poll()

            // Update the logging on how long trams have to queue
            LoggingData.tramQueueTimes[event.tramStop]!!.add(event.time - queuedEvent.time)

            // Reschedule the Event
            queuedEvent.time = event.time
            logic.addEvent(queuedEvent)
        }
    }
}