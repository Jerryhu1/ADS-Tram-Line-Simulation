package simulation.eventhandlers

import simulation.constants.Constants
import simulation.constants.DrivingSchedule
import simulation.events.TramDepartureEvent
import simulation.events.TramEndpointArrivalEvent
import simulation.events.TramStopAvailableEvent
import simulation.events.TramSwitchAvailableEvent
import simulation.interfaces.Event
import simulation.interfaces.EventHandler
import simulation.logging.LoggingData
import simulation.logic.StateLogic

object TramEndpointArrivalHandler: EventHandler {
    override fun handleEvent(event: Event, logic: StateLogic) {
        // Cast this Event to a TramArrivalEvent
        event as TramEndpointArrivalEvent
        // If the switch is busy or both lanes occupied or if another tram is scheduled earlier. We'll have to wait in front of the switch
        val nextArrival = event.tramStop.nextTramArrivalQueue.peek()
        if ((event.tramStop.switchBusy && !event.isStarting) ||
                event.tramStop.freeLanes.isEmpty() ||
                (nextArrival != null && nextArrival != event && !event.isStarting)) {

            // Only if this is a new event, we add it to the queue. Otherwise it should be the most recent one and it should be added to the first.
           if (nextArrival == event) {
                event.tramStop.tramQueue.addFirst(event)
           }
            else {
               event.tramStop.tramQueue.addLast(event)
           }

        }
        // Otherwise we can arrive at this endpoint Tramstop
        else {
            event.tram.currentTramStop = event.tramStop
            event.tramStop.tramsOnStop.add(event.tram)
//            // Remove the event from the next tram arrival queue
//            event.tramStop.nextTramArrivalQueue.poll()

            // Set the previous arrival to this event
            event.tramStop.previousTramArrival = event

            // If at least 1 lane and the switch are free the Tram will arrive at the TramStop
            val freeLane = event.tramStop.freeLanes.first()
            event.tramStop.freeLanes.remove(freeLane)

            // Set switch to busy for 60 seconds if this Tram hasn't just started service
            // Also remove this Tram from the arriving Tram events
            if (!event.isStarting) {
                event.tramStop.nextTramArrivalQueue.poll()
                setSwitchBusy(event, logic)

                // Logging
                if (event.tramStop.number == 1) {
                    LoggingData.roundTrips++
                }
            } else {
                // Logging
                LoggingData.tramsAdded++
            }

            // Check if this Tram should be removed according to the schedule
            val removeTram = (event.tramStop.tramsToRemove > 0 || (event.tramStop.number == 1 && logic.state.drivingSchedule.tramPRDepartureTimes.isEmpty()))
            if (removeTram) {
                LoggingData.tramsRemoved++
                event.tramStop.tramsToRemove--
                logic.removeTram(event.tram)
            }

            // Let all the passengers exit and enter, and save these amounts
            val passengerInfo = TramArrivalHandler.handlePassengers(logic, event, removeTram)

            // Get the dwell time of the Tram at the current TramStop, with a minimum of 0.8 * the mean dwell time
            val dwellTime = if (event.isStarting) {
                TramArrivalHandler.getDwellTime(logic, passengerInfo) // If this tram is starting, the driver won't have to switch
            } else {
                TramArrivalHandler.getDwellTime(logic, passengerInfo, Constants.endPointDwell)
            }

            // If the tram is not removed a TramDepartureEvent is scheduled
            if (!removeTram) {
                // Get the extra waiting time if we are too early
                val scheduledTime = if (event.tramStop.number == 1) {
                    if (logic.state.drivingSchedule.tramPRDepartureTimes.isNotEmpty()) {

                        logic.state.drivingSchedule.tramPRDepartureTimes[0]
                    } else {
                        System.err.println("We're out of P+R schedule!")
                        0
                    }
                } else {
                    if (logic.state.drivingSchedule.tramCSDepartureTimes.isNotEmpty()) {
                        logic.state.drivingSchedule.tramCSDepartureTimes[0]
                    } else {
                        System.err.println("We're out of CS schedule!")
                        0
                    }
                }

                var extraWaitingTime = 0
                if (!removeTram) {
                    if (scheduledTime > event.time + dwellTime) {
                        extraWaitingTime = scheduledTime - (event.time + dwellTime)
                    }
                }

                val departureTime = event.time + dwellTime + extraWaitingTime

                // Update logging
                passengerInfo.waitingTimeInfo.forEach{ passengerInfo ->
                    repeat(passengerInfo.arrivalAmount) {
                        LoggingData.passengerWaitingTimesPerTramStop[event.tramStop]!!.add(passengerInfo.avgWaitingTime + dwellTime + extraWaitingTime)
                    }
                }

                // Event: Schedule the TramDepartureEvent from the current TramStop
                scheduleDeparture(logic, event, departureTime, freeLane, scheduledTime)
            }
            // Else this Tram will leave service and a TramStopAvailableEvent is scheduled
            else {
                event.tramStop.tramsOnStop.remove(event.tram)

                // Event: The tram stop will be available again after 40 seconds
                logic.addEvent(TramStopAvailableEvent(
                        time = event.time + dwellTime + Constants.tramStopAvailableTime,
                        tramStop = event.tramStop,
                        lane = freeLane
                ))
            }
        }
    }

    private fun scheduleDeparture(logic: StateLogic, event: TramEndpointArrivalEvent, departureTime: Int, freeLane : Int, scheduledTime: Int){
        logic.addEvent(TramDepartureEvent(
                time = departureTime,
                tramStop = event.tramStop,
                tram = event.tram,
                lane = freeLane,
                scheduledTime = scheduledTime
        ))

        // This Tram is departing from CS or P+R, remove its departure time from the list of departure times
        if (event.tramStop.number == 1) {
            if (logic.state.drivingSchedule.tramPRDepartureTimes.isNotEmpty()) {
//                println("Difference with PR schedule: ${DrivingSchedule.tramPRDepartureTimes[0] - event.time}")
                logic.state.drivingSchedule.tramPRDepartureTimes.removeAt(0)
            } else {
                System.err.println("Out of P+R schedule!")
            }
        } else {
            if (logic.state.drivingSchedule.tramCSDepartureTimes.isNotEmpty()) {
//                println("Difference with CS schedule: ${DrivingSchedule.tramCSDepartureTimes[0] - event.time}")
                logic.state.drivingSchedule.tramCSDepartureTimes.removeAt(0)
            } else {
                System.err.println("Out of CS schedule!")
            }
        }

    }

    /**
     * The switch will be occupied for 60 seconds
     * Switch should not be set to busy if the tram starts service
     */
    private fun setSwitchBusy(event: TramEndpointArrivalEvent, logic: StateLogic){
        event.tramStop.switchBusy = true
        logic.addEvent(TramSwitchAvailableEvent(
                time = event.time + Constants.switchTime,
                tramStop = event.tramStop
        ))
    }
}