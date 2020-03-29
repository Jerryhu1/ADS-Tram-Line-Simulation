package simulation.constants

import simulation.utilities.Timer.Companion.convertSimulationTime

class DrivingSchedule {

    var tramPRDepartureTimes = mutableListOf<Int>()
    var tramCSDepartureTimes = mutableListOf<Int>()
    var tramAdditionTimes = mutableListOf<Int>()
    var tramRemovalTimes = mutableListOf<Int>()

    fun createDrivingSchedule() {

        var tramsAvailable = 0
        val tramsDriving = mutableListOf<Int>()

        // Schedule Trams from 6:00 until 21:30
        repeat ((Constants.maxRunTime + (Constants.scheduleRoundtripTime)) / 60) { i ->
            val simulationTime = i * 60 // Schedule is per minute

            // Check if a Tram has arrived back at P+R
            var removeTram = false
            tramsDriving.forEach { additionTime ->
                if (simulationTime - additionTime == Constants.scheduleRoundtripTime) {
//                    println("At ${convertSimulationTime(simulationTime + 21600)} the tram that started at ${convertSimulationTime(additionTime + 21600)} got back. It drove for ${convertSimulationTime(simulationTime - additionTime)} ")
                    removeTram = true
                    tramsAvailable++
                }
            }
            if (removeTram) {
                tramsDriving.removeAt(0)
            }

            // Until 21:30 we can schedule new trams, after that we only remove them
            if (simulationTime < Constants.lastTramScheduleTime) {
                // Add the trams
                if (simulationTime < Constants.peakTime) {
                    // Schedule a Tram every 12 minutes in the offpeak hours
                    if (simulationTime % Constants.offPeakTramFrequency == 0) {
                        tramPRDepartureTimes.add(simulationTime)
                        tramsDriving.add(simulationTime)
                        if (tramsAvailable <= 0) {
                            tramAdditionTimes.add(simulationTime - Constants.tramStartBufferTime) // Trams are added some time before their scheduled departure time
                        } else {
                            tramsAvailable--
                        }
                    }
                } else if (simulationTime < Constants.offPeakTime) {
                    // Schedule a Tram every 4 minutes in the offpeak hours
                    if (simulationTime % Constants.peakTramFrequency == 0) {
                        tramPRDepartureTimes.add(simulationTime)
                        tramsDriving.add(simulationTime)
                        if (tramsAvailable <= 0) {
                            tramAdditionTimes.add(simulationTime - Constants.tramStartBufferTime) // Trams are added some time before their scheduled departure time
                        } else {
                            tramsAvailable--
                        }
                    }
                } else {
                    // This is after the peak hours, if there is more than 1 Tram available, remove it
                    if (tramsAvailable > 1) {
                        tramRemovalTimes.add(simulationTime)
                        tramsAvailable--
                    }
                    // Schedule a Tram every 12 minutes in the offpeak hours
                    if (simulationTime % Constants.offPeakTramFrequency == 0) {
                        tramPRDepartureTimes.add(simulationTime)
                        tramsDriving.add(simulationTime)
                        tramsAvailable--
                    }
                }
            } else {
                // If we're outside of the driving schedule, no Trams are added and only Trams will be removed
                if (tramsAvailable >= 1) {
                    tramRemovalTimes.add(simulationTime)
                    tramsAvailable--
                }
            }
        }

        // Now that we know the departure times at P+R, we add the corresponding departure times from CS
        tramPRDepartureTimes.forEach {
            tramCSDepartureTimes.add(it + Constants.drivingTime + Constants.scheduleEndPointDwell)
        }
    }

    /**
     * Reset the driving schedule
     */
    fun resetDrivingSchedule(){
        tramPRDepartureTimes.clear()
        tramCSDepartureTimes.clear()
        tramAdditionTimes.clear()
        tramRemovalTimes.clear()
    }
}

/**
 * Run this to see the schedule
 */
fun main(args: Array<String>) {
    println("Algorithms for Decision Support - Simulation Assignment - Driving Schedule \n")

    val newSchedule = DrivingSchedule()

    newSchedule.createDrivingSchedule()

    newSchedule.tramPRDepartureTimes.forEach {
        println("PR departure times: " + simulation.utilities.Timer.convertSimulationTime(it + Constants.startTime))
    }

    newSchedule.tramCSDepartureTimes.forEach {
        println("CS departure times: " + simulation.utilities.Timer.convertSimulationTime(it + Constants.startTime))
    }

    newSchedule.tramAdditionTimes.forEach {
        println("Addition times: " + convertSimulationTime(it + Constants.startTime))
    }

    newSchedule.tramRemovalTimes.forEach {
        println("Removal times: " + convertSimulationTime(it + Constants.startTime))
    }
}