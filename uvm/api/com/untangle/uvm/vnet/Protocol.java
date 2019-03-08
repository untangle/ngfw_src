/**
 * $Id$
 */
package com.untangle.uvm.vnet;

/**
 * The <code>Protocol</code> interface represents an IP protocol.
 */
public enum Protocol
{
    TCP("TCP", 6),
    UDP("UDP", 17),
    ICMP("ICMP", 1);

    private final String name;
    private final int id;

    /**
     * Get the protocol instance for a given name
     * names: TCP, UDP, or ICMP
     * Anything else results in null
     * @param name
     * @return Protocol
     */
    public static Protocol getInstance(String name)
    {
        Protocol[] values = values();
        for (int i = 0; i < values.length; i++) {
            if (values[i].getName().equals(name)){
                return values[i];
            }
        }
        return null;
    }

    /**
     * Get the protocol instance for a given protocol id
     * names: 6, 17, 1
     * Anything else results in null
     * @param id - protocol id
     * @return Protocol
     */
    public static Protocol getInstance(int id)
    {
        Protocol[] values = values();
        for (int i = 0; i < values.length; i++) {
            if (values[i].getId() == id){
                return values[i];
            }
        }
        return null;
    }

    /**
     * Constructor - private. use getInstance()
     * @param name
     * @param id
     */
    private Protocol(String name, int id)
    {
        this.name = name;
        this.id   = id;
    }

    /**
     * Get the ID for a protocol
     * TCP = 6
     * UDP = 17
     * ICMP = 1
     * @return int
     */
    public int getId()
    {
        return id;
    }

    /**
     * Get the name for a protocol
     * @return string
     */
    public String getName()
    {
        return name;
    }
                                          
}
