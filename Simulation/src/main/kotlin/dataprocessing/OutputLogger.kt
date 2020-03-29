package dataprocessing

import org.apache.commons.csv.CSVFormat
import org.apache.commons.csv.CSVPrinter
import simulation.constants.Constants
import simulation.logging.LoggingData
import simulation.logic.StateLogic
import simulation.logic.models.TramStop
import java.io.*
import java.nio.file.Files
import java.nio.file.Paths
import kotlin.math.roundToInt

class OutputLogger{
    companion object {
        fun writeData(stateLogic: StateLogic) {

            var csvHeader = StringBuilder("q,schedule,passenger_entered,passenger_exited,trams_too_late_cs,trams_too_late_pr, " +
                    "%trams_late_PR>1,%trams_lateCS>1,trams_too_early_cs,trams_too_early_pr,AVG_passenger_waiting_time,peak_tram_freq,offpeak_tram_freq")
            val q = Constants.endPointDwell
            val passengerEntered = LoggingData.passengersEntered
            val passengerExited = LoggingData.passengersExited
            val tramTooLateCS = LoggingData.tramTooLate[stateLogic.state.tramStops[8]]!!.size
            val tramTooEarlyCS = LoggingData.tramTooEarly[stateLogic.state.tramStops[8]]!!.size
            val peakTramFrequency = Constants.peakTramFrequency
            val offPeakTramFrequency = Constants.offPeakTramFrequency
            val tramTooLatePR = LoggingData.tramTooLate[stateLogic.state.tramStops[0]]!!.size
            val tramTooEarlyPR = LoggingData.tramTooEarly[stateLogic.state.tramStops[0]]!!.size
            val scaling = Constants.passengerScaler

            // TODO: Simulation still stops early sometimes cause no events in queue. Only take successful ones



            var pcounter = 0
            var accumulator = 0.0
            LoggingData.passengerWaitingTimesPerTramStop.forEach{pair ->
                accumulator += pair.value.sum()
                pcounter += pair.value.size
            }
            val averagePassengerWaitingTime = accumulator / pcounter

            val averageTramQueueTimes: MutableMap<TramStop, Int> = mutableMapOf()

            stateLogic.state.tramStops.forEach{
                if(LoggingData.tramQueueTimes[it]!!.isNotEmpty()){
                    averageTramQueueTimes[it] = LoggingData.tramQueueTimes[it]!!.average().toInt()
                }else{
                    averageTramQueueTimes[it] = 0
                }
            }

            val maxTramQueueTimes: MutableMap<TramStop, Int?> = mutableMapOf()


            stateLogic.state.tramStops.forEach{
                if(LoggingData.tramQueueTimes[it]!!.isNotEmpty()){
                    maxTramQueueTimes[it] = LoggingData.tramQueueTimes[it]!!.max()
                }else{
                    maxTramQueueTimes[it] = 0
                }
            }

            // Get the size of values < 60 and divide by total amount
            val tramsTooLatePR = LoggingData.tramTooLate[stateLogic.state.tramStops[0]]

            var percentageTramWaitingMoreThan1MinutePR  = 0.0
            var counter = 0
            if(tramsTooLatePR!!.isNotEmpty()) {
                tramsTooLatePR.forEach {
                    if (it > 60) counter++
                }
                percentageTramWaitingMoreThan1MinutePR = counter.toDouble() / LoggingData.departuresFromPR.toDouble()
            }
            val tramsTooLateCS = LoggingData.tramTooLate[stateLogic.state.tramStops[8]]

            var percentageTramWaitingMoreThan1MinuteCS = 0.0
            counter = 0
            if(tramsTooLateCS!!.isNotEmpty()) {
                tramsTooLateCS.forEach {
                    if (it > 60) counter++
                }
                percentageTramWaitingMoreThan1MinuteCS = counter.toDouble() / LoggingData.departuresFromCS.toDouble()
            }
            for (time in averageTramQueueTimes) {
                csvHeader.append(",average_tramqueue_time_" + time.key.name)
            }

            for(time in maxTramQueueTimes){
                csvHeader.append(",max_tramqueue_time_" + time.key.name)
            }
            // Remove last ,
            csvHeader.replace(csvHeader.length - 1, csvHeader.length - 1, "")

            val output = OutputData(
                    q = q,
                    schedule = 1,
                    passengerEntered = passengerEntered,
                    passengerExited = passengerExited,
                    tramsTooLateCS = tramTooLateCS,
                    tramsTooLatePR = tramTooLatePR,
                    percTramTooLatePR = percentageTramWaitingMoreThan1MinutePR * 100,
                    percTramTooLateCS = percentageTramWaitingMoreThan1MinuteCS * 100,
                    tramTooEarlyCS = tramTooEarlyCS,
                    tramTooEarlyPR = tramTooEarlyPR,
                    avgPassengerWaitingTime = averagePassengerWaitingTime,
                    peakTramFrequency = peakTramFrequency,
                    offPeakTramFrequency = offPeakTramFrequency
            )
            val outputName = "src/main/kotlin/output/simulation-output-scaling=${scaling}-peakfreq=${peakTramFrequency/60}-offPeakFreq=${offPeakTramFrequency/60}-q=${q}.csv" //Constants.passengerScaler // Normal runs
//            val outputName = "src/main/kotlin/output/simulation-output-lanes=${Constants.endPointLanes}.csv" //Constants.passengerScaler // Endpoint lanes runs
//            val outputName = "src/main/kotlin/output/simulation-output-validationfile=${Constants.validationFile.substring(Constants.validationFile.length-6, Constants.validationFile.length-4)}.csv" // Validation runs
            val file = Paths.get(outputName)
            // Only apply header if the file does not exist yet
            if(Files.exists(file)){
                csvHeader.setLength(0)
            }
            val fileWriter = FileWriter(outputName, true)

            try {
                fileWriter.append(csvHeader)
                fileWriter.append('\n')

                fileWriter.append(output.toString())
                for (time in averageTramQueueTimes) {
                
                    fileWriter.append(',' + time.value.toString())

                }
                for(time in maxTramQueueTimes){
                    if(time.value != null) {
                        fileWriter.append(',' + time.value.toString())
                    }else{
                        fileWriter.append(",0" )

                    }
                }
                //println("Write successfull!")
            } catch (e: Exception) {
                println("Csv error")
                e.printStackTrace()
            } finally {
                try {
                    fileWriter.flush()
                    fileWriter.close()
                } catch (e: IOException) {
                    e.printStackTrace()
                }
            }
        }

        fun writeTramTooLateData(){
            val csvHeader : MutableList<String> = ArrayList()
            val tramTooLate = LoggingData.tramTooLate

            for (time in tramTooLate) {
                csvHeader.add(time.key.name)
            }

            val file = Paths.get("src/main/kotlin/output/tram-punctuality.csv")
            // Only apply header if the file does not exist yet
            if(Files.exists(file)){
                csvHeader.clear()
            }
            val fileWriter = FileWriter("src/main/kotlin/output/tram-punctuality.csv", true)
            val csvWriter = CSVPrinter(fileWriter, CSVFormat.DEFAULT)

            try {
                for(pair in tramTooLate){
                    csvWriter.printRecord(pair.key.toString())
                    for(time in pair.value) {
                        csvWriter.print( time.toString())
                    }
                    csvWriter.println()
                }
                //println("Write successfull!")
            } catch (e: Exception) {
                println("Csv error")
                e.printStackTrace()
            } finally {
                try {
                    fileWriter.flush()
                    fileWriter.close()
                } catch (e: IOException) {
                    e.printStackTrace()
                }
            }
        }
    }

}