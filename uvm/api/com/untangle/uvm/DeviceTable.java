/**
 * $Id: DeviceTable.java 42009 2015-12-29 19:15:19Z dmorris $
 */
package com.untangle.uvm;

import java.util.LinkedList;
import java.util.Hashtable;
import java.net.InetAddress;

/**
 * The Device Table is responsible for storing known information about devices
 */
public interface DeviceTable
{
    public DeviceTableEntry getDevice( String macAddress );
}

