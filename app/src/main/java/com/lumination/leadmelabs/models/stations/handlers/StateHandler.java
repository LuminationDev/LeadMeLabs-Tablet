package com.lumination.leadmelabs.models.stations.handlers;

public class StateHandler {
    //region Statuses
    public static final String AWAITING_HEADSET = "Awaiting headset connection...";
    public static final String READY = "Ready to go";
    public static final String NOT_SET = "Not set";
    public static final String NOT_RESPONDING = "Not Responding";
    public static final String IDLE_MODE = "Idle Mode";
    //endregion

    private String state;

    public String getState() {
        return this.state;
    }

    public void setState(String state) {
        this.state = state;
    }

    /**
     * Return whether the Station is currently not responding.
     * @return A boolean of if the Station is not responding.
     */
    public boolean isNotResponding() {
        return state != null && state.equals(NOT_RESPONDING);
    }

    /**
     * Determine if the Station has a State.
     * @return A boolean if the Station has a valid State.
     */
    public boolean hasState() {
        return state != null && !state.isEmpty();
    }

    /**
     * Check if the Station is currently waiting on a headset connection.
     * @return A boolean if the Station is awaiting a headset connection.
     */
    public boolean isAwaitingHeadset() {
        return state != null && state.equals(AWAITING_HEADSET);
    }

    /**
     * Check if the State is available to be set.
     * @return A boolean if the State is not the default or non-valid
     */
    public boolean isAvailable() {
        return state != null && !state.equals(NOT_SET) && !state.equals(READY);
    }

    /**
     * Check the State to determine if Idle mode can be engaged.
     * @return A boolean if Idle mode can be entered.
     */
    public boolean isAwaitingHeadsetOrReady() {
        return state != null && (state.equals(AWAITING_HEADSET) || state.equals(READY) || state.equals(IDLE_MODE));
    }
}
