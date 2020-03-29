package dataprocessing

import simulation.constants.Constants
import java.io.File
import kotlin.math.roundToInt

class ValidationInput(run: Boolean) {

    class DataFile(var passengerArrivalAmounts: MutableList<ArrayList<Double>>, var passengerExitingAmounts: MutableList<ArrayList<Double>>, var tramStopNames: MutableList<String>)
    val combinedData : DataFile
    val exitPercentages : MutableList<MutableList<Double>>

    init {
        if (run) {
            combinedData = readData(File(Constants.validationFile).readLines())
            exitPercentages = getExitPercentages(combinedData)
        } else {
            combinedData = DataFile(mutableListOf(), mutableListOf(), mutableListOf())
            exitPercentages = mutableListOf()
        }
    }

    /**
     * Read the data from the validation input file and return a processed DataFile
     */
    private fun readData(input: List<String>) : DataFile {

        // Prepare the DataFile
        val dataFile = DataFile(ArrayList(), ArrayList(), mutableListOf(
                "P+R De Uithof",
                "WKZ",
                "UMC",
                "Heidelberglaan",
                "Padualaan",
                "Kromme Rijn",
                "Galgenwaard",
                "Vaartsche Rijn",
                "Centraal Station",
                "Vaartsche Rijn 2",
                "Galgenwaard 2",
                "Kromme Rijn 2",
                "Padualaan 2",
                "Heidelberglaan 2",
                "UMC 2",
                "WKZ 2"
        ))

        // Run through the lines that were read from the input file
        input.forEachIndexed { index, line ->
            val tokens = line.split(",", ";")
            if (tokens.isNotEmpty()) {

                // If this line starts with Stop, it contains no data. Let's initialise the data structure.
                if (tokens[0] == "Stop") {

                    // Create empty passenger arrival and exit Lists for each TramStop
                    repeat(18) {
                        dataFile.passengerArrivalAmounts.add(ArrayList())
                        dataFile.passengerExitingAmounts.add(ArrayList())
                    }
                }

                // This line contains TramStop information, lets process it
                else {
                    val timeSlots = ((tokens[3].toDouble() - tokens[2].toDouble()) * 4).toInt()

                    // Direction P+R to CS
                    if (tokens[1].toInt() == 0) {
                        repeat(timeSlots) {
                            dataFile.passengerArrivalAmounts[(index - 1) % 9].add(tokens[4].toDouble() / timeSlots)
                            dataFile.passengerExitingAmounts[(index - 1) % 9].add(tokens[5].toDouble() / timeSlots)
                        }
                    }

                    // Direction CS to P+R
                    else {
                        repeat(timeSlots) {
                            dataFile.passengerArrivalAmounts[(index - 1) % 9 + 9].add(tokens[4].toDouble() / timeSlots)
                            dataFile.passengerExitingAmounts[(index - 1) % 9 + 9].add(tokens[5].toDouble() / timeSlots)
                        }
                    }
                }
            }
        }

        // CS and P+R are both 1 TramStop in our simulation, combine those data points
        dataFile.passengerArrivalAmounts[8] = dataFile.passengerArrivalAmounts[8].plus(dataFile.passengerArrivalAmounts[9])
        dataFile.passengerArrivalAmounts[0] = dataFile.passengerArrivalAmounts[0].plus(dataFile.passengerArrivalAmounts[17])
        dataFile.passengerExitingAmounts[8] = dataFile.passengerExitingAmounts[8].plus(dataFile.passengerExitingAmounts[9])
        dataFile.passengerExitingAmounts[0] = dataFile.passengerExitingAmounts[0].plus(dataFile.passengerExitingAmounts[17])
        dataFile.passengerArrivalAmounts.removeAt(17)
        dataFile.passengerExitingAmounts.removeAt(17)
        dataFile.passengerArrivalAmounts.removeAt(9)
        dataFile.passengerExitingAmounts.removeAt(9)

        return dataFile
    }

    /**
     * Custom plus operator to simplify Array operations
     */
    private operator fun ArrayList<Double>.plus(value: ArrayList<Double>): ArrayList<Double> {
        val newArray = ArrayList<Double>(this.size)
        repeat (this.size) { i ->
            newArray.add(this[i] + value[i])
        }
        return newArray
    }

    /**
     * Returns a list of the percentages of passengers exiting at each TramStop at each 15 minute timeslot
     */
    private fun getExitPercentages(dataFile: DataFile) : MutableList<MutableList<Double>> {

        val tramStopPercentagesList = mutableListOf<MutableList<Double>>()

        repeat(16) { tramStop ->
            val emptyList = mutableListOf<Double>()
            repeat(64) { timeSlot ->
                emptyList.add(0.0)
            }
            tramStopPercentagesList.add(emptyList)
        }

        var currentPassengers = 0.0
        repeat(dataFile.passengerExitingAmounts[0].size) { timeSlot ->

            repeat(dataFile.passengerExitingAmounts.size) { previousTramStopNr ->
                var tramStopNr = previousTramStopNr + 1

                if (tramStopNr >= dataFile.passengerArrivalAmounts.size) {
                    tramStopNr = 0
                }

                currentPassengers += dataFile.passengerArrivalAmounts[previousTramStopNr][timeSlot] // Passengers entered last TramStop
                val exitingPassengers = dataFile.passengerExitingAmounts[tramStopNr][timeSlot] // Passengers exit this TramStop
                var exitingPercentage = if (!(exitingPassengers/currentPassengers).isNaN()) {
                    (((exitingPassengers / currentPassengers) * 100000000)).roundToInt() / 100000000.0 // Let's have this percentage 8 decimals accurate
                } else {
                    0.0
                }

                if (exitingPercentage < 0) {
                     exitingPercentage = 0.0
                } else if (exitingPercentage > 1) {
                    exitingPercentage = 1.0
                }

                tramStopPercentagesList[tramStopNr][timeSlot] = exitingPercentage
                currentPassengers -= exitingPassengers

                if (currentPassengers < 0) {
                    currentPassengers = 0.0
                }
            }
        }
        return tramStopPercentagesList
    }
}

fun main(args: Array<String>) {
    println("Algorithms for Decision Support - Simulation Assignment - Validation \n")

    val input = ValidationInput(true)

    // Print data for Trams for 1 day
    println("Begin input validation file: ${Constants.validationFile}")
    println()
    for (i in input.combinedData.passengerArrivalAmounts.indices) {
        println("TramStop $i - ${input.combinedData.tramStopNames[i]} :")
        println(" Passengers arrived : ${input.combinedData.passengerArrivalAmounts[i].sum()}")
        println(" Passengers arrived : ${input.combinedData.passengerArrivalAmounts[i].toList()}")
        println(" Passengers exited  : ${input.combinedData.passengerExitingAmounts[i].sum()}")
        println(" Passengers exited  : ${input.combinedData.passengerExitingAmounts[i].toList()}")
        println()
    }

    println("End input validation file: ${Constants.validationFile}")
    println("To run another input validation file, edit Constants.validationFile")
}