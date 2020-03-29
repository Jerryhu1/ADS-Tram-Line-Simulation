package simulation.logic

import dataprocessing.OutputLogger
import simulation.constants.Constants
import simulation.eventhandlers.*
import simulation.events.*
import simulation.logging.LoggingData
import simulation.logic.models.*
import simulation.utilities.*
import kotlin.collections.ArrayList
import kotlin.math.roundToInt

class Logic {

    private val stateLogic = StateLogic()
    private var timer = Timer()

    /**
     * Initialises the simulation
     */
    init {
        // Clears all data for a new simulation
        LoggingData.resetData()
        stateLogic.state.drivingSchedule.resetDrivingSchedule()
        stateLogic.state.distributions.clearDistributions()

        // Init: Create the TramStops
        val allTramStops = listOf(
                TramStop(1, "P+R De Uithof", isEndPoint = true),
                TramStop(2, "WKZ"),
                TramStop(3, "UMC"),
                TramStop(4, "Heidelberglaan"),
                TramStop(5, "Padualaan"),
                TramStop(6, "Kromme Rijn"),
                TramStop(7, "Galgenwaard"),
                TramStop(8, "Vaartsche Rijn"),

                TramStop(9, "Centraal Station", isEndPoint = true),
                TramStop(10, "Vaartsche Rijn 2"),
                TramStop(11, "Galgenwaard 2"),
                TramStop(12, "Kromme Rijn 2"),
                TramStop(13, "Padualaan 2"),
                TramStop(14, "Heidelberglaan 2"),
                TramStop(15, "UMC 2"),
                TramStop(16, "WKZ 2")
        )
        stateLogic.setTramStops(allTramStops)

        // Init: Initialise the distributions used in this simulation
        stateLogic.state.distributions.setPassengerArrivalDistribution(allTramStops)
        stateLogic.state.distributions.setPassengerExitingDistributions(allTramStops)
        stateLogic.state.distributions.setDrivingTimeDistributions(allTramStops)

        // Init: Set nextTramStop and previousTramStop for every TramStop. Used for scheduling the next ArrivalEvent
        allTramStops.forEach { tramStop ->
            tramStop.nextTramStop = stateLogic.getNextTramStop(tramStop)
            tramStop.previousTramStop = stateLogic.getPreviousTramStop(tramStop)

            // Logging: Add an empty ArrayList for each TramStop for queue time logging
            LoggingData.tramQueueTimes[tramStop] = ArrayList()
            LoggingData.drivingTimes[tramStop] = ArrayList()

            LoggingData.passengersEnteredPerTramStop[tramStop] = ArrayList()
            LoggingData.passengersExitedPerTramStop[tramStop] = ArrayList()
            LoggingData.passengerWaitingTimesPerTramStop[tramStop] = ArrayList()
        }

        // Logging: Add ArrayLists for drive time logging for CS and P+R
        LoggingData.tramTooEarly[allTramStops[0]] = ArrayList()
        LoggingData.tramTooEarly[allTramStops[8]] = ArrayList()
        LoggingData.tramTooLate[allTramStops[0]] = ArrayList()
        LoggingData.tramTooLate[allTramStops[8]] = ArrayList()
        LoggingData.tramOnTime[allTramStops[0]] = ArrayList()
        LoggingData.tramOnTime[allTramStops[8]] = ArrayList()

        // Init: Create a number of Trams and schedule them for departure according to the driving schedule
        stateLogic.state.drivingSchedule.createDrivingSchedule() // Create the driving schedule using the data from the Constants class
        stateLogic.state.drivingSchedule.tramAdditionTimes.forEachIndexed { index, departureTime ->
            stateLogic.addEvent(AddTramEvent(departureTime, Tram(index+1, Constants.tramCapacity, 0, departureTime)))
        }

        // Init: Schedule Tram removals according to the driving schedule
        stateLogic.state.drivingSchedule.tramRemovalTimes.forEach { removalTime ->
            stateLogic.addEvent(RemoveTramEvent(removalTime))
        }
    }

    /**
     * Run the simulation
     */
    fun runSimulation() {
        // Start a timer
        timer = Timer()

        // Run the simulation until no Events are queued
        while (stateLogic.isQueueNotEmpty()) {

            // Check what kind of Event is scheduled and let the corresponding EventHandler handle the Event
            val event = stateLogic.nextEvent()
            stateLogic.setTime(event.time)

            when (event) {
                is AddTramEvent -> AddTramEventHandler.handleEvent(event, stateLogic)
                is RemoveTramEvent -> RemoveTramEventHandler.handleEvent(event, stateLogic)
                is TramArrivalEvent -> TramArrivalHandler.handleEvent(event, stateLogic)
                is TramDepartureEvent -> TramDepartureHandler.handleEvent(event, stateLogic)
                is TramStopAvailableEvent -> TramStopAvailableHandler.handleEvent(event, stateLogic)
                is TramSwitchAvailableEvent -> TramSwitchAvailableHandler.handleEvent(event, stateLogic)
                is TramEndpointArrivalEvent -> TramEndpointArrivalHandler.handleEvent(event, stateLogic)
                else -> throw Exception("This Event has no Handler associated with it.")
            }
        }

//        if (Constants.validationFile != null) {
//            System.err.println("This simulation ran using this validation input file: ${Constants.validationFile}")
//        }
    }

    /**
     * Print a report of the ran simulation
     */
    fun report() {
        println()
        println("Ended simulation at time ${stateLogic.getTime()}")
        println("The simulation ran from ${Timer.convertSimulationTime(Constants.startTime)} until ${Timer.convertSimulationTime(stateLogic.getTime() + Constants.startTime)}")
        println("The simulation took $timer to simulate")
        println()

        if (Constants.logPassengerNumbers) {
            println("Passengers entered                         : ${LoggingData.passengersEntered}")
            println("Passengers exited                          : ${LoggingData.passengersExited}")
            var entered = 0
            LoggingData.passengersEnteredPerTramStop.values.forEach {
                entered += it.sum()
            }
            var exited = 0
            LoggingData.passengersExitedPerTramStop.values.forEach {
                exited += it.sum()
            }
            println("Passengers entered                         : $entered")
            println("Passengers exited                          : $exited")
//        println("Passengers never entered                   : ${LoggingData.passengersGenerated - LoggingData.passengersEntered}")
            println()
            stateLogic.state.tramStops.forEach {
                println("Passengers                                 : ${LoggingData.passengersEnteredPerTramStop[it]?.sum()} entered and ${LoggingData.passengersExitedPerTramStop[it]?.sum()} exited at tram stop $it")
            }
            println()
        }

        if (Constants.logDistributionNumbers) {
            println()

            var totalEnter = 0.0
            var totalExitPercentages = 0.0

            stateLogic.state.tramStops.forEach {tramStop ->
                var enterMeans = 0.0
                stateLogic.state.distributions.passengerArrivalDistributions[tramStop]?.forEach {
                    enterMeans += it.mean
                }
                totalEnter += enterMeans

                var exitPercentages = 0.0
                stateLogic.state.distributions.passengerExitingPercentages[tramStop]?.forEach {
                    exitPercentages += it
                }
                totalExitPercentages += exitPercentages

                println("Distribution passenger numbers             : ${enterMeans.roundToInt()} should enter and ${(exitPercentages/stateLogic.state.distributions.passengerExitingPercentages[tramStop]!!.size * 100).roundToInt()}% should exit at tram stop $tramStop")

            }
            println()
            println("Distribution passenger total exit          : 100% of passengers per day (although this isn't a log, just println shizzle)")
            println("Distribution passenger total enter         : ${totalEnter.roundToInt()} passengers per day")
            println()
        }

        if (Constants.logPassengerWaitingTime) {
            if (LoggingData.passengerWaitingTimesPerTramStop.isNotEmpty()) {
                var totalWaitingTime = 0.0
                stateLogic.state.tramStops.forEach { tramStop ->
                    val times = LoggingData.passengerWaitingTimesPerTramStop[tramStop]!!
                    if (times.average() != 0.0 && !times.average().isNaN()) {
                        println("Average passenger waiting time             : ${Timer.convertSimulationTime(times.average().roundToInt())} for tram stop $tramStop")
                        totalWaitingTime += times.sum()
                    } else {
                        println("Average passenger waiting time             : 00:00:00 for tram stop $tramStop (no passengers arrived)")
                    }
                }
                val totalAverageWaitingTime = totalWaitingTime / LoggingData.passengersEntered // LoggingData.passengerWaitingTimesPerTramStop.size
                println("Average waiting time of entered passengers : ${Timer.convertSimulationTime(totalAverageWaitingTime.roundToInt())}")
            } else {
                println("Average waiting time of entered passengers : 00:00 (0 seconds)")
            }
            println()
        }

        if (Constants.logTramOnTime) {
            println("Amount of trams too late at CS             : ${LoggingData.tramTooLate[stateLogic.state.tramStops[8]]!!.size}")
            println("Amount of trams too late at P+R            : ${LoggingData.tramTooLate[stateLogic.state.tramStops.first()]!!.size}")
            var tooLateCS = 0
            LoggingData.tramTooLate[stateLogic.state.tramStops[8]]!!.forEach {
                if (it > 60) {
                    tooLateCS++
                }
            }
            var tooLatePR = 0
            LoggingData.tramTooLate[stateLogic.state.tramStops.first()]!!.forEach {
                if (it > 60) {
                    tooLatePR++
                }
            }

            println("Amount of trams >1min too late at CS       : $tooLateCS")
            println("Amount of trams >1min too late at P+R      : $tooLatePR")
            println("Amount of trams too early at CS            : ${LoggingData.tramTooEarly[stateLogic.state.tramStops[8]]!!.size}")
            println("Amount of trams too early at P+R           : ${LoggingData.tramTooEarly[stateLogic.state.tramStops.first()]!!.size}")
            println("Amount of trams on time at CS              : ${LoggingData.tramOnTime[stateLogic.state.tramStops[8]]!!.size}")
            println("Amount of trams on time at P+R             : ${LoggingData.tramOnTime[stateLogic.state.tramStops.first()]!!.size}")
            println()
        }

        if (Constants.logTramQueueTime) {
            stateLogic.state.tramStops.forEach {
                if (LoggingData.tramQueueTimes[it]!!.isNotEmpty()) {
                    println("Average tram queue time                    : ${LoggingData.tramQueueTimes[it]!!.average().roundToInt()} at ${stateLogic.state.tramStops[it.number - 1]}")
                } else {
                    println("Average tram queue time                    : 0 at ${stateLogic.state.tramStops[it.number - 1]}")
                }
            }
            println()
        }

        if (Constants.logDrivingTime) {
            LoggingData.drivingTimes.forEach { tramStop, time ->
                println("Average driving time                       : ${time.average().toInt()} to tram stop $tramStop")
            }
        }

        if (Constants.logTramAmounts) {
            println()
            println("Trams added                                : ${LoggingData.tramsAdded}")
            println("Trams removed                              : ${LoggingData.tramsRemoved}")
        }

        if (Constants.logTramRoundTrips) {
            println()
            println("Number of round trips                      : ${LoggingData.roundTrips} (should be 198 with the regular schedule)")
            println("Number of departures from P+R              : ${LoggingData.departuresFromPR} (should be 198 with the regular schedule)")
        }

        if (Constants.printDriveTimes) {
            println()
            stateLogic.state.tramStops.forEach { tramStop ->
                println("Drive time is on average                   : ${stateLogic.state.distributions.drivingTimeDistributions[tramStop]!!.numericalMean.roundToInt()}s to drive to ${tramStop.nextTramStop}")
            }
        }
    }

    fun logOutput(){
        OutputLogger.writeData(stateLogic)

    }


}