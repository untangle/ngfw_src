/**
 * $Id: IpsecVpnTunnel.java 39842 2015-03-11 15:50:17Z mahotz $
 */

package com.untangle.app.ipsec_vpn;

import java.io.Serializable;

import org.json.JSONObject;
import org.json.JSONString;

/**
 * This class contains all settings for each configured IPsec tunnel.
 * 
 * @author mahotz
 * 
 */

@SuppressWarnings("serial")
public class IpsecVpnTunnel implements JSONString, Serializable
{
    private int id;
    private boolean active;
    private int ikeVersion = 1;
    private String conntype;
    private String description;
    private String secret;
    private String runmode;
    private String dpddelay = "30";
    private String dpdtimeout = "120";
    private boolean phase1Manual = false;
    private String phase1Cipher = "3des";
    private String phase1Hash = "md5";
    private String phase1Group = "modp2048";
    private String phase1Lifetime = "28800";
    private boolean phase2Manual = false;
    private String phase2Cipher = "3des";
    private String phase2Hash = "md5";
    private String phase2Group = "modp2048";
    private String phase2Lifetime = "3600";
    private String left;
    private String leftId;
    private String leftSubnet;
    private String leftProtoPort;
    private String leftNextHop;
    private String right;
    private String rightId;
    private String rightSubnet;
    private String rightProtoPort;
    private String rightNextHop;
    private String pingAddress;
    private int pingInterval;

    public IpsecVpnTunnel()
    {
    }

    // THIS IS FOR ECLIPSE - @formatter:off

    public int getId() { return (id); }
    public void setId(int id) { this.id = id; }

    public int getIkeVersion() { return (ikeVersion); }
    public void setIkeVersion(int ikeVersion) { this.ikeVersion = ikeVersion; }

    public boolean getActive() { return (active); }
    public void setActive(boolean active) { this.active = active; }
    
    public String getConntype() { return (conntype); }
    public void setConntype(String conntype) { this.conntype = conntype; }

    public String getDescription() { return (description); }
    public void setDescription(String description) { this.description = description; }

    public String getSecret() { return (secret); }
    public void setSecret(String secret) { this.secret = secret; }

    public String getRunmode() { return (runmode); }
    public void setRunmode(String runmode) { this.runmode = runmode; }

    public String getDpddelay() { return (dpddelay); }
    public void setDpddelay(String dpddelay) { this.dpddelay = dpddelay; }
    
    public String getDpdtimeout() { return (dpdtimeout); }
    public void setDpdtimeout(String dpdtimeout) { this.dpdtimeout = dpdtimeout; }
    
    public boolean getPhase1Manual() { return (phase1Manual); }
    public void setPhase1Manual(boolean phase1Manual) { this.phase1Manual = phase1Manual; }

    public String getPhase1Cipher() { return (phase1Cipher); }
    public void setPhase1Cipher(String phase1Cipher) { this.phase1Cipher = phase1Cipher; }
    
    public String getPhase1Hash() { return (phase1Hash); }
    public void setPhase1Hash(String phase1Hash) { this.phase1Hash = phase1Hash; }
    
    public String getPhase1Group() { return (phase1Group); }
    public void setPhase1Group(String phase1Group) { this.phase1Group = phase1Group; }
    
    public String getPhase1Lifetime() { return (phase1Lifetime); }
    public void setPhase1Lifetime(String phase1Lifetime) { this.phase1Lifetime = phase1Lifetime; }
    

    public boolean getPhase2Manual() { return (phase2Manual); }
    public void setPhase2Manual(boolean phase2Manual) { this.phase2Manual = phase2Manual; }

    public String getPhase2Cipher() { return (phase2Cipher); }
    public void setPhase2Cipher(String phase2Cipher) { this.phase2Cipher = phase2Cipher; }
    
    public String getPhase2Hash() { return (phase2Hash); }
    public void setPhase2Hash(String phase2Hash) { this.phase2Hash = phase2Hash; }
    
    public String getPhase2Group() { return (phase2Group); }
    public void setPhase2Group(String phase2Group) { this.phase2Group = phase2Group; }
    
    public String getPhase2Lifetime() { return (phase2Lifetime); }
    public void setPhase2Lifetime(String phase2Lifetime) { this.phase2Lifetime = phase2Lifetime; }

    
    public String getLeft() { return (left); }
    public void setLeft(String left) { this.left = left; }

    public String getLeftId() { return (leftId); }
    public void setLeftId(String leftId) { this.leftId = leftId; }

    public String getLeftSubnet() { return (leftSubnet); }
    public void setLeftSubnet(String leftSubnet) { this.leftSubnet = leftSubnet; }

    public String getLeftProtoPort() { return (leftProtoPort); }
    public void setLeftProtoPort(String leftProtoPort) { this.leftProtoPort = leftProtoPort; }

    public String getLeftNextHop() { return (leftNextHop); }
    public void setLeftNextHop(String leftNextHop) { this.leftNextHop = leftNextHop; }

    public String getRight() { return (right); }
    public void setRight(String right) { this.right = right; }

    public String getRightId() { return (rightId); }
    public void setRightId(String rightId) { this.rightId = rightId; }

    public String getRightSubnet() { return (rightSubnet); }
    public void setRightSubnet(String rightSubnet) { this.rightSubnet = rightSubnet; }

    public String getRightProtoPort() { return (rightProtoPort); }
    public void setRightProtoPort(String rightProtoPort) { this.rightProtoPort = rightProtoPort; }

    public String getRightNextHop() { return (rightNextHop); }
    public void setRightNextHop(String rightNextHop) { this.rightNextHop = rightNextHop; }

    public String getPingAddress() { return(pingAddress); }
    public void setPingAddress(String pingAddress) { this.pingAddress = pingAddress; }

    public int getPingInterval() { return(pingInterval); }
    public void setPingInterval(int pingInterval) { this.pingInterval = pingInterval; }

    // THIS IS FOR ECLIPSE - @formatter:on

    public String toJSONString()
    {
        JSONObject jO = new JSONObject(this);
        return jO.toString();
    }
}
