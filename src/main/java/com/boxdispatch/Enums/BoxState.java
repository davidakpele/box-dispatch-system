package com.boxdispatch.Enums;

/**
 * Represents the lifecycle states of a Box.
 * Valid transitions: IDLE → LOADING → LOADED → DELIVERING → DELIVERED → RETURNING → IDLE
 */
public enum BoxState {
    IDLE,
    LOADING,
    LOADED,
    DELIVERING,
    DELIVERED,
    RETURNING;
 
    /**
     * Determines whether a transition from this state to the target state is valid.
     */
    public boolean canTransitionTo(BoxState target) {
        return switch (this) {
            case IDLE     -> target == LOADING;
            case LOADING  -> target == LOADED || target == IDLE;
            case LOADED   -> target == DELIVERING || target == IDLE;
            case DELIVERING -> target == DELIVERED;
            case DELIVERED  -> target == RETURNING;
            case RETURNING  -> target == IDLE;
        };
    }
 
    /**
     * Returns true if this state allows items to be loaded.
     */
    public boolean isLoadable() {
        return this == IDLE || this == LOADING;
    }
}
 