package simulation.eventhandlers

import simulation.constants.Constants
import simulation.constants.Distributions
import simulation.events.TramArrivalEvent
import simulation.events.TramDepartureEvent
import simulation.interfaces.*
import simulation.logging.LoggingData
import simulation.logic.StateLogic
import kotlin.math.roundToInt

object TramArrivalHandler : EventHandler {

    class PassengerAmounts(val enteringPassengers: Int, val exitingPassengers: Int, val waitingTimeInfo: List<Distributions.PassengerInfo>)

    override fun handleEvent(event: Event, logic: StateLogic) {
        // Cast this Event to a TramArrivalEvent
        event as TramArrivalEvent

        // If the TramStop is occupied or was in the past 40 seconds OR if there is another tram in front, this Tram will have to wait
        val nextArrival = event.tramStop.nextTramArrivalQueue.peek()
        if  (event.tramStop.isBusy ||
            (nextArrival != null && nextArrival != event)) {
            if (nextArrival == event) {
                event.tramStop.tramQueue.addFirst(event)
            }
            else {
                event.tramStop.tramQueue.addLast(event)
            }
        }
        else{
            event.tram.currentTramStop = event.tramStop
            event.tramStop.tramsOnStop.add(event.tram)

            // If the TramStop was free, the Tram will arrive at the TramStop
            event.tramStop.isBusy = true

            // Remove this Tram from the nextTramArrivalQueue
            event.tramStop.nextTramArrivalQueue.poll()
            event.tramStop.previousTramArrival = event

            // Let all the passengers exit and enter, and save these amounts
            val passengerInfo = handlePassengers(logic, event)

            // Get the dwell time of the Tram at the current TramStop, with a minimum of 0.8 * the mean dwell time
            val dwellTime = getDwellTime(logic, passengerInfo)

            // Update the logging
            passengerInfo.waitingTimeInfo.forEach{ passengerInfo ->
                repeat(passengerInfo.arrivalAmount) {
                    LoggingData.passengerWaitingTimesPerTramStop[event.tramStop]!!.add(passengerInfo.avgWaitingTime + dwellTime)
                }
            }

            // Schedule the TramDepartureEvent from the current TramStop
            logic.addEvent(TramDepartureEvent(
                    time = event.time + dwellTime,
                    tramStop = event.tramStop,
                    tram = event.tram
            ))
        }
    }

    /**
     * Let the passengers at this TramStop exit and enter the Tram
     */
    fun handlePassengers(logic: StateLogic, event: Event, isLeaving: Boolean = false) : PassengerAmounts {
        // This function is called by either a TramArrivalEvent or TramEndpointArrivalEvent
        event as TramArrivalInterface
        //println("Time ${event.time}: ${event.tram} arrived at TramStop ${event.tramStop.number} (${event.tramStop}) with ${event.tram.passengerAmount} passengers")

        // Let the appropriate amount of passengers exit
        val exitingPassengers = if (event.tramStop.isEndPoint) {
            event.tram.passengerAmount // At an endpoint 100% of passengers should exit
        } else {
            logic.state.distributions.getPassengerExitingDistribution(event.tramStop, event.time, event.tram.passengerAmount).sample()
        }
        event.tram.passengerAmount -= exitingPassengers

        // Update the logging
        LoggingData.passengersExited += exitingPassengers
        LoggingData.passengersExitedPerTramStop[event.tramStop]?.add(exitingPassengers)

        // If this tram is leaving the line do not let any passengers enter
        if (isLeaving) {
            return PassengerAmounts(0, exitingPassengers, emptyList())
        }

        // Let the waiting passengers enter. Add them to the Tram and remove them from the TramStop queue
        val previousTime = event.tramStop.previousTramDeparture?.time ?: 0 // The time at which the last tram departed
        val arrivalTime = event.time                                       // The time this tram arrives at the TramStop

        // If there were still passengers waiting from the previous departure time, increase their waiting time
        if (event.tramStop.passengerArrivalQueue.isNotEmpty()) {
            event.tramStop.passengerArrivalQueue.forEach { passengerInfo ->
                passengerInfo.avgWaitingTime += arrivalTime - previousTime
            }
        }

        // Add the amount of passengers that arrived since the previous Tram departure to the tramStop.passengerArrivalQueue
        event.tramStop.passengerArrivalQueue.addAll(logic.state.distributions.getPassengerArrivalInfo(event.tramStop, previousTime, arrivalTime))

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
        enteringPassengerInfo.forEach { passengerInfo ->
            repeat(passengerInfo.arrivalAmount) {
                enteringPassengers++
            }
        }
        // Update the logging
        LoggingData.passengersEntered += enteringPassengers
        LoggingData.passengersEnteredPerTramStop[event.tramStop]?.add(enteringPassengers)

        return PassengerAmounts(enteringPassengers, exitingPassengers, enteringPassengerInfo)
    }

    /**
     * Mean dwell time: d = 12.5 + 0.22 * passengers_in + 0.13 * passengers_out
     *
     * Returns the dwell time of a Tram
     * With a minimum of 0.8d, and a minimum of either 0 or 3 minutes if this is CS or P+R
     */
    fun getDwellTime(logic: StateLogic, passengerInfo: PassengerAmounts, minDwellTime: Int = 0) : Int {
        // Get the dwell time for the amount of passengers exiting and entering the Tram
        val dwellTimeMean = Constants.p_const + (Constants.p_in * passengerInfo.enteringPassengers) + (Constants.p_out * passengerInfo.exitingPassengers)
        var dwellTime =  if (dwellTimeMean == 0.0) {
            0.0
        } else {
            logic.state.distributions.getGammaDistribution(dwellTimeMean, Constants.k).sample()
        }

        // The minimum dwell time is 0.8d
        if (dwellTime < (Constants.dwellMin * dwellTimeMean)) {
            dwellTime = Constants.dwellMin * dwellTimeMean
        }

        // The dwell time is always positive, and at CS and P+R at least 3 minutes
        if (dwellTime < minDwellTime) {
            dwellTime = minDwellTime.toDouble()
        }

        return dwellTime.roundToInt()
    }
}