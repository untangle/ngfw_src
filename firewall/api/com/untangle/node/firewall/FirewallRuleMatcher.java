/**
 * $Id: FirewallRuleMatcher.java,v 1.00 2011/08/24 14:54:43 dmorris Exp $
 */
package com.untangle.node.firewall;

import java.util.List;
import java.io.Serializable;
import java.net.InetAddress;

import org.apache.log4j.Logger;
import org.json.JSONObject;
import org.json.JSONString;

import com.untangle.uvm.UvmContextFactory;
import com.untangle.uvm.vnet.NodeSession;
import com.untangle.uvm.vnet.NodeIPSession;
import com.untangle.uvm.node.IPMatcher;
import com.untangle.uvm.node.PortMatcher;
import com.untangle.uvm.node.IntfMatcher;
import com.untangle.uvm.node.UserMatcher;
import com.untangle.uvm.node.GroupMatcher;
import com.untangle.uvm.node.ProtocolMatcher;
import com.untangle.uvm.node.DirectoryConnector;
import com.untangle.node.util.GlobUtil;

/**
 * This is a matching criteria for a Firewall Rule
 * Example: "Destination Port" == "80"
 *
 * A FirewallRule has a set of these to determine what traffic to match
 */
@SuppressWarnings("serial")
public class FirewallRuleMatcher implements JSONString, Serializable
{
    private final Logger logger = Logger.getLogger(getClass());

    /**
     * The different type of matchers currently available
     */
    public enum MatcherType {
        SRC_ADDR, /* IPMatcher syntax */
            DST_ADDR, /* IPMatcher syntax */
            SRC_PORT, /* PortMatcher syntax */
            DST_PORT, /* PortMatcher syntax */
            SRC_INTF, /* "External" "any" */
            DST_INTF, /* "External" "any" */
            PROTOCOL, /* "TCP" "UDP" "TCP,UDP" "any" */
            DIRECTORY_CONNECTOR_USERNAME, /* "dmorris" or "none" or "*" */
            DIRECTORY_CONNECTOR_GROUP, /* "teachers" or "none" or "*" */
    }

    private FirewallRuleMatcher.MatcherType matcherType = null;
    private String value = null;
    private Boolean invert = Boolean.FALSE;

    /**
     * These internal are used in various matchers
     * They are stored here so that repatitive evaluation is quicker
     * They are prepared by calling _computerMatchers()
     */
    private IPMatcher       ipMatcher       = null;
    private PortMatcher     portMatcher     = null;
    private IntfMatcher     intfMatcher     = null;
    private UserMatcher     userMatcher     = null;
    private GroupMatcher    groupMatcher     = null;
    private ProtocolMatcher protocolMatcher = null;
    
    public FirewallRuleMatcher( )
    {

    }

    public FirewallRuleMatcher( MatcherType matcherType, String value )
    {
        this.setValue(value);
        this.setMatcherType(matcherType);
        _computeMatchers();
    }

    public MatcherType getMatcherType()
    {
        return this.matcherType;
    }

    public void setMatcherType( MatcherType type ) 
    {
        this.matcherType = type;
        /* If the object is sufficiently initialized compute the cached computers */
        if (this.matcherType != null && this.value != null) _computeMatchers();
    }

    public String getValue()
    {
        return this.value;
    }

    public void setValue( String value)
    {
        this.value = value;
        /* If the object is sufficiently initialized compute the cached computers */
        if (this.matcherType != null && this.value != null) _computeMatchers();
    }

    public Boolean getInvert()
    {
        return this.invert;
    }

    public void setInvert( Boolean value )
    {
        this.invert = value;
        /* If the object is sufficiently initialized compute the cached computers */
        if ( this.matcherType != null && this.value != null ) _computeMatchers();
    }

    public String toJSONString()
    {
        JSONObject jO = new JSONObject(this);
        return jO.toString();
    }

    public boolean matches( short protocol,
                            int srcIntf, int dstIntf,
                            InetAddress srcAddress, InetAddress dstAddress,
                            int srcPort, int dstPort,
                            String username)
    {
        if (this.getInvert()) 
            return !_matches( protocol, srcIntf, dstIntf, srcAddress, dstAddress, srcPort, dstPort, username );
        else
            return _matches( protocol, srcIntf, dstIntf, srcAddress, dstAddress, srcPort, dstPort, username );
    }
    
    private boolean _matches( short protocol,
                              int srcIntf, int dstIntf,
                              InetAddress srcAddress, InetAddress dstAddress,
                              int srcPort, int dstPort,
                              String username)
    {
        String attachment = null;
        
        switch (this.matcherType) {
        case SRC_ADDR:
            if (this.ipMatcher == null) {
                logger.warn("Invalid IP Src Matcher: " + this.ipMatcher);
                return false;
            }

            return this.ipMatcher.isMatch(srcAddress);

        case DST_ADDR:
            if (this.ipMatcher == null) {
                logger.warn("Invalid IP Dst Matcher: " + this.ipMatcher);
                return false;
            }

            return this.ipMatcher.isMatch(dstAddress);

        case SRC_PORT:
            if (this.portMatcher == null) {
                logger.warn("Invalid Src Port Matcher: " + this.portMatcher);
                return false;
            }

            return this.portMatcher.isMatch(srcPort);

        case DST_PORT:
            if (this.portMatcher == null) {
                logger.warn("Invalid Dst Port Matcher: " + this.portMatcher);
                return false;
            }

            return this.portMatcher.isMatch(dstPort);

        case SRC_INTF:
            if (this.intfMatcher == null) {
                logger.warn("Invalid Src Intf Matcher: " + this.intfMatcher);
                return false;
            }

            return this.intfMatcher.isMatch(srcIntf);

        case DST_INTF:
            if (this.intfMatcher == null) {
                logger.warn("Invalid Dst Intf Matcher: " + this.intfMatcher);
                return false;
            }

            return this.intfMatcher.isMatch(dstIntf);
            
        case PROTOCOL:
            if (protocolMatcher == null) {
                logger.warn("Invalid Protocol Matcher: " + this.protocolMatcher);
                return false;
            }

            return protocolMatcher.isMatch(protocol);

        case DIRECTORY_CONNECTOR_USERNAME:
            if ("none".equals(value) && username == null)
                return true;
            if (username == null)
                return false;
            if (this.userMatcher.isMatch(username))
                return true;
            return false;

        case DIRECTORY_CONNECTOR_GROUP:
            if (username == null)
                return false;

            DirectoryConnector adconnector = (DirectoryConnector)UvmContextFactory.context().nodeManager().node("untangle-node-adconnector");
            boolean isMemberOf = false;

            if (adconnector.isMemberOf( username, value )) 
                isMemberOf = true;

            if (!isMemberOf) {
                List<String> groupNames = adconnector.memberOf( username );
                for (String groupName : groupNames) {
                    if ( this.groupMatcher.isMatch(groupName) ) {
                        isMemberOf = true;
                        break;
                    }
                }
            }
            
            logger.debug("Checking if " + username + " is in group \"" + value + "\" : " + isMemberOf);
            return isMemberOf;
            
        default:
            logger.error("Unknown Matcher Type: \"" + this.matcherType + "\""); 
            break;
        }

        return false;
    }
    
    private void _computeMatchers()
    {

        /**
         * Update cache for quick computation
         */
        switch (this.matcherType) {
        case DST_ADDR:
        case SRC_ADDR: 
            try {
                this.ipMatcher = new IPMatcher(this.value);
            } catch (Exception e) {
                logger.warn("Invalid IP Matcher: " + value, e);
            }

            break;

        case DST_PORT:
        case SRC_PORT: 
            try {
                this.portMatcher = new PortMatcher(this.value);
            } catch (Exception e) {
                logger.warn("Invalid Port Matcher: " + value, e);
            }

            break;

        case DST_INTF:
        case SRC_INTF: 
            try {
                this.intfMatcher = new IntfMatcher(this.value);
            } catch (Exception e) {
                logger.warn("Invalid Intf Matcher: " + value, e);
            }

            break;

        case DIRECTORY_CONNECTOR_USERNAME:
            try {
                this.userMatcher = new UserMatcher(this.value);
            } catch (Exception e) {
                logger.warn("Invalid User Matcher: " + value, e);
            }

            break;
            
        case DIRECTORY_CONNECTOR_GROUP:
            try {
                this.groupMatcher = new GroupMatcher(this.value);
            } catch (Exception e) {
                logger.warn("Invalid Group Matcher: " + value, e);
            }

            break;

        case PROTOCOL:
            try {
                this.protocolMatcher = new ProtocolMatcher(this.value);
            } catch (Exception e) {
                logger.warn("Invalid Intf Matcher: " + value, e);
            }

            break;

        default:
            logger.warn("Unknown Matcher type: " + this.matcherType + " - ignoring precomputing");
        }
    }
}
