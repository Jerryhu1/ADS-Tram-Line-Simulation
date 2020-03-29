package simulation.utilities

import simulation.interfaces.Event
import java.util.Comparator

class QueueSorter : Comparator<Event> {

    /**
	 * Compares the start time of two Events, used by the PriorityQueue
	 */
    override fun compare(event1: Event, event2: Event): Int {
        return Integer.compare(event1.time, event2.time)
    }

}