package simulation.interfaces

import simulation.logic.StateLogic

interface EventHandler {

    /**
     * @param event The Event that is currently scheduled to occur
     * @param logic The StateLogic required to schedule a new Event
     */
    fun handleEvent(event: Event, logic: StateLogic)

}