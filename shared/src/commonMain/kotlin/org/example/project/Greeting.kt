package org.example.project

/**
* ## 5. Key Logic Flows (Non-obvious behavior)
* - **Local & Remote State Synchronization**: The `getBalanceInfo` method implements a complex synchronization logic to inform the user about bonuses that may have expired while they were away from the app. The sequence is:
* 1.  Fetch the current list of bonuses from the server.
* 2.  Update the main UI state with this server data.
* 3.  **After the UI is updated**, read the *previously* active bonus from local storage.
* 4.  Compare the locally saved bonus with the new list from the server. If the saved bonus is no longer present, it means it has expired or been used.
* 5.  The expired bonus is then temporarily placed in the `savedActiveBonusBalance` state property, allowing the UI to show a one-time "Your bonus... has expired" message.
* 6.  Finally, the local storage is synchronized with the new server state: the new active bonus is saved, or the storage is cleared if no bonus is active.
*
* - **Dual Loading Indicators**: The state uses two separate flags, `isLoading` and `isActivationLoading`, to differentiate between the initial screen load and the smaller, in-progress actions of activating/deactivating a bonus. This allows the UI to show appropriate feedback for each.
*/
class Greeting {
    /**
     * Fetches the initial bonus balance information and performs a sync check between
     * remote data and locally persisted active bonus state.
     * @param handleProgress If true, shows a full-screen loading indicator during the fetch.
     */
    private val platform = getPlatform()

    fun greet(): String {
        return "Hello, ${platform.name}!"
    }
}