package dataprocessing

class OutputData(
                 val q: Int,
                 val schedule: Int,
                 val passengerEntered: Int,
                 val passengerExited: Int,
                 val tramsTooLateCS: Int,
                 val tramsTooLatePR: Int,
                 val percTramTooLatePR : Double,
                 val percTramTooLateCS : Double,
                 val tramTooEarlyCS: Int,
                 val tramTooEarlyPR: Int,
                 val avgPassengerWaitingTime: Double,
                 val peakTramFrequency: Int,
                 val offPeakTramFrequency: Int) {

    override fun toString(): String {
        return (q.toString() + ',' + schedule.toString() + ',' + passengerEntered.toString() + ',' + passengerExited.toString()
                + ',' + tramsTooLateCS.toString() + ',' + tramsTooLatePR.toString() + ',' + percTramTooLatePR.toString()
                + ',' + percTramTooLateCS.toString() + ',' + tramTooEarlyCS.toString()
                + ',' + tramTooEarlyPR.toString() + ',' + avgPassengerWaitingTime.toString()
                + ',' + peakTramFrequency.toString() + ',' + offPeakTramFrequency.toString())
    }
}