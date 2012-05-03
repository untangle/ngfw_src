/**
 * $Id: IpsStatistics.java,v 1.00 2012/05/02 20:50:43 dmorris Exp $
 */
package com.untangle.node.ips;

import java.io.Serializable;

/**
 * Statistics for the Ips node.
 */
@SuppressWarnings("serial")
public class IpsStatistics implements Serializable
{
    private int rulesLength;
    private int variablesLength;
    private int immutableVariablesLength;

    private int totalAvailable;
    private int totalBlocking;
    private int totalLogging;

    public IpsStatistics() { }

    public int getRulesLength()
    {
        return this.rulesLength;
    }

    public void setRulesLength(int rulesLength)
    {
        this.rulesLength = rulesLength;
    }

    public int getVariablesLength()
    {
        return this.variablesLength;
    }

    public void setVariablesLength(int variablesLength)
    {
        this.variablesLength = variablesLength;
    }

    public int getImmutableVariablesLength()
    {
        return this.immutableVariablesLength;
    }

    public void setImmutableVariablesLength(int immutableVariablesLength)
    {
        this.immutableVariablesLength = immutableVariablesLength;
    }

    public int getTotalAvailable()
    {
        return this.totalAvailable;
    }

    public void setTotalAvailable(int totalAvailable)
    {
        this.totalAvailable = totalAvailable;
    }

    public int getTotalBlocking()
    {
        return this.totalBlocking;
    }

    public void setTotalBlocking(int totalBlocking)
    {
        this.totalBlocking = totalBlocking;
    }

    public int getTotalLogging()
    {
        return this.totalLogging;
    }

    public void setTotalLogging(int totalLogging)
    {
        this.totalLogging = totalLogging;
    }
}
