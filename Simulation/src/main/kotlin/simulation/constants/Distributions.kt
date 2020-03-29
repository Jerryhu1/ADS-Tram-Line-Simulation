package simulation.constants

import dataprocessing.PassengerInput
import dataprocessing.ValidationInput
import org.apache.commons.math3.distribution.*
import org.apache.commons.math3.random.MersenneTwister
import simulation.logic.models.TramStop
import kotlin.math.roundToInt

class Distributions {

    class PassengerInfo(var arrivalAmount: Int, var avgWaitingTime: Double)

    private val validationInput : ValidationInput
    private val passengerInput : PassengerInput

    init {
        if (Constants.validationFile != "") {
            validationInput = ValidationInput(true)
            passengerInput = PassengerInput(false)
        } else {
            validationInput = ValidationInput(false)
            passengerInput = PassengerInput(true)
        }

    }


    // A single MersenneTwister is used to initiate the Gamma and LogNormal Distributions
    // The Poisson is created once per simulation, so the seed is far enough apart and doesn't need this
    private val random = MersenneTwister()

    // A Map containing the List of passenger arrival Distributions for each TramStop, for each interval of 15 minutes
    val passengerArrivalDistributions: MutableMap<TramStop, List<PoissonDistribution>> = HashMap()

    // A Map containing the List of passenger exiting Distributions for each TramStop, for each interval of 15 minutes
    val passengerExitingPercentages: MutableMap<TramStop, List<Double>> = HashMap()

    // A Map containing the List of the driving time Distributions for each TramStop
    val drivingTimeDistributions: MutableMap<TramStop, LogNormalDistribution> = HashMap()

    /**
     * Sets the appropriate Distribution for driving times between TramStops
     */
    fun setPassengerArrivalDistribution(allTramStops: List<TramStop>) {

        // If Constants.validationFile is null, we're running a normal simulation with busline 12 data
        if (Constants.validationFile == "") {
            allTramStops.forEachIndexed { index, tramStop ->
                val distributionsList = mutableListOf<PoissonDistribution>()
                passengerInput.combinedData.passengerArrivalAmounts[index].forEach { meanArrivedPassengers ->
                    if (meanArrivedPassengers > 0) {
                        distributionsList.add(PoissonDistribution(meanArrivedPassengers))
                    } else {
                        distributionsList.add(PoissonDistribution(0.00000000001)) // Strictly bigger than 0, but small enough to ensure no new passengers
                    }
                }
                passengerArrivalDistributions[tramStop] = distributionsList
            }
        }

        // If Constants.validationFile is not null, we're using a validation simulation with custom input data
        else {
            allTramStops.forEachIndexed { index, tramStop ->
                val distributionsList = mutableListOf<PoissonDistribution>()
                validationInput.combinedData.passengerArrivalAmounts[index].forEach { meanArrivedPassengers ->
                    if (meanArrivedPassengers > 0) {
                        distributionsList.add(PoissonDistribution(meanArrivedPassengers))
                    } else {
                        distributionsList.add(PoissonDistribution(0.00000000001)) // Strictly bigger than 0, but small enough to ensure no new passengers
                    }
                }
                passengerArrivalDistributions[tramStop] = distributionsList
            }
        }
    }

    /**
     * Returns the amount of passengers that arrives at the given TramStop for a given time interval
     * If the interval spans multiple 15 minute timeslots, this is taken into account
     */
    fun getPassengerArrivalInfo(tramStop: TramStop, startTime: Int, stopTime: Int) : List<PassengerInfo> {
//            if (startTime != 0 && stopTime - startTime >= 15 * 60) {
//                throw error("Distributions.getPassengerArrivalInfo: More than 15 between trams! Previous departure time: $startTime, current arrival time: $stopTime")
//                System.err.println("Distributions.getPassengerArrivalInfo: More than 15 between trams! Previous departure time: $startTime, current arrival time: $stopTime")
//            }

        val startTimeslot = startTime / (60 * 15) // In which 15 minute timeslot does this Distribution fall
        val stopTimeslot = stopTime / (60 * 15) // In which 15 minute timeslot does this Distribution fall

        var currentTime = startTime
        var currentTimeslot = currentTime / (60 * 15) // In which 15 minute timeslot does this Distribution fall
        val passengerInfos = mutableListOf<PassengerInfo>()

        // Get the passenger numbers and waiting times for all required timeslots
        repeat(stopTimeslot - startTimeslot + 1) {
            if (currentTimeslot < passengerArrivalDistributions[tramStop]!!.size) {
                if (currentTimeslot != stopTimeslot) {
                    val endOfTimeslotTime = (currentTimeslot + 1) * (60 * 15)
                    val secondsInTimeslot = (endOfTimeslotTime - currentTime).toDouble()
                    val arrivalAmount = (passengerArrivalDistributions[tramStop]!![currentTimeslot].sample() * (secondsInTimeslot / (60.0 * 15.0))).roundToInt()
                    passengerInfos.add(PassengerInfo(arrivalAmount, ((endOfTimeslotTime - currentTime) / 2.0) + (stopTime - endOfTimeslotTime)))

                    // Increment the currentTime to the start of the next timeslot
                    currentTime = (currentTimeslot + 1) * (60 * 15)
                    currentTimeslot++
                } else {
                    // currentTime is in the same timeslot as the stopTime. This is the final run of this algorithm.
                    if (currentTimeslot < passengerArrivalDistributions[tramStop]!!.size) {
                        val secondsInTimeslot = (stopTime - currentTime).toDouble()
                        val arrivalAmount = (passengerArrivalDistributions[tramStop]!![currentTimeslot].sample() * (secondsInTimeslot / (60.0 * 15.0))).roundToInt()
                        passengerInfos.add(PassengerInfo(arrivalAmount, (stopTime - currentTime) / 2.0))
                    }
                }
            }
        }

        return passengerInfos
    }

    /**
     * Sets the appropriate Distribution for driving times between TramStops
     */
    fun setPassengerExitingDistributions(allTramStops: List<TramStop>) {

        // If Constants.validationFile is null, we're running a normal simulation with busline 12 data
        if (Constants.validationFile == "") {
            allTramStops.forEachIndexed { index, tramStop ->
                val percentageList = mutableListOf<Double>()

                passengerInput.exitPercentages[index].forEach { percentageExitedPassengers ->
                    if (percentageExitedPassengers > 0) {
                        percentageList.add(percentageExitedPassengers)
                    } else {
                        percentageList.add(0.0)
                    }
                }
                passengerExitingPercentages[tramStop] = percentageList
            }
        }

        // If Constants.validationFile is not null, we're using a validation simulation with custom input data
        else {
            allTramStops.forEachIndexed { index, tramStop ->
                val percentageList = mutableListOf<Double>()

                validationInput.exitPercentages[index].forEach { percentageExitedPassengers ->
                    if (percentageExitedPassengers > 0) {
                        percentageList.add(percentageExitedPassengers)
                    } else {
                        percentageList.add(0.0)
                    }
                }
                passengerExitingPercentages[tramStop] = percentageList
            }
        }
    }

    /**
     * Returns the Distribution for passenger exit percentages at the given TramStop for a given 15 minute timeslot
     */
    fun getPassengerExitingDistribution(tramStop: TramStop, time: Int, currentPassengers: Int) : BinomialDistribution {
        val timeslot= time / (60 * 15) // In which 15 minute timeslot does this Distribution fall
        var currentPassengersClean = currentPassengers
        if (currentPassengers < 0) {
            currentPassengersClean = 0
        }
        return if (timeslot >= passengerExitingPercentages[tramStop]!!.size) {
            BinomialDistribution(random, currentPassengersClean, 1.0 / 8.0) // If we're driving outside of regular times, just take a evenly distributed exit amount
        } else {
            BinomialDistribution(random, currentPassengersClean, passengerExitingPercentages[tramStop]!![timeslot])
        }
    }

    /**
     * Returns a Gamma distribution, given the mean and shape parameter k
     * Calculating the scale parameter is done by dividing the mean by the shape parameter
     *
     * This is used to calculate the dwell times
     */
    fun getGammaDistribution(mean: Double, shape: Double): GammaDistribution {
        val scale = mean / shape
        return GammaDistribution(random, shape, scale)
    }

    /**
     * Sets the appropriate Distribution for driving times between TramStops
     */
    fun setDrivingTimeDistributions(allTramStops: List<TramStop>) {
        // The parameters per tramstop, first value is the logsd, second is the logmean
        val parameters = listOf(
                Pair(0.13, 4.69),   // WKZ
                Pair(0.12, 4.35),   // UMC
                Pair(0.12, 4.40),   // Heidel
                Pair(0.11, 4.09),   // Padua
                Pair(0.13, 4.60),   // Kromme
                Pair(0.11, 4.07),   // Galg
                Pair(0.17, 5.49),   // Vaartsch
                Pair(0.13, 4.72),   // P+R
                Pair(0.14, 4.89),   // Vaarsch
                Pair(0.17, 5.48),   // Galg
                Pair(0.17, 4.07),   // Kromme
                Pair(0.11, 4.61),   // Padua
                Pair(0.13, 4.09),   // Heidel
                Pair(0.11, 4.45),   // UMC
                Pair(0.12, 4.35),   // WKZ
                Pair(0.11, 4.72)   // CS
        )
        for ((counter, tramStop) in allTramStops.withIndex()) {
            drivingTimeDistributions[tramStop] = LogNormalDistribution(random, parameters[counter].second, parameters[counter].first)
        }
    }

    /**
     * Clears all distributions
     */
    fun clearDistributions() {
        drivingTimeDistributions.clear()
        passengerArrivalDistributions.clear()
        passengerExitingPercentages.clear()
    }
}