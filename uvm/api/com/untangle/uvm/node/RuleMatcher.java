/**
 * $Id: RuleMatcher.java,v 1.00 2011/08/24 14:43:42 dmorris Exp $
 */
package com.untangle.uvm.node;

import java.util.List;
import java.io.Serializable;
import java.net.InetAddress;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import org.apache.log4j.Logger;
import org.json.JSONObject;
import org.json.JSONString;

import com.untangle.uvm.UvmContextFactory;
import com.untangle.uvm.HostTable;
import com.untangle.uvm.HostTableEntry;
import com.untangle.uvm.vnet.NodeSession;
import com.untangle.uvm.vnet.NodeSession;
import com.untangle.uvm.node.IPMatcher;
import com.untangle.uvm.node.IntMatcher;
import com.untangle.uvm.node.IntfMatcher;
import com.untangle.uvm.node.UserMatcher;
import com.untangle.uvm.node.GroupMatcher;
import com.untangle.uvm.node.ProtocolMatcher;
import com.untangle.uvm.node.UrlMatcher;
import com.untangle.uvm.node.DirectoryConnector;
import com.untangle.node.util.GlobUtil;

/**
 * This is a matching criteria for a generic Rule
 * Example: "Destination Port" == "80"
 * Example: "HTTP Host" == "salesforce.com"
 *
 * A Rule has a set of these to determine what traffic to match
 */
@SuppressWarnings("serial")
public class RuleMatcher implements JSONString, Serializable
{
    protected final Logger logger = Logger.getLogger(getClass());

    /**
     * The different type of matchers currently available
     */
    public enum MatcherType {
        /* Generic IP matchers */
        SRC_ADDR, /* IPMatcher syntax */
            DST_ADDR, /* IPMatcher syntax */
            SRC_PORT, /* IntMatcher syntax */
            DST_PORT, /* IntMatcher syntax */
            SRC_INTF, /* "External" "any" */
            DST_INTF, /* "External" "any" */
            PROTOCOL, /* "TCP" "UDP" "TCP,UDP" "any" */
            USERNAME, /* "dmorris" or "none" or "*" */
            CLIENT_HOSTNAME, /* glob */
            SERVER_HOSTNAME, /* glob */
            CLIENT_IN_PENALTY_BOX, /* none */
            SERVER_IN_PENALTY_BOX, /* none */
            CLIENT_HAS_NO_QUOTA, /* none */
            SERVER_HAS_NO_QUOTA, /* none */
            CLIENT_QUOTA_EXCEEDED, /* none */
            SERVER_QUOTA_EXCEEDED, /* none */
            DAY_OF_WEEK, /* "monday" "monday,tuesday" "any" */
            TIME_OF_DAY, /* "any" "10:00-11:00" */

            DST_LOCAL, /* none - ONLY available in iptables rules */
            SRC_MAC, /* 00:11:22:33:44:55 - ONLY available in iptables rules */
            
            /* application specific matchers */
            HTTP_HOST, /* "playboy.com" "any" */
            HTTP_URI, /* "/foo.html" "any" */
            HTTP_URL, /* UrlMatcher syntax "playboy.com/foo.html" */
            HTTP_CONTENT_TYPE, /* "image/jpeg" "any" */
            HTTP_CONTENT_LENGTH, /* "800" "any" */
            HTTP_USER_AGENT, /* "playboy.com" "any" */
            HTTP_USER_AGENT_OS, /* "*Mozilla*" "any" */
            PROTOCOL_CONTROL_SIGNATURE, /* "Bittorrent" "*" */
            PROTOCOL_CONTROL_CATEGORY, /* "Networking" "*" */
            PROTOCOL_CONTROL_DESCRIPTION, /* "description" "*" */
            CLASSD_APPLICATION, /* GOOGLE */
            CLASSD_CATEGORY, /* Proxy */
            CLASSD_PROTOCHAIN, /* /IP/TCP/HTTP/GOOGLE */
            CLASSD_DETAIL, /* blahblahblah */
            CLASSD_CONFIDENCE, /* 100 */
            CLASSD_PRODUCTIVITY, /* productivity index */
            CLASSD_RISK, /* risk index */
            DIRECTORY_CONNECTOR_GROUP, /* "teachers" or "none" or "*" */
            SITEFILTER_CATEGORY, /* "Pornography" or "Porn*" */ 
            SITEFILTER_CATEGORY_DESCRIPTION, /* *Nudity* */
            SITEFILTER_FLAGGED, /* boolean */
            HTTPS_SNI_HOSTNAME, /* "microsoft.com" "any" */

            /* DEPRECATED */
            /* DEPRECATED */
            /* DEPRECATED */
            DIRECTORY_CONNECTOR_USERNAME, /* DEPRECATED in 9.4 */
            SITEFILTER_CATEGORY_FLAGGED, /* DEPRECATED */
            ESOFT_WEB_FILTER_CATEGORY, /* DEPRECATED */
            ESOFT_WEB_FILTER_CATEGORY_DESCRIPTION, /* DEPRECATED */
            ESOFT_WEB_FILTER_CATEGORY_FLAGGED, /* DEPRECATED */
    }

    protected RuleMatcher.MatcherType matcherType = null;
    protected String value = null;
    protected Boolean invert = Boolean.FALSE;

    /**
     * These internal are used in various matchers
     * They are stored here so that repatitive evaluation is quicker
     * They are prepared by calling _computerMatchers()
     */
    private IPMatcher        ipMatcher       = null;
    private IntMatcher       intMatcher     = null;
    private IntfMatcher      intfMatcher     = null;
    private UserMatcher      userMatcher     = null;
    private GroupMatcher     groupMatcher     = null;
    private ProtocolMatcher  protocolMatcher = null;
    private TimeOfDayMatcher timeOfDayMatcher = null;
    private DayOfWeekMatcher dayOfWeekMatcher = null;
    private UrlMatcher       urlMatcher     = null;
    private String           regexValue      = null;
    private Integer          intValue        = null;
    private Long             longValue        = null;
    
    public RuleMatcher( )
    {

    }
    
    public RuleMatcher( MatcherType matcherType, String value )
    {
        this.setValue(value);
        this.setMatcherType(matcherType);
        computeMatchers();
    }

    public RuleMatcher( MatcherType matcherType, String value, Boolean invert )
    {
        this.setValue(value);
        this.setMatcherType(matcherType);
        this.setInvert(invert);
        computeMatchers();
    }
    
    public MatcherType getMatcherType()
    {
        return this.matcherType;
    }

    public void setMatcherType( MatcherType type ) 
    {
        this.matcherType = type;
        /* If the object is sufficiently initialized compute the cached computers */
        if (this.matcherType != null && this.value != null ) computeMatchers();
    }

    public String getValue()
    {
        return this.value;
    }

    public void setValue( String value)
    {
        this.value = value;
        /* If the object is sufficiently initialized compute the cached computers */
        if (this.matcherType != null && this.value != null ) computeMatchers();
    }

    public Boolean getInvert()
    {
        return this.invert;
    }

    public void setInvert( Boolean value )
    {
        this.invert = value;
        /* If the object is sufficiently initialized compute the cached computers */
        if ( this.matcherType != null && this.value != null ) computeMatchers();
    }
    
    public String toJSONString()
    {
        JSONObject jO = new JSONObject(this);
        return jO.toString();
    }
    
    /**
     * Returns true if this matcher matches the specified session
     */
    public boolean matches( NodeSession sess )
    {
        if (this.getInvert()) 
            return !_matches(sess);
        else
            return _matches(sess);
    }

    /**
     * This provides limited matching
     * This is useful for sessions that do not yet exists
     * Many matches will never match in this case because the session does not exist
     */
    public boolean matches( short protocol,
                            int srcIntf, int dstIntf,
                            InetAddress srcAddress, InetAddress dstAddress,
                            int srcPort, int dstPort)
    {
        if (this.getInvert()) 
            return !_matches( protocol, srcIntf, dstIntf, srcAddress, dstAddress, srcPort, dstPort );
        else
            return _matches( protocol, srcIntf, dstIntf, srcAddress, dstAddress, srcPort, dstPort );
    }
    
    /**
     * This pre-computes any information necessary for fast matching
     */
    protected void computeMatchers()
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
                this.intMatcher = new IntMatcher(this.value);
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

        case PROTOCOL:
            try {
                this.protocolMatcher = new ProtocolMatcher(this.value);
            } catch (Exception e) {
                logger.warn("Invalid Intf Matcher: " + value, e);
            }

            break;
            
        case DIRECTORY_CONNECTOR_USERNAME:
        case USERNAME:
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
            
        case TIME_OF_DAY:
            try {
                this.timeOfDayMatcher = new TimeOfDayMatcher(this.value);
            } catch (Exception e) {
                logger.warn("Invalid Time Of Day Matcher: " + value, e);
            }

            break;

        case DAY_OF_WEEK:
            try {
                this.dayOfWeekMatcher = new DayOfWeekMatcher(this.value);
            } catch (Exception e) {
                logger.warn("Invalid Day Of Week Matcher: " + value, e);
            }

            break;

        case SITEFILTER_FLAGGED:
        case CLIENT_IN_PENALTY_BOX:
        case SERVER_IN_PENALTY_BOX:
        case CLIENT_HAS_NO_QUOTA:
        case SERVER_HAS_NO_QUOTA:
        case CLIENT_QUOTA_EXCEEDED:
        case SERVER_QUOTA_EXCEEDED:
            // nothing necessary
            break;

        case HTTP_URL: 
            try {
                this.urlMatcher = new UrlMatcher( this.value );
            } catch (Exception e) {
                logger.warn("Invalid Url Matcher: " + value, e);
            }

            break;
            
        case CLIENT_HOSTNAME:
        case SERVER_HOSTNAME:
        case HTTP_HOST:
        case HTTP_URI:
        case HTTP_CONTENT_TYPE:
        case HTTP_USER_AGENT:
        case HTTP_USER_AGENT_OS:
        case PROTOCOL_CONTROL_SIGNATURE:
        case PROTOCOL_CONTROL_CATEGORY:
        case PROTOCOL_CONTROL_DESCRIPTION:
        case SITEFILTER_CATEGORY:
        case SITEFILTER_CATEGORY_DESCRIPTION:
        case CLASSD_APPLICATION:
        case CLASSD_CATEGORY:
        case CLASSD_PROTOCHAIN:
        case CLASSD_DETAIL:
        case HTTPS_SNI_HOSTNAME:
            this.regexValue = GlobUtil.globToRegex(value);
            break;

        case CLASSD_CONFIDENCE:
        case CLASSD_PRODUCTIVITY:
        case CLASSD_RISK:
        case HTTP_CONTENT_LENGTH:
            try {
                this.intMatcher = new IntMatcher(this.value);
            } catch (Exception e) {
                logger.warn("Invalid Port Matcher: " + value, e);
            }

            break;
            
        case SITEFILTER_CATEGORY_FLAGGED:
        case ESOFT_WEB_FILTER_CATEGORY:
        case ESOFT_WEB_FILTER_CATEGORY_DESCRIPTION:
        case ESOFT_WEB_FILTER_CATEGORY_FLAGGED:
            logger.warn("matcher deprecated: " + this.matcherType);
            break;

        case DST_LOCAL:
        case SRC_MAC:
            break;
            
        default:
            logger.warn("Unknown Matcher type: " + this.matcherType + " - ignoring precomputing");
        }
    }

    private boolean _matches( NodeSession sess )
    {
        String  attachment = null;
        Integer attachmentInt = null;
        Long    attachmentLong = null;
        HostTableEntry entry;
        
        switch (this.matcherType) {
        case SRC_ADDR:
            if (this.ipMatcher == null) {
                logger.warn("Invalid IP Src Matcher: " + this.ipMatcher);
                return false;
            }

            return this.ipMatcher.isMatch(sess.getClientAddr());

        case DST_ADDR:
            if (this.ipMatcher == null) {
                logger.warn("Invalid IP Dst Matcher: " + this.ipMatcher);
                return false;
            }

            return this.ipMatcher.isMatch(sess.getServerAddr());

        case SRC_PORT:
            if (this.intMatcher == null) {
                logger.warn("Invalid Src Port Matcher: " + this.intMatcher);
                return false;
            }

            return this.intMatcher.isMatch(sess.getClientPort());

        case DST_PORT:
            if (this.intMatcher == null) {
                logger.warn("Invalid Dst Port Matcher: " + this.intMatcher);
                return false;
            }

            return this.intMatcher.isMatch(sess.getServerPort());

        case SRC_INTF:
            if (this.intfMatcher == null) {
                logger.warn("Invalid Src Intf Matcher: " + this.intfMatcher);
                return false;
            }

            return this.intfMatcher.isMatch(sess.getClientIntf());

        case DST_INTF:
            if (this.intfMatcher == null) {
                logger.warn("Invalid Dst Intf Matcher: " + this.intfMatcher);
                return false;
            }

            return this.intfMatcher.isMatch(sess.getServerIntf());
            
        case PROTOCOL:
            if (protocolMatcher == null) {
                logger.warn("Invalid Protocol Matcher: " + this.protocolMatcher);
                return false;
            }

            return protocolMatcher.isMatch(sess.getProtocol());

        case CLIENT_HAS_NO_QUOTA:
            entry = UvmContextFactory.context().hostTable().getHostTableEntry( sess.getClientAddr() );
            if (entry == null)
                return true;
            return ( entry.getQuotaSize() == 0 );

        case SERVER_HAS_NO_QUOTA:
            entry = UvmContextFactory.context().hostTable().getHostTableEntry( sess.getServerAddr() );
            if (entry == null)
                return true;
            return ( entry.getQuotaSize() == 0 );

        case CLIENT_QUOTA_EXCEEDED:
            return UvmContextFactory.context().hostTable().hostQuotaExceeded( sess.getClientAddr() );

        case SERVER_QUOTA_EXCEEDED:
            return UvmContextFactory.context().hostTable().hostQuotaExceeded( sess.getServerAddr() );
            
        case TIME_OF_DAY:
            if ( timeOfDayMatcher == null ) {
                logger.warn("Invalid Time Of Day Matcher: " + this.timeOfDayMatcher);
                return false;
            }

            return timeOfDayMatcher.isMatch();

        case DAY_OF_WEEK:
            if ( dayOfWeekMatcher == null ) {
                logger.warn("Invalid Day Of Week Matcher: " + this.dayOfWeekMatcher);
                return false;
            }

            return dayOfWeekMatcher.isMatch();

        case HTTP_URL:
            attachment = (String) sess.globalAttachment(NodeSession.KEY_HTTP_URL);

            if ( urlMatcher == null ) {
                logger.warn("Invalid Url Matcher: " + this.urlMatcher);
                return false;
            }

            return urlMatcher.isMatch( attachment );

        case HTTP_HOST:
            attachment = (String) sess.globalAttachment(NodeSession.KEY_HTTP_HOSTNAME);
            if (attachment == null)
                return false;
            return Pattern.matches(regexValue, attachment);

        case HTTP_URI:
            attachment = (String) sess.globalAttachment(NodeSession.KEY_HTTP_URI);
            if (attachment == null)
                return false;
            return Pattern.matches(regexValue, attachment);

        case HTTP_CONTENT_TYPE:
            attachment = (String) sess.globalAttachment(NodeSession.KEY_HTTP_CONTENT_TYPE);
            if (attachment == null)
                return false;
            return Pattern.matches(regexValue, attachment);
            
        case HTTP_USER_AGENT:
            entry = UvmContextFactory.context().hostTable().getHostTableEntry( sess.getClientAddr() );
            if (entry == null)
                return false;
            attachment = entry.getHttpUserAgent();
            if (attachment == null)
                return false;
            return Pattern.matches(regexValue, attachment);
            
        case HTTP_USER_AGENT_OS:
            entry = UvmContextFactory.context().hostTable().getHostTableEntry( sess.getClientAddr() );
            if (entry == null)
                return false;
            attachment = entry.getHttpUserAgentOs();
            if (attachment == null)
                return false;
            return Pattern.matches(regexValue, attachment);

        case HTTP_CONTENT_LENGTH:
            attachmentLong = (Long) sess.globalAttachment(NodeSession.KEY_HTTP_CONTENT_LENGTH);
            if (this.intMatcher == null) {
                logger.warn("Invalid Dst Port Matcher: " + this.intMatcher);
                return false;
            }

            return this.intMatcher.isMatch( attachmentLong );
            
        case PROTOCOL_CONTROL_SIGNATURE:
            attachment = (String) sess.globalAttachment(NodeSession.KEY_PROTOFILTER_SIGNATURE);
            if (attachment == null)
                return false;
            return Pattern.matches(regexValue, attachment);
                                                 
        case PROTOCOL_CONTROL_CATEGORY:
            attachment = (String) sess.globalAttachment(NodeSession.KEY_PROTOFILTER_SIGNATURE_CATEGORY);
            if (attachment == null)
                return false;
            return Pattern.matches(regexValue, attachment);

        case PROTOCOL_CONTROL_DESCRIPTION:
            attachment = (String) sess.globalAttachment(NodeSession.KEY_PROTOFILTER_SIGNATURE_DESCRIPTION);
            if (attachment == null)
                return false;
            return Pattern.matches(regexValue, attachment);

        case ESOFT_WEB_FILTER_CATEGORY:
        case ESOFT_WEB_FILTER_CATEGORY_DESCRIPTION:
        case ESOFT_WEB_FILTER_CATEGORY_FLAGGED:
        case SITEFILTER_CATEGORY_FLAGGED:
            logger.warn("matcher deprecated: " + this.matcherType);
            return false;
            
        case SITEFILTER_CATEGORY:
            attachment = (String) sess.globalAttachment(NodeSession.KEY_SITEFILTER_BEST_CATEGORY_NAME);
            if (attachment == null)
                return false;
            return Pattern.matches(regexValue, attachment);

        case SITEFILTER_CATEGORY_DESCRIPTION:
            attachment = (String) sess.globalAttachment(NodeSession.KEY_SITEFILTER_BEST_CATEGORY_DESCRIPTION);
            if (attachment == null)
                return false;
            return Pattern.matches(regexValue, attachment);

        case SITEFILTER_FLAGGED:
            Boolean flagged = (Boolean) sess.globalAttachment(NodeSession.KEY_SITEFILTER_FLAGGED);
            if (flagged == null)
                return false;
            return flagged.booleanValue();

        case DIRECTORY_CONNECTOR_USERNAME:
        case USERNAME:
            attachment = (String) sess.globalAttachment(NodeSession.KEY_PLATFORM_USERNAME);
            if (this.userMatcher.isMatch(attachment))
                return true;
            return false;

        case DIRECTORY_CONNECTOR_GROUP:
            String username = (String) sess.globalAttachment(NodeSession.KEY_PLATFORM_USERNAME);
            if (username == null)
                return false;

            DirectoryConnector adconnector = (DirectoryConnector)UvmContextFactory.context().nodeManager().node("untangle-node-adconnector");
            boolean isMemberOf = false;

            if (adconnector.isMemberOf( username, value )) 
                isMemberOf = true;

            if (!isMemberOf) {
                List<String> groupNames = adconnector.memberOf( username );
                for (String groupName : groupNames) {
                    if ( this.groupMatcher.isMatch( groupName ) ) {
                        isMemberOf = true;
                        break;
                    }
                }
            }
            
            logger.debug("Checking if " + username + " is in group \"" + value + "\" : " + isMemberOf);
            return isMemberOf;

        case CLASSD_APPLICATION:
            attachment = (String) sess.globalAttachment(NodeSession.KEY_CLASSD_APPLICATION);
            if (attachment == null)
                return false;
            if (attachment.equals(regexValue)) //check exact equals first
                return true;
            return Pattern.matches(regexValue, attachment);

        case CLASSD_CATEGORY:
            attachment = (String) sess.globalAttachment(NodeSession.KEY_CLASSD_CATEGORY);
            if (attachment == null)
                return false;
            if (attachment.equals(regexValue)) //check exact equals first
                return true;
            return Pattern.matches(regexValue, attachment);
            
        case CLASSD_PROTOCHAIN:
            attachment = (String) sess.globalAttachment(NodeSession.KEY_CLASSD_PROTOCHAIN);
            if (attachment == null)
                return false;
            if (attachment.equals(regexValue)) //check exact equals first
                return true;
            return Pattern.matches(regexValue, attachment);

        case CLASSD_DETAIL:
            attachment = (String) sess.globalAttachment(NodeSession.KEY_CLASSD_DETAIL);
            if (attachment == null)
                return false;
            if (attachment.equals(regexValue)) //check exact equals first
                return true;
            return Pattern.matches(regexValue, attachment);

        case CLASSD_CONFIDENCE:
            attachmentInt = (Integer) sess.globalAttachment(NodeSession.KEY_CLASSD_CONFIDENCE);
            if (attachmentInt != null && attachmentInt > this.intValue)
                return true;
            else
                return false;

        case CLASSD_PRODUCTIVITY:
            attachmentInt = (Integer) sess.globalAttachment(NodeSession.KEY_CLASSD_PRODUCTIVITY);
            if (this.intMatcher == null) {
                logger.warn("Invalid Dst Port Matcher: " + this.intMatcher);
                return false;
            }

            return this.intMatcher.isMatch( attachmentInt );

        case CLASSD_RISK:
            attachmentInt = (Integer) sess.globalAttachment(NodeSession.KEY_CLASSD_RISK);
            if (this.intMatcher == null) {
                logger.warn("Invalid Dst Port Matcher: " + this.intMatcher);
                return false;
            }

            return this.intMatcher.isMatch( attachmentInt );

        case CLIENT_HOSTNAME:
            entry = UvmContextFactory.context().hostTable().getHostTableEntry( sess.getClientAddr() );
            if (entry == null)
                return false;
            attachment = entry.getHostname();
            if (attachment == null)
                return false;
            return Pattern.matches(regexValue, attachment);
            
        case SERVER_HOSTNAME:
            entry = UvmContextFactory.context().hostTable().getHostTableEntry( sess.getServerAddr() );
            if (entry == null)
                return false;
            attachment = entry.getHostname();
            if (attachment == null)
                return false;
            return Pattern.matches(regexValue, attachment);

        case CLIENT_IN_PENALTY_BOX:
            return UvmContextFactory.context().hostTable().hostInPenaltyBox( sess.getClientAddr() );
            
        case SERVER_IN_PENALTY_BOX:
            return UvmContextFactory.context().hostTable().hostInPenaltyBox( sess.getServerAddr() );

        case HTTPS_SNI_HOSTNAME:
            attachment = (String) sess.globalAttachment(NodeSession.KEY_HTTPS_SNI_HOSTNAME);
            if (attachment == null)
                return false;
            return Pattern.matches(regexValue, attachment);

        default:
            logger.error("Unsupported Matcher Type: \"" + this.matcherType + "\""); 
            break;
        }

        return false;
    }

    private boolean _matches( short protocol,
                              int srcIntf, int dstIntf,
                              InetAddress srcAddress, InetAddress dstAddress,
                              int srcPort, int dstPort)
    {
        String attachment = null;
        HostTableEntry entry;
        
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
            if (this.intMatcher == null) {
                logger.warn("Invalid Src Port Matcher: " + this.intMatcher);
                return false;
            }

            return this.intMatcher.isMatch(srcPort);

        case DST_PORT:
            if (this.intMatcher == null) {
                logger.warn("Invalid Dst Port Matcher: " + this.intMatcher);
                return false;
            }

            return this.intMatcher.isMatch(dstPort);

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

        case USERNAME:
            entry = UvmContextFactory.context().hostTable().getHostTableEntry( srcAddress );
            if (entry == null)
                return false;
            attachment = entry.getUsername();
            if (this.userMatcher.isMatch( attachment ))
                return true;
            return false;

        case CLIENT_HOSTNAME:
            entry = UvmContextFactory.context().hostTable().getHostTableEntry( srcAddress );
            if (entry == null)
                return false;
            attachment = entry.getHostname();
            if (attachment == null)
                return false;
            return Pattern.matches(regexValue, attachment);
            
        case SERVER_HOSTNAME:
            entry = UvmContextFactory.context().hostTable().getHostTableEntry( dstAddress );
            if (entry == null)
                return false;
            attachment = entry.getHostname();
            if (attachment == null)
                return false;
            return Pattern.matches(regexValue, attachment);
            
        case CLIENT_IN_PENALTY_BOX:
            return UvmContextFactory.context().hostTable().hostInPenaltyBox( srcAddress );
            
        case SERVER_IN_PENALTY_BOX:
            return UvmContextFactory.context().hostTable().hostInPenaltyBox( dstAddress );

        case CLIENT_HAS_NO_QUOTA:
            entry = UvmContextFactory.context().hostTable().getHostTableEntry( srcAddress );
            if (entry == null)
                return true;
            return ( entry.getQuotaSize() == 0 );

        case SERVER_HAS_NO_QUOTA:
            entry = UvmContextFactory.context().hostTable().getHostTableEntry( dstAddress );
            if (entry == null)
                return true;
            return ( entry.getQuotaSize() == 0 );

        case CLIENT_QUOTA_EXCEEDED:
            return UvmContextFactory.context().hostTable().hostQuotaExceeded( srcAddress );

        case SERVER_QUOTA_EXCEEDED:
            return UvmContextFactory.context().hostTable().hostQuotaExceeded( dstAddress );
            
        case TIME_OF_DAY:
            if (timeOfDayMatcher == null) {
                logger.warn("Invalid Time Of Day Matcher: " + this.timeOfDayMatcher);
                return false;
            }

            return timeOfDayMatcher.isMatch();

        case DAY_OF_WEEK:
            if (dayOfWeekMatcher == null) {
                logger.warn("Invalid Day Of Week Matcher: " + this.dayOfWeekMatcher);
                return false;
            }

            return dayOfWeekMatcher.isMatch();

        case DIRECTORY_CONNECTOR_GROUP:
            entry = UvmContextFactory.context().hostTable().getHostTableEntry( srcAddress );
            if ( entry == null )
                return false;
            attachment = entry.getUsername();
            if ( attachment == null )
                return false;

            DirectoryConnector adconnector = (DirectoryConnector)UvmContextFactory.context().nodeManager().node("untangle-node-adconnector");
            boolean isMemberOf = false;

            if (adconnector.isMemberOf( attachment, value )) 
                isMemberOf = true;

            if (!isMemberOf) {
                List<String> groupNames = adconnector.memberOf( attachment );
                for (String groupName : groupNames) {
                    if ( this.groupMatcher.isMatch(groupName) ) {
                        isMemberOf = true;
                        break;
                    }
                }
            }
            
            logger.debug("Checking if " + attachment + " is in group \"" + value + "\" : " + isMemberOf);
            return isMemberOf;

        case HTTP_USER_AGENT:
            entry = UvmContextFactory.context().hostTable().getHostTableEntry( srcAddress );
            if (entry == null)
                return false;
            attachment = entry.getHttpUserAgent();
            if (attachment == null)
                return false;
            return Pattern.matches(regexValue, attachment);
            
        case HTTP_USER_AGENT_OS:
            entry = UvmContextFactory.context().hostTable().getHostTableEntry( srcAddress );
            if (entry == null)
                return false;
            attachment = entry.getHttpUserAgentOs();
            if (attachment == null)
                return false;
            return Pattern.matches(regexValue, attachment);

        default:
            logger.error("Unsupported Matcher Type: \"" + this.matcherType + "\""); 
            break;
        }

        return false;
    }
    
}
