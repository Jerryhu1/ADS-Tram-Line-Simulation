package simulation.logic.models

class Tram(val number: Int, val capacity: Int, var passengerAmount: Int, var endpointDepartureTime: Int) {

    lateinit var nextTram: Tram     // The next Tram on the track
    lateinit var previousTram: Tram // The previous Tram on the track
    var currentTramStop : TramStop? = null

    /**
	 * @return The amount of new passengers that will fit in this Tram
	 */
    fun getAvailableCapacity(): Int {
        return capacity - passengerAmount
    }

    /**
	 * @return A String identifying this Tram
	 */
    override fun toString(): String {
        return "Tram $number"
    }

}