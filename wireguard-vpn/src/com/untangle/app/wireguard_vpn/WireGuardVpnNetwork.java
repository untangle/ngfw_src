/**
 * $Id:
 */

package com.untangle.app.wireguard_vpn;

import java.io.Serializable;
import org.json.JSONObject;
import org.json.JSONString;
import com.untangle.uvm.app.IPMaskedAddress;
import com.untangle.uvm.util.ValidSerializable;

/**
 * This class is used when storing a list of IPMaskedAddress objects
 * that will be managed using a UI grid control.
 */

@SuppressWarnings("serial")
@ValidSerializable
public class WireGuardVpnNetwork implements JSONString, Serializable
{
    private int id;
    private IPMaskedAddress address;

    public WireGuardVpnNetwork()
    {
        this.address = new IPMaskedAddress("0.0.0.0/0");
    }
    public WireGuardVpnNetwork(IPMaskedAddress address)
    {
        this.address = address;
    }

    public int getId()
    {
        return (id);
    }

    public String getAddress()
    {
        return (address.toString());
    }
    
    public IPMaskedAddress getMaskedAddress()
    {
        return address;
    }

    public void setId(int id)
    {
        this.id = id;
    }

    public void setAddress(String address)
    {
        this.address = new IPMaskedAddress(address);
    }

    public void setAddress(IPMaskedAddress address)
    {
        this.address = address;
    }

    public String toJSONString()
    {
        JSONObject jO = new JSONObject(this);
        return jO.toString();
    }
}
