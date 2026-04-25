package com.boxdispatch.Enums;

public enum BoxState {
    IDLE,
    LOADING,
    LOADED,
    DELIVERING,
    DELIVERED,
    RETURNING;

    public boolean canTransitionTo(BoxState target) {
        return switch (this) {
            case IDLE       -> target == LOADING;
            case LOADING    -> target == LOADED || target == IDLE;
            case LOADED     -> target == DELIVERING || target == IDLE;
            case DELIVERING -> target == DELIVERED;
            case DELIVERED  -> target == RETURNING;
            case RETURNING  -> target == IDLE;
        };
    }

    public boolean isLoadable() {
        return this == IDLE || this == LOADING;
    }

    public boolean isAvailableForLoading() {
        return this == IDLE || this == LOADING;
    }
}