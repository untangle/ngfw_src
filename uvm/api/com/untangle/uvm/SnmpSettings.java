/**
 * $Id$
 */
package com.untangle.uvm;

import java.io.Serializable;

import org.json.JSONObject;
import org.json.JSONString;

/**
 * For those not familiar with SNMP, here is a bit of an explanation
 * of the relationship between properties:
 * <br><br>
 *   <ul>
 *   <li>{@link #setEnabled Enabled} turns the entire SNMP support on/off.  If
 *   this property is false, the remainder of the properties have no
 *   significance.
 *   </li>
 *   <li>
 *   {@link #getCommunityString CommunityString} is like a crappy UID/PWD combination.  This
 *   represents the UID/PWD of someone who can read the contents of the MIB on the UVM
 *   machine.  This user has <b>read only access</b>.  There is no read/write access
 *   possible with our implementation (for safety reasons).
 *   </li>
 *   <li>
 *   The property {@link #getPort Port} property is the port on-which the UVM
 *   will listen for UDP SNMP messages.  The default is 161.
 *   </li>
 *   <li>
 *   {@link #getSysContact SysContact} and {@link #getSysLocation SysLocation} are
 *   related in that they are informational properties, useful only if someone is
 *   monitoring the UVM as one of many devices.  These fields are in no way required,
 *   but may make management easier.
 *   </li>
 *   <li>
 *   {@link #setSendTraps SendTraps} controls if SNMP traps are sent from the UVM.  If this
 *   is true, then {@link #setTrapHost TrapHost}, {@link #setTrapPort TrapPort}, and
 *   {@link #setTrapCommunity TrapCommunity} must be set.
 *   </li>
 * </ul>
 * <br><br>
 * If Snmp {@link #isEnabled is enabled}, the {@link #getCommunityString CommunityString}
 * must be set (everything else can be defaulted).  If {@link #isSendTraps traps are enabled},
 * then {@link #setTrapHost TrapHost} and {@link #setTrapCommunity TrapCommunity} must be
 * set.
 */
@SuppressWarnings("serial")
public class SnmpSettings implements Serializable, JSONString
{
    /**
     * The standard port for "normal" agent messages, as
     * per RFC 1157 sect 4.  The value is 161
     */
    public static final int STANDARD_MSG_PORT = 161;

    /**
     * The standard port for trap messages, as per RFC 1157
     * sect 4.  The value is 162
     */
    public static final int STANDARD_TRAP_PORT = 162;

    private boolean enabled;
    private int port;
    private String communityString;
    private String sysContact;
    private String sysLocation;

    private boolean v3Enabled;
    private boolean v3Required;
    private String v3Username;
    private String v3AuthenticationProtocol;
    private String v3AuthenticationPassphrase;
    private String v3PrivacyProtocol;
    private String v3PrivacyPassphrase;

    private boolean sendTraps;
    private String trapHost;
    private String trapCommunity;
    private int trapPort;

    public boolean isEnabled() { return this.enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }

    public int getPort() { return this.port; }
    public void setPort(int port) { this.port = port; }

    /**
     * This cannot be blank ("") or null
     *
     * @return the community String
     */
    public String getCommunityString() { return this.communityString; }
    public void setCommunityString(String s) { this.communityString = s; }

    /**
     * @return the contact info
     */
    public String getSysContact() { return this.sysContact; }
    public void setSysContact(String s) { this.sysContact = s; }

    /**
     * @return the system location
     */
    public String getSysLocation() { return this.sysLocation; }
    public void setSysLocation(String s) { this.sysLocation = s; }

    /**
     * SNMP v3
     */
    public boolean isV3Enabled() { return this.v3Enabled; }
    public void setV3Enabled(boolean enabled) { this.v3Enabled = enabled; }

    public boolean isV3Required() { return this.v3Required; }
    public void setV3Required(boolean enabled) { this.v3Required = enabled; }

    public String getV3Username() { return this.v3Username; }
    public void setV3Username(String s) { this.v3Username = s; }

    public String getV3AuthenticationProtocol() { return this.v3AuthenticationProtocol; }
    public void setV3AuthenticationProtocol(String s) { this.v3AuthenticationProtocol = s; }

    public String getV3AuthenticationPassphrase() { return this.v3AuthenticationPassphrase; }
    public void setV3AuthenticationPassphrase(String s) { this.v3AuthenticationPassphrase = s; }

    public String getV3PrivacyProtocol() { return this.v3PrivacyProtocol; }
    public void setV3PrivacyProtocol(String s) { this.v3PrivacyProtocol = s; }

    public String getV3PrivacyPassphrase() { return this.v3PrivacyPassphrase; }
    public void setV3PrivacyPassphrase(String s) { this.v3PrivacyPassphrase = s; }

    /**
     * Traps
     */
    public void setSendTraps(boolean sendTraps) { this.sendTraps = sendTraps; }
    public boolean isSendTraps() { return this.sendTraps; }

    public void setTrapHost(String trapHost) { this.trapHost = trapHost; }
    public String getTrapHost() { return this.trapHost; }

    public void setTrapCommunity(String tc) { this.trapCommunity = tc; }
    public String getTrapCommunity() { return this.trapCommunity; }

    public void setTrapPort(int tp) { this.trapPort = tp; }
    public int getTrapPort() { return this.trapPort; }

    public String toJSONString()
    {
        JSONObject jO = new JSONObject(this);
        return jO.toString();
    }

}
