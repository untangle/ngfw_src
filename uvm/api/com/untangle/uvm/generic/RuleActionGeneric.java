/**
 * $Id$
 */
package com.untangle.uvm.generic;

import java.io.Serializable;
import java.net.InetAddress;

import org.json.JSONObject;
import org.json.JSONString;

/**
 * This in the Generic Rule Action Class
 * Used for vue model transformations
 */
@SuppressWarnings("serial")
public class RuleActionGeneric implements JSONString, Serializable {

    /**
     * DNAT - Required for Port Forward Rules
     * SNAT, MASQUERADE - Required for NAT Rules
     * BYPASS, PROCESS - Required for Bypass Rules
     * ACCEPT, REJECT - Required for Filter, Access and UPnP Rules
     * PRIORITY - Required for QoS Rules
     * SCAN, PASS - Required for Shield Rules
     * TARGET_POLICY - Required for Policy Manager
     */
    public enum Type { DNAT, SNAT, MASQUERADE, BYPASS, PROCESS, ACCEPT, REJECT, SET_PRIORITY, SCAN, PASS, TARGET_POLICY }
    // Common To All Rules
    private Type type;

    public Type getType() { return type; }
    public void setType(Type type) { this.type = type; }

    // Required for Port Forward Rules
    private InetAddress dnat_address;
    private String dnat_port;

    public InetAddress getDnat_address() { return dnat_address; }
    public void setDnat_address(InetAddress dnat_address) { this.dnat_address = dnat_address; }
    public String getDnat_port() { return dnat_port; }
    public void setDnat_port(String dnat_port) { this.dnat_port = dnat_port; }

    // Required for NAT Rules
    private InetAddress snat_address;

    public InetAddress getSnat_address() { return snat_address; }
    public void setSnat_address(InetAddress snat_address) { this.snat_address = snat_address; }

    // Required for QoS Rules
    private int priority;

    public int getPriority() { return priority; }
    public void setPriority(int priority) { this.priority = priority; }

    // Required for Policy Manager
    private Integer targetPolicy;

    public Integer getTargetPolicy() { return targetPolicy; }
    public void setTargetPolicy(Integer targetPolicy) { this.targetPolicy = targetPolicy; }


    public String toJSONString() {
        JSONObject jO = new JSONObject(this);
        return jO.toString();
    }
}
