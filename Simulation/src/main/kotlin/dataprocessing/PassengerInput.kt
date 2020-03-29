package dataprocessing

import simulation.constants.Constants
import java.io.File
import kotlin.math.roundToInt

class PassengerInput(run: Boolean) {

    class DataFile(var passengerArrivalAmounts: MutableList<DoubleArray>, var passengerExitingAmounts: MutableList<DoubleArray>, var tramStopNames: MutableList<String>)

    // Bus stop AZU is split in 3 stops, the passengers should be distributed as follows:
    // The amount of passengers that enter at PRenter, WKZenter and UMCenter in the forecasted P+R -> CS Tram
    private val enterPR = 14.5278371706583
    private val enterWKZ = 1014.55398000707
    private val enterUMC = 2660.24398536867
    private val enterTotal = enterPR + enterWKZ + enterUMC // What AZU would be, constructed from tha 12a and 12b data files
    private val enterPRpercentage = enterPR / enterTotal
    private val enterWKZpercentage = enterWKZ / enterTotal
    private val enterUMCpercentage = enterUMC / enterTotal

    // Bus stop AZU is split in 3 stops, the passengers should be distributed as follows:
    // The amount of passengers that exit at PRenter, WKZenter and UMCenter in the forecasted CS -> P+R Tram
    private val exitPR = 14.6183316043998
    private val exitWKZ = 643.902701622372
    private val exitUMC = 2577.28843406885
    private val exitTotal = exitPR + exitWKZ + exitUMC // What AZU would be, constructed from tha 12a and 12b data files
    private val exitPRpercentage = exitPR / exitTotal
    private val exitWKZpercentage = exitWKZ / exitTotal
    private val exitUMCpercentage = exitUMC / exitTotal

    // Scaling the amount of passengers to match the 2020 forecast, divided by 21 to get the amount for 1 day
    private val scalingCStoPR = 22240.1556751767 / 9620 / 21 * Constants.passengerScaler // 12a
    private val scalingPRtoCS = 22759.8443248234 / 10090 / 21 * Constants.passengerScaler // 12b

    var combinedData : DataFile
    var exitPercentages : MutableList<MutableList<Double>>

    init {
        if (run) {
            // NOTE: No passengers enter at UMC and WKZ while driving towards P+R, and no passengers exit
            // at UMC and WKZ while driving towards CS. This is because AZU was the endpoint in the input data.
            val data12a = readData(File("../Datasets/12a.csv").readLines())
            val data12b = readData(File("../Datasets/12b.csv").readLines())

            val processedData12a = processData12a(data12a)
            val processedData12b = processData12b(data12b)

            combinedData = combineDataFiles(processedData12a, processedData12b)
            exitPercentages = getExitPercentages(combinedData)
        } else {
            combinedData = DataFile(mutableListOf(), mutableListOf(), mutableListOf())
            exitPercentages = mutableListOf()
        }
    }

    /**
     * Put the List of Strings of 1 csv file into a predefined DataFile
     */
    private fun readData(input: List<String>) : DataFile {

        val dataFile = DataFile(ArrayList(), ArrayList(), mutableListOf())

        input.forEach { line ->
            val tokens = line.split(",", ";")
            if (tokens.isNotEmpty()) {

                // If this line starts with Trip it contains the es
                if (tokens[0] == "Trip") {
                    // Create empty Lists for the entering passengers
                    for (i in 3..11) {
                        val emptyList = DoubleArray(64)
                        repeat (64) {j ->
                            emptyList[j] = 0.0
                        }
                        dataFile.passengerArrivalAmounts.add(emptyList.copyOf())
                        dataFile.tramStopNames.add(tokens[i])
                    }

                    // Create empty Lists for the exiting passengers
                    for (i in 12..20) {
                        val emptyList = DoubleArray(64)
                        repeat (64) {j ->
                            emptyList[j] = 0.0
                        }
                        dataFile.passengerExitingAmounts.add(emptyList.copyOf())
//                        dataFile.tramStopExitingNames.add(tokens[i])
                    }

                }

                // If this line starts with R it contains the timestamp and the amount of arriving passengers
                else if (tokens[0].startsWith("R")) {
                    val timeString = tokens[2]
                    var timeslot = -1
                    if (timeString.isNotEmpty()) {
                        val simulationTimeStamp = if (timeString.length == 4) {
                            timeString[0].toString().toInt() * 60 * 60 + timeString[2].toString().toInt() * 10 * 60 + timeString[3].toString().toInt() * 60
                        } else {
                            timeString[0].toString().toInt() * 10 * 60 * 60 + timeString[1].toString().toInt() * 60 * 60 + timeString[3].toString().toInt() * 10 * 60 + timeString[4].toString().toInt() * 60
                        }
                        timeslot = (simulationTimeStamp / (60 * 15)) - (6 * 4) // In which 15 minute timeslot does this fall, between 0 and 63
                    }

                    // Add the arriving passengers to the data
                    for (i in 3..11) {
                        val tramStopNr = i-3
                        dataFile.passengerArrivalAmounts[tramStopNr][timeslot] = dataFile.passengerArrivalAmounts[tramStopNr][timeslot] + tokens[i].toInt()
                    }

                    // Add the exiting passengers to the data
                    for (i in 12..20) {
                        val tramStopNr = i-12
                        dataFile.passengerExitingAmounts[tramStopNr][timeslot] = dataFile.passengerExitingAmounts[tramStopNr][timeslot] + tokens[i].toInt()
                    }
                }
            }
        }
        return dataFile
    }

    /**
     * Process the 12a input data.
     * [AZU, Heidelberglaan, Padualaan, De Kromme Rijn, Stadion Galgenwaard, Rubenslaan, Sterrenwijk, Bleekstraat, CS Centrumzijde]
     *
     * Rubenslaan will be combined with Galgenwaard
     * Sterrenwijk will be combined with Vaartscherijn (Bleekstraat)
     * AZU will be split into UMC, WKZ and P+R Uithof according to the calculated percentages
     */
    private fun processData12a(dataFile: DataFile) : DataFile{

        val arrivalAmounts = mutableListOf<DoubleArray>().apply { addAll(dataFile.passengerArrivalAmounts) }
        val exitingAmounts = mutableListOf<DoubleArray>().apply { addAll(dataFile.passengerExitingAmounts) }

        // 12a arrival processing
        // Match the numbers in the 2020 forecast
        // [AZU, Heidelberglaan, Padualaan, De Kromme Rijn, Stadion Galgenwaard, Rubenslaan, Sterrenwijk, Bleekstraat, CS Centrumzijde]
        arrivalAmounts[0] = dataFile.passengerArrivalAmounts[0] * enterPRpercentage * scalingCStoPR// PR
        arrivalAmounts[1] = dataFile.passengerArrivalAmounts[0] * enterWKZpercentage * scalingCStoPR// WKZ
        arrivalAmounts[2] = dataFile.passengerArrivalAmounts[0] * enterUMCpercentage * scalingCStoPR// UMC
        arrivalAmounts[3] = dataFile.passengerArrivalAmounts[1] * scalingCStoPR// Heidelberglaan
        arrivalAmounts[4] = dataFile.passengerArrivalAmounts[2] * scalingCStoPR// Padualaan
        arrivalAmounts[5] = dataFile.passengerArrivalAmounts[3] * scalingCStoPR// Kromme Rijn
        arrivalAmounts[6] = (dataFile.passengerArrivalAmounts[4] + dataFile.passengerArrivalAmounts[5]) * scalingCStoPR // Galgenwaard
        arrivalAmounts[7] = (dataFile.passengerArrivalAmounts[6] + dataFile.passengerArrivalAmounts[7]) * scalingCStoPR // Vaartsche Rijn
        arrivalAmounts[8] = dataFile.passengerArrivalAmounts[8] * scalingCStoPR // Centraal Station

        // 12a exiting processing
        // Match the numbers in the 2020 forecast
        // [AZU, Heidelberglaan, Padualaan, De Kromme Rijn, Stadion Galgenwaard, Rubenslaan, Sterrenwijk, Bleekstraat, CS Centrumzijde]
        exitingAmounts[0] = dataFile.passengerExitingAmounts[0] * exitPRpercentage * scalingCStoPR // PR
        exitingAmounts[1] = dataFile.passengerExitingAmounts[0] * exitWKZpercentage * scalingCStoPR // WKZ
        exitingAmounts[2] = dataFile.passengerExitingAmounts[0] * exitUMCpercentage * scalingCStoPR // UMC
        exitingAmounts[3] = dataFile.passengerExitingAmounts[1] * scalingCStoPR // Heidelberglaan
        exitingAmounts[4] = dataFile.passengerExitingAmounts[2] * scalingCStoPR // Padualaan
        exitingAmounts[5] = dataFile.passengerExitingAmounts[3] * scalingCStoPR // Kromme Rijn
        exitingAmounts[6] = (dataFile.passengerExitingAmounts[4] + dataFile.passengerExitingAmounts[5]) * scalingCStoPR // Galgenwaard
        exitingAmounts[7] = (dataFile.passengerExitingAmounts[6] + dataFile.passengerExitingAmounts[7]) * scalingCStoPR // Vaartsche Rijn
        exitingAmounts[8] = dataFile.passengerExitingAmounts[8] * scalingCStoPR // Centraal Station

        val names12a = mutableListOf(
                "P+R De Uithof",
                "WKZ",
                "UMC",
                "Heidelberglaan",
                "Padualaan",
                "Kromme Rijn",
                "Galgenwaard",
                "Vaartsche Rijn",
                "Centraal Station"
        )

        return DataFile(arrivalAmounts, exitingAmounts, names12a)
    }

    /**
     * Process the 12b input data.
     * [CS Centrumzijde, Bleekstraat, Sterrenwijk, Rubenslaan, Stadion Galgenwaard, De Kromme Rijn, Padualaan, Heidelberglaan, AZU]
     *
     * Rubenslaan will be combined with Galgenwaard
     * Sterrenwijk will be combined with Vaartscherijn (Bleekstraat)
     * AZU will be split into UMC, WKZ and P+R Uithof according to the calculated percentages
     */
    private fun processData12b(dataFile: DataFile) : DataFile {

        val arrivalAmounts = mutableListOf<DoubleArray>().apply { addAll(dataFile.passengerArrivalAmounts) }
        val exitingAmounts = mutableListOf<DoubleArray>().apply { addAll(dataFile.passengerExitingAmounts) }

        // 12b arrival processing
        // Match the numbers in the 2020 forecast
        // [CS Centrumzijde, Bleekstraat, Sterrenwijk, Rubenslaan, Stadion Galgenwaard, De Kromme Rijn, Padualaan, Heidelberglaan, AZU]
        arrivalAmounts[8] = dataFile.passengerArrivalAmounts[8] * enterPRpercentage * scalingPRtoCS // PR
        arrivalAmounts[7] = dataFile.passengerArrivalAmounts[8] * enterWKZpercentage * scalingPRtoCS // WKZ
        arrivalAmounts[6] = dataFile.passengerArrivalAmounts[8] * enterUMCpercentage * scalingPRtoCS // UMC
        arrivalAmounts[5] = dataFile.passengerArrivalAmounts[7] * scalingPRtoCS // Heidelberglaan
        arrivalAmounts[4] = dataFile.passengerArrivalAmounts[6] * scalingPRtoCS // Padualaan
        arrivalAmounts[3] = dataFile.passengerArrivalAmounts[5] * scalingPRtoCS // Kromme Rijn
        arrivalAmounts[2] = (dataFile.passengerArrivalAmounts[4] + dataFile.passengerArrivalAmounts[3]) * scalingPRtoCS // Galgenwaard
        arrivalAmounts[1] = (dataFile.passengerArrivalAmounts[2] + dataFile.passengerArrivalAmounts[1]) * scalingPRtoCS // Vaartsche Rijn
        arrivalAmounts[0] = dataFile.passengerArrivalAmounts[0] * scalingPRtoCS // Centraal Station

        // 12b exiting processing
        // Match the numbers in the 2020 forecast
        // [CS Centrumzijde, Bleekstraat, Sterrenwijk, Rubenslaan, Stadion Galgenwaard, De Kromme Rijn, Padualaan, Heidelberglaan, AZU]
        exitingAmounts[8] = dataFile.passengerExitingAmounts[8] * exitPRpercentage * scalingPRtoCS // PR
        exitingAmounts[7] = dataFile.passengerExitingAmounts[8] * exitWKZpercentage * scalingPRtoCS // WKZ
        exitingAmounts[6] = dataFile.passengerExitingAmounts[8] * exitUMCpercentage * scalingPRtoCS // UMC
        exitingAmounts[5] = dataFile.passengerExitingAmounts[7] * scalingPRtoCS // Heidelberglaan
        exitingAmounts[4] = dataFile.passengerExitingAmounts[6] * scalingPRtoCS // Padualaan
        exitingAmounts[3] = dataFile.passengerExitingAmounts[5] * scalingPRtoCS // Kromme Rijn
        exitingAmounts[2] = (dataFile.passengerExitingAmounts[4] + dataFile.passengerExitingAmounts[3]) * scalingPRtoCS // Galgenwaard
        exitingAmounts[1] = (dataFile.passengerExitingAmounts[2] + dataFile.passengerExitingAmounts[1]) * scalingPRtoCS // Vaartsche Rijn
        exitingAmounts[0] = dataFile.passengerExitingAmounts[0] * scalingPRtoCS // Centraal Station

        val names12b = mutableListOf(
                "Centraal Station",
                "Vaartsche Rijn",
                "Galgenwaard",
                "Kromme Rijn",
                "Padualaan",
                "Heidelberglaan",
                "UMC",
                "WKZ",
                "P+R De Uithof"
        )

        return DataFile(arrivalAmounts, exitingAmounts, names12b)
    }

    /**
     * Combine the 12a and 12b files into a single file
     * All TramStops, apart from P+R and CS, are listed twice because they have 2 directions
     */
    private fun combineDataFiles(data12a: DataFile, data12b: DataFile) : DataFile {

        val tramStopNames = mutableListOf<String>()
        val arrivalAmounts = mutableListOf<DoubleArray>()
        val exitingAmounts = mutableListOf<DoubleArray>()

        tramStopNames.addAll(data12a.tramStopNames)
        tramStopNames.removeAt(tramStopNames.size-1)
        tramStopNames.addAll(data12b.tramStopNames)
        tramStopNames.removeAt(tramStopNames.size-1)

        arrivalAmounts.addAll(data12a.passengerArrivalAmounts)
        arrivalAmounts.removeAt(arrivalAmounts.size-1)
        arrivalAmounts.addAll(data12b.passengerArrivalAmounts)
        arrivalAmounts.removeAt(arrivalAmounts.size-1)

        exitingAmounts.addAll(data12a.passengerExitingAmounts)
        exitingAmounts[0] = data12b.passengerExitingAmounts[data12b.passengerExitingAmounts.size-1]
        val thesize = exitingAmounts.size
        exitingAmounts.addAll(data12b.passengerExitingAmounts)
        exitingAmounts.removeAt(thesize)
        exitingAmounts.removeAt(exitingAmounts.size-1)

        return DataFile(arrivalAmounts, exitingAmounts, tramStopNames)
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

    /**
     * Custom times operator to simplify Array operations
     */
    private operator fun DoubleArray.times(value: Double): DoubleArray {
        val newArray = DoubleArray(this.size)
        repeat (this.size) { i ->
            newArray[i] = (this[i] * value)
        }
        return newArray
    }

    /**
     * Custom plus operator to simplify Array operations
     */
    private operator fun DoubleArray.plus(value: DoubleArray): DoubleArray {
        val newArray = DoubleArray(this.size)
        repeat (this.size) { i ->
            newArray[i] = (this[i] + value[i])
        }
        return newArray
    }
}

fun main(args: Array<String>) {
    println("Algorithms for Decision Support - Simulation Assignment - Input Data \n")

    val input = PassengerInput(true)

    // Print data for Trams for 1 day
    for (i in input.combinedData.passengerArrivalAmounts.indices) {
        println("Name: ${input.combinedData.tramStopNames[i]} :")
//        println(" Passengers arrived : ${input.combinedData.passengerArrivalAmounts[i].sum()}")
//        println(" Passengers arrived : ${input.combinedData.passengerArrivalAmounts[i].toList()}")
//        println(" Passengers exited  : ${input.combinedData.passengerExitingAmounts[i].sum()}")
        println(" Passengers exited  : ${input.combinedData.passengerExitingAmounts[i].toList()}")
        println(" Percentage exited  : ${input.exitPercentages[i].toList()}")
//        println()
    }

}
