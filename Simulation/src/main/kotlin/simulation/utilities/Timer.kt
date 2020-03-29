package simulation.utilities

class Timer {

    private val startTime: Long = System.currentTimeMillis()

    /**
     * Returns a Long with how many ms ago this Timer has been started
     */
    private fun elapsed(): Long {
        return System.currentTimeMillis() - startTime
    }

    /**
     * Prints how many ms ago this Timer has been started
     */
    override fun toString(): String {
        return elapsed().toString() + " ms"
    }

    /**
     * Converts the seconds of the simulation to a time of day
     */
    companion object {
        fun convertSimulationTime(simulationTime: Int): String {
//            val days = simulationTime / 86400
            val hours = simulationTime % 86400 / 3600
            val minutes = simulationTime % 86400 % 3600 / 60
            val seconds = simulationTime % 86400 % 3600 % 60

            return String.format("%02d", hours) + ":" + String.format("%02d", minutes) + ":" + String.format("%02d", seconds)
        }
    }
}