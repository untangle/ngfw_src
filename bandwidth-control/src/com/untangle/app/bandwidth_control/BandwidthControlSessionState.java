/**
 * $Id: BandwidthControlSessionState.java,v 1.00 2018/03/20 20:08:04 dmorris Exp $
 */
package com.untangle.app.bandwidth_control;

/**
 * This class stores the Bandwidth Control session state for each session
 * It is attached to each session and used for internal accounting
 * Note: this should live in impl but is currently here because BandwidthControlRuleAction needs access to it
 */
public class BandwidthControlSessionState
{
    /**
     * Instantiate a BandwidthControlSessionState
     */
    public BandwidthControlSessionState()
    {
        this.chunkCount = 0;
        this.lastPriority = -1;
    }

    /**
     * This is just a counter of the number of data chunks seen in this network
     */
    public int chunkCount;

    /**
     * This stores the last known priority set by BWC for this session
     */
    public int lastPriority;
    
}
