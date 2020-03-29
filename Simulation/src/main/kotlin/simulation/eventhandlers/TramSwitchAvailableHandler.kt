package simulation.eventhandlers

import simulation.events.TramSwitchAvailableEvent
import simulation.interfaces.Event
import simulation.interfaces.EventHandler
import simulation.logging.LoggingData
import simulation.logic.StateLogic

/**
 * Event handler when a tram leaves the switch
 * The switch should become available, or the next tram will occupy it
 */
object TramSwitchAvailableHandler : EventHandler {
    override fun handleEvent(event: Event, logic: StateLogic) {
        // Cast this Event to a TramSwitchAvailableEvent
        event as TramSwitchAvailableEvent

        // This switch is no longer busy
        event.tramStop.switchBusy = false

        // If the Tram queue is not empty, schedule the arrival or departure of the first Tram in the queue at this time
        if (event.tramStop.tramQueue.isNotEmpty()) {
            val queuedEvent = event.tramStop.tramQueue.pollFirst()

            // Update the logging on how long trams have to queue
            LoggingData.tramQueueTimes[event.tramStop]!!.add(event.time - queuedEvent.time)

            // Reschedule the Event
            queuedEvent.time = event.time
            logic.addEvent(queuedEvent)
        }
    }
}