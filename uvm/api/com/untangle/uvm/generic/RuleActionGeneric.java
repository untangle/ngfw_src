/**
 * $Id$
 */
package com.untangle.uvm.generic;

import java.io.Serializable;
import java.net.InetAddress;

import org.json.JSONObject;
import org.json.JSONString;

/**
 * This in the Generic Rule Class
 */
@SuppressWarnings("serial")
public class RuleActionGeneric implements JSONString, Serializable {

    // Common To All Rules
    private Type type;

    // Required for Port Forward Rules
    private InetAddress dnat_address;
    private String dnat_port;

    // Required for NAT Rules
    private InetAddress snat_address;

    // Common To All Rules
    public Type getType() { return type; }
    public void setType(Type type) { this.type = type; }

    // Required for Port Forward Rules
    public InetAddress getDnat_address() { return dnat_address; }
    public void setDnat_address(InetAddress dnat_address) { this.dnat_address = dnat_address; }
    public String getDnat_port() { return dnat_port; }
    public void setDnat_port(String dnat_port) { this.dnat_port = dnat_port; }

    // Required for NAT Rules
    public InetAddress getSnat_address() { return snat_address; }
    public void setSnat_address(InetAddress snat_address) { this.snat_address = snat_address; }

    public enum Type { DNAT, SNAT, MASQUERADE }

    public String toJSONString() {
        JSONObject jO = new JSONObject(this);
        return jO.toString();
    }
}
