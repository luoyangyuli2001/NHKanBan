package com.lyyl.nhkanban.common.task;

public enum TaskStatus {

    OPEN,
    CLAIMED,
    SUBMITTED,
    COMPLETED,
    EXPIRED,
    CANCELLED;

    public boolean isTerminal() {
        return this == COMPLETED || this == CANCELLED || this == EXPIRED;
    }
}
