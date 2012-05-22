/**
 * $HeadURL$
 */
package com.untangle.uvm;

/**
 * Interface for the singleton which
 * controls Snmp functionality.
 */
public interface SnmpManager
{

    SnmpSettings getSnmpSettings();

    void setSnmpSettings(SnmpSettings settings);

}
