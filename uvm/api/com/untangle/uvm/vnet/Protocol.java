/**
 * $Id$
 */
package com.untangle.uvm.vnet;


/**
 * The <code>Protocol</code> interface represents an IP protocol.
 *
 */
public enum Protocol
{
    TCP("TCP", 6),
    UDP("UDP", 17),
    ICMP("ICMP", 1);

    private final String name;
    private final int id;

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

    private Protocol(String name, int id)
    {
        this.name = name;
        this.id   = id;
    }

    public int getId()
    {
        return id;
    }

    public String getName() {
        return name;
    }
                                          
}
