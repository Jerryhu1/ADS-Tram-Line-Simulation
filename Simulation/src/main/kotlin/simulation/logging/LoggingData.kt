package simulation.logging

import simulation.logic.models.TramStop

object LoggingData {

    // Overall logging variables
    var passengersEntered = 0
    var passengersExited = 0
    var tramsAdded = 0
    var tramsRemoved = 0
    var roundTrips = 0
    var departuresFromPR = 0
    var departuresFromCS = 0

    // Logging variables for all TramStops
    var tramQueueTimes : MutableMap<TramStop, ArrayList<Int>> = HashMap()
    var drivingTimes : MutableMap<TramStop, ArrayList<Int>> = HashMap()
    var passengersEnteredPerTramStop : MutableMap<TramStop, ArrayList<Int>> = HashMap()
    var passengersExitedPerTramStop : MutableMap<TramStop, ArrayList<Int>> = HashMap()
    var passengerWaitingTimesPerTramStop : MutableMap<TramStop, ArrayList<Double>> = HashMap()

    // Logging variables for CS and P+R
    var tramTooLate : MutableMap<TramStop, ArrayList<Int>> = HashMap()
    var tramTooEarly : MutableMap<TramStop, ArrayList<Int>> = HashMap()
    var tramOnTime : MutableMap<TramStop, ArrayList<Int>> = HashMap()

    /**
     * Reset the fields, for running the simulation multiple times
      */
    fun resetData(){
        passengersEntered = 0
        passengersExited = 0
        passengersEnteredPerTramStop.clear()
        passengersExitedPerTramStop.clear()
        passengerWaitingTimesPerTramStop.clear()
        tramQueueTimes.clear()
        tramTooEarly.clear()
        tramTooLate.clear()
        tramOnTime.clear()
        drivingTimes.clear()
        tramsAdded = 0
        tramsRemoved = 0
        roundTrips = 0
        departuresFromPR = 0
        departuresFromCS = 0
    }
}