package simulation.eventhandlers

import simulation.constants.Constants
import simulation.constants.Distributions
import simulation.events.*
import simulation.interfaces.Event
import simulation.interfaces.EventHandler
import simulation.logging.LoggingData
import simulation.logic.StateLogic
import kotlin.math.roundToInt

object TramDepartureHandler : EventHandler {
    override fun handleEvent(event: Event, logic: StateLogic) {
        // Cast this Event to a TramDepartureEvent
        event as TramDepartureEvent

        // If the TramStop is an endpoint and the switch is occupied, we'll have to wait before departing
        if (event.tramStop.isEndPoint && event.tramStop.switchBusy) {
            event.tramStop.tramQueue.addFirst(event)
        }

        // If the TramStop is no endpoint, or
        // If the TramStop is an endpoint and the switch is free, the Tram can depart
        else {
            event.tramStop.tramsOnStop.remove(event.tram)

            // How many passengers arrived between this Tram arrival and this departure?
            // Add the extra dwell time to the departure time of this Event
            val newPassengers = handleNewPassengers(logic, event)
            val extraDwellTime = getExtraDwellTime(logic, newPassengers)

            event.time += extraDwellTime

            // Get the driving time from this TramStop to the next TramStop
            val drivingTime = (logic.state.distributions.drivingTimeDistributions[event.tramStop]!!.sample()).roundToInt()

            // Event: Schedule ArrivalEvent at the next TramStop.
            // If that is an endpoint we'll arrive at the switch 60 seconds before the TramStop.
            scheduleNextArrival(event, drivingTime, logic)

            // Event: If this is an endpoint, the switch will be occupied for 1 minute
            if (event.tramStop.isEndPoint) {
                event.tramStop.switchBusy = true
                logic.addEvent(TramSwitchAvailableEvent(
                        time = event.time + Constants.switchTime,
                        tramStop = event.tramStop
                ))

                event.tram.endpointDepartureTime = event.time // Set the time this Tram departed from CS or P+R
            }

            // Add driving time for logging
            LoggingData.drivingTimes[event.tramStop.nextTramStop]!!.add(drivingTime)

            // Event: The tram stop will be available again after 40 seconds
            logic.addEvent(TramStopAvailableEvent(
                    time = event.time + Constants.tramStopAvailableTime,
                    tramStop = event.tramStop,
                    lane = event.lane
            ))

            // Logging
            logDepartureTime(event)
        }
    }

    /**
     * Schedule the arrival of the current Tram at the next TramStop
     */
    private fun scheduleNextArrival(event: TramDepartureEvent, drivingTime: Int, logic: StateLogic) {
        // If the next TramStop is CS or P+R, it is a special arrival event
        val tramArrEv =  if (event.tramStop.nextTramStop.isEndPoint) {
            TramEndpointArrivalEvent(
                    time = event.time + drivingTime,
                    tramStop = event.tramStop.nextTramStop,
                    tram = event.tram,
                    causedBy = event
            )
        } else {
            TramArrivalEvent(
                    time = event.time + drivingTime,
                    tramStop = event.tramStop.nextTramStop,
                    tram = event.tram
            )
        }
        logic.addEvent(tramArrEv)

        // Save the last departure from the current TramStop
        event.tramStop.previousTramDeparture = event

        // This Tram is now driving towards the next TramStop, we will add it to the driving queue
        event.tramStop.nextTramStop.nextTramArrivalQueue.add(tramArrEv)
    }

    /**
     * If new passengers arrived in the time that this Tram arrived and this TramDepartureEvent, let them enter if possible
     */
    private fun handleNewPassengers(logic: StateLogic, event: TramDepartureEvent) : Int {
        // Let the waiting passengers enter. Add them to the Tram and remove them from the TramStop queue
        val arrivalTime = event.tramStop.previousTramArrival?.time ?: 0    // The time at which this Tram arrived
        val departingTime = event.time                                     // The time this tram departs the TramStop

        // Add the amount of passengers that arrived since the previous Tram departure to the tramStop.passengerArrivalQueue
        event.tramStop.passengerArrivalQueue.addAll(logic.state.distributions.getPassengerArrivalInfo(event.tramStop, arrivalTime, departingTime))

        // Passengers enter as long as there is space for them
        val enteringPassengerInfo = mutableListOf<Distributions.PassengerInfo>()
        while (event.tram.getAvailableCapacity() > 0 && event.tramStop.passengerArrivalQueue.isNotEmpty()) {
            // If there is enough space, let all passengers from this timeslot enter
            if (event.tram.getAvailableCapacity() >= event.tramStop.passengerArrivalQueue.peek().arrivalAmount) {
                val enteringPassengers = event.tramStop.passengerArrivalQueue.peek().arrivalAmount
                event.tram.passengerAmount += enteringPassengers // Add them to the Tram
                enteringPassengerInfo.add(event.tramStop.passengerArrivalQueue.poll()) // Remove from TramStop and save this
            } else {
                // If there is not enough space, let only some passengers enter
                val enteringPassengers = event.tram.getAvailableCapacity()
                event.tramStop.passengerArrivalQueue.peek().arrivalAmount -= enteringPassengers // Remove them from the queue
                event.tram.passengerAmount += enteringPassengers // Add them to the Tram
                enteringPassengerInfo.add(Distributions.PassengerInfo(enteringPassengers, event.tramStop.passengerArrivalQueue.peek().avgWaitingTime)) // Save this
            }
        }

        // See how many passengers entered
        var enteringPassengers = 0
        enteringPassengerInfo.forEach() { passengerInfo ->
            repeat(passengerInfo.arrivalAmount) {
                enteringPassengers++

                // Update the logging
                LoggingData.passengerWaitingTimesPerTramStop[event.tramStop]!!.add(passengerInfo.avgWaitingTime)
            }
        }
        // Update the logging
        LoggingData.passengersEntered += enteringPassengers
        LoggingData.passengersEnteredPerTramStop[event.tramStop]?.add(enteringPassengers)

        return enteringPassengers
    }

    /**
     * Mean dwell time: d = 0.22 * passengers_in
     * Returns the extra dwell time of a Tram if new passengers arrived
     */
    private fun getExtraDwellTime(logic: StateLogic, enteringPassengers: Int, minDwellTime: Double = 0.0) : Int {
        // Get the dwell time for the amount of passengers exiting and entering the Tram
        val dwellTimeMean = Constants.p_in * enteringPassengers
        var dwellTime =  if (dwellTimeMean == 0.0) {
            0.0
        } else {
            logic.state.distributions.getGammaDistribution(dwellTimeMean, Constants.k).sample()
        }

        // The dwell time is always positive, and at CS and P+R at least 3 minutes
        if (dwellTime < minDwellTime) {
            dwellTime = minDwellTime
        }

        return dwellTime.roundToInt()
    }

    /**
     * Update the logging on how often a Tram arrives on time or not
     */
    private fun logDepartureTime(event: TramDepartureEvent){
        val delay = event.time - event.scheduledTime
        // If bigger than 0 we are too late, if smaller we are too early, if 0 we are right on time
        when {
            delay > 0 -> LoggingData.tramTooLate[event.tramStop]?.add(delay)
            delay < 0 -> LoggingData.tramTooEarly[event.tramStop]?.add(delay)
            delay == 0 -> LoggingData.tramOnTime[event.tramStop]?.add(delay)
        }

        // P+R Departures logging
        if (event.tramStop.number == 1) {
            LoggingData.departuresFromPR++
        }else if(event.tramStop.number == 8){
            LoggingData.departuresFromCS++
        }
    }
}