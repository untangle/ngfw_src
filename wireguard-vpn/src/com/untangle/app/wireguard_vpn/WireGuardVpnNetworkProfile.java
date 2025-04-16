/**
* $Id:
*/

package com.untangle.app.wireguard_vpn;

import com.untangle.uvm.app.IPMaskedAddress;
import org.json.JSONObject;
import org.json.JSONString;

import java.io.Serializable;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * This class is used to store the subnets in different local network profiles
 * It will be managed using a UI grid control.
 */
@SuppressWarnings("serial")
public class WireGuardVpnNetworkProfile implements JSONString, Serializable {

    private String profileName;
    private List<WireGuardVpnNetwork> subnets;
    private String subnetsAsString;

    public String getProfileName() { return profileName; }
    public void setProfileName(String profileName) { this.profileName = profileName; }

    public List<WireGuardVpnNetwork> getSubnets() { return subnets; }
    public void setSubnets(List<WireGuardVpnNetwork> subnets) { this.subnets = subnets; }
    public void setSubnets(String subnetsAsString) {
        if (subnetsAsString == null || subnetsAsString.trim().isEmpty()) {
            this.subnets = new LinkedList<>();
            return;
        }
        this.subnets = Arrays.stream(subnetsAsString.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .map(addressStr -> {
                    WireGuardVpnNetwork net = new WireGuardVpnNetwork(new IPMaskedAddress(addressStr));
                    return net;
                })
                .collect(Collectors.toList());
    }

    public void setSubnetsAsString(String subnetsAsString) { this.subnetsAsString = subnetsAsString; }
    public String getSubnetsAsString() {
        if (subnetsAsString != null) return subnetsAsString;
        if (subnets == null) {
            return "";
        }
        return subnets.stream()
                .map(wg -> wg.getAddress().toString())
                .collect(Collectors.joining(","));
    }

    @Override
    public String toJSONString() {
        JSONObject jO = new JSONObject(this);
        return jO.toString();
    }

}
