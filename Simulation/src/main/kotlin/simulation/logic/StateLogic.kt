package simulation.logic

import simulation.constants.Distributions
import simulation.constants.DrivingSchedule
import simulation.interfaces.Event
import simulation.logic.models.Tram
import simulation.logic.models.TramStop
import simulation.utilities.QueueSorter
import java.util.PriorityQueue
import java.util.LinkedList


class StateLogic {

    class State(var queue: PriorityQueue<Event>) {
        var time = 0
//        var isPeak = false
        var tramStops: List<TramStop> = LinkedList() // A List of all TramStops
        var trams: MutableList<Tram> = ArrayList() // A List of all currently driving Trams
        var drivingSchedule = DrivingSchedule() // The driving schedule
        var distributions = Distributions()  // The distributions
    }
    val state = State(PriorityQueue(QueueSorter()))

    /**
     * Add all TramStops to the State
     */
    fun setTramStops(allTramStops: List<TramStop>) {
        state.tramStops = allTramStops
    }

    /**
     * Add all TramStops to the State
     */
    fun setTrams(allTrams: MutableList<Tram>) {
        state.trams = allTrams
    }

    fun addTram(tram: Tram){
        state.trams.add(tram)
    }
    /**
     * Adds a new Test Event to the queue
     */
    fun addEvent(event: Event) {
        state.queue.add(event)
    }

    /**
     * Gets the next Event
     */
    fun nextEvent() : Event {
        return state.queue.poll()
    }

    /**
     * Checks if there is an Event in the queue
     */
    fun isQueueNotEmpty() : Boolean {
        return state.queue.isNotEmpty()
    }

    /**
     *
     */
    fun setTime(time: Int) {
        state.time = time
    }

    /**
     *
     */
    fun getTime() : Int {
        return state.time
    }

    /**
     * @param tramStop The current TramStop
     * @return The next TramStop
     */
    fun getNextTramStop(tramStop: TramStop) : TramStop {
        return if (tramStop == state.tramStops.last()) {
            state.tramStops.first()
        } else {
            state.tramStops[tramStop.number]
        }
    }

    /**
     * @param tramStop The current TramStop
     * @return The previous TramStop
     */
    fun getPreviousTramStop(tramStop: TramStop) : TramStop {
        return if (tramStop == state.tramStops.first()) {
            state.tramStops.last()
        } else {
            state.tramStops[tramStop.number-2]
        }
    }

    fun removeTram(tram: Tram){
        state.trams.remove(tram)
    }

    override fun toString(): String {
        return state.queue.toString()
    }
}