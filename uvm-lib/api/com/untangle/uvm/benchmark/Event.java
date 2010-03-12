package com.untangle.uvm.benchmark;

/**
 * A list of events that are tracked by the benchmark manager
 * @author rbscott
 *
 */
public enum Event {
    SETUP_TIME(0),
    PACKET_READ(1),
    PACKET_WRITE(2),
    MAX_EVENT(3);
    
    private final int key;
    Event(int key) {
        this.key = key;
    }
    
    public int getKey()
    {
        return this.key;
    }
}
