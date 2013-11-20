/**
 * $Id$
 */
package com.untangle.uvm.vnet;

/**
 * Represents affinity for a particular side of the pipeline.
 *
 */
public class Affinity
{
    public static final Affinity CLIENT = new Affinity("client");
    public static final Affinity SERVER = new Affinity("server");

    private String affinity;

    private Affinity(String affinity)
    {
        this.affinity = affinity;
    }

    public String toString()
    {
        return affinity;
    }
}
