/* $HeadURL$ */
package com.untangle.uvm.snmp;

/**
 * Interface for the singleton which
 * controls Snmp functionality.
 */
public interface SnmpManager
{

    SnmpSettings getSnmpSettings();

    void setSnmpSettings(SnmpSettings settings);

}
