package simulation.logic.models

import simulation.constants.Constants
import simulation.constants.Distributions
import simulation.events.TramDepartureEvent
import simulation.interfaces.Event
import simulation.interfaces.TramArrivalInterface
import java.util.*
import kotlin.collections.ArrayList

class TramStop(val number: Int = -1, val name: String = "test", var isBusy: Boolean = false, var isEndPoint: Boolean = false) {

    var passengerArrivalQueue: Queue<Distributions.PassengerInfo> = ArrayDeque<Distributions.PassengerInfo>()   // The passenger queue
    var tramQueue: Deque<Event> = ArrayDeque()           // The queue with Events with arriving or leaving Trams
    var tramsOnStop: MutableList<Tram> = ArrayList()

    var nextTramArrivalQueue: Queue<TramArrivalInterface> = ArrayDeque<TramArrivalInterface>() // The tram that will arrive next first (to prevent overtaking)
    var previousTramDeparture : TramDepartureEvent? = null
    var previousTramArrival : TramArrivalInterface? = null

    lateinit var nextTramStop: TramStop                         // The next TramStop on the track
    lateinit var previousTramStop: TramStop                     // The previous TramStop on the track

    // CS and P+R specific variables
    var switchBusy = false                                      // A boolean representing if the switch is busy
    var freeLanes = arrayListOf<Int>()              // An ArrayList representing which tracks are free
    var tramsToRemove = 0                                       // How many trams are still to be removed at P+R

    init {
        repeat(Constants.endPointLanes) {
            freeLanes.add(it)
        }
    }

    /**
	 * @return A String identifying this TramStop
	 */
    override fun toString(): String {
        return name
    }

    fun printTramQueue() : String {
        val str = StringBuilder()
        for (ev in tramQueue){
            str.append(ev.toString())
        }

        return str.toString()
    }

}