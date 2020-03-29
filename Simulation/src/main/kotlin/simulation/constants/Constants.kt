package simulation.constants

object Constants {

    const val MINUTE = 60
    const val QUARTER = 15 * 60
    const val HOUR = 60 * 60

    // Start - Simulation constants
    const val tramStopAvailableTime = 40                     // A TramStop becomes available 40 seconds after a Tram departs
    const val tramCapacity = 420                             // Capacity of a Tram
    const val switchTime = 1 * MINUTE                   // A switch becomes available 60 seconds after a Tram starts passing through
    const val startTime = 6 * HOUR                      // The start time of the simulation in seconds, 06:00 in the morning
    var endPointDwell = 3 * MINUTE                      // The minimum turn-around time q at the endstations
    var passengerScaler = 1.0                                // In- or decrease the number of arriving passengers
    var endPointLanes = 2                                    // The amount of endpoint lanes
//     End - Simulation constants

    // Start - Variables used to create the schedule
    const val peakTime = 1 * HOUR                            // 7:00, the time peaktime starts. Counting from 6:00
    const val offPeakTime = 13 * HOUR                        // 19:00, the time offpeaktime starts. Counting from 6:00
    const val lastTramScheduleTime = (15.5 * HOUR).toInt()   // This simulation runs for 16 hours, schedule the last tram earlier

    const val maxRunTime = 16 * HOUR                         // This simulation runs for 16 hours
    var peakTramFrequency = 4 * MINUTE                 // Time between scheduled trams during peak hours
    var offPeakTramFrequency = 12 * MINUTE             // Time between scheduled trams during offpeak hours

    const val tramStartBufferTime = 90                            // The amount of seconds a new Tram arrives at P+R before its scheduled departure
    const val drivingTime = 17 * MINUTE                      // one-way driving time of 17 minutes
    var scheduleEndPointDwell = endPointDwell + (2 * MINUTE) // The scheduled turn-around time q at the endstations
    var scheduleRoundtripTime = 2 * (drivingTime + scheduleEndPointDwell)  // 1 trip takes 17 minutes and turning around 5 minutes, the return trip the same. So 44 minutes total
//     End - Variables used to create the schedule

    // Start - Dwell time                                     // Mean d = 12.5 + 0.22 * p_in + 0.13 * p_out
    const val p_const = 12.5                                  // The constant amount of seconds in the dwell time mean formula
    const val p_in = 0.22                                     // The amount of seconds a passenger entering adds to the dwell time
    const val p_out = 0.13                                    // The amount of seconds a passenger exiting adds to the dwell time
    const val dwellMin = 0.8                                  // The fraction of the mean dwell time that is the minimum dwell time
    const val k = 2.0                                         // The shape parameter for the GammaDistribution used for the dwell times
    // End - Dwell time

    const val endTime = 21.5 * HOUR                  // The stop time of the simulation in seconds, 21:30 in the evening
    const val maxRunBlocks = maxRunTime / QUARTER        // The amount of 15 minute blocks this simulation will run
    const val doorJamProb = 0.01                             // The probability of a door jam
//    const val q = 3 * 60                                  // The minimum turn-around time q at the endstations
//    const val tramsInPeak = 19                            // # Of trams driving in peak hours
//    const val tramsInOffPeak = 4                          // # Of trams driving in peak hours
//    const val minOffPeakTramFrequency = 900               // Min frequency of trams during peak hours
//    const val maxOffPeakTramFrequency = 727               // Max frequency of trams during peak hours


    // Turn the different log reportings on or off
    const val logPassengerNumbers = true
    const val logPassengerWaitingTime = true
    const val logTramOnTime = true
    const val logTramQueueTime = true
    const val logDrivingTime = false
    const val logTramAmounts = true
    const val logTramRoundTrips = true
    const val logDistributionNumbers = false
    const val printDriveTimes = false

    // If validationFile is not null, a validation file will be used for passenger input data
    var validationFile = ""
//    const val validationFile = "../Datasets/Validation/input-data-passengers-01.csv"
//    const val validationFile = "../Datasets/Validation/input-data-passengers-015.csv"
//    const val validationFile = "../Datasets/Validation/input-data-passengers-02.csv"
//    const val validationFile = "../Datasets/Validation/input-data-passengers-025.csv"
//    const val validationFile = "../Datasets/Validation/input-data-passengers-03.csv"
//    const val validationFile = "../Datasets/Validation/input-data-passengers-04.csv"
//    const val validationFile = "../Datasets/Validation/input-data-passengers-06.csv"

}