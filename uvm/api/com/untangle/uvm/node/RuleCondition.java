/**
 * $Id$
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
import com.untangle.uvm.GeographyManager;
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
import com.untangle.uvm.util.GlobUtil;

/**
 * This is a matching criteria for a generic Rule
 * Example: "Destination Port" == "80"
 * Example: "HTTP Host" == "salesforce.com"
 *
 * A Rule has a set of these to determine what traffic to match
 */
@SuppressWarnings("serial")
public class RuleCondition implements JSONString, Serializable
{
    protected final Logger logger = Logger.getLogger(getClass());

    /**
     * The different type of matchers currently available
     */
    public enum ConditionType {
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
            CLIENT_QUOTA_ATTAINMENT, /* 0.9 1.1 */
            SERVER_QUOTA_ATTAINMENT, /* 0.9 1.1 */
            DAY_OF_WEEK, /* "monday" "monday,tuesday" "any" */
            TIME_OF_DAY, /* "any" "10:00-11:00" */

            DST_LOCAL, /* none - ONLY available in iptables rules */
            SRC_MAC, /* 00:11:22:33:44:55 */
            DST_MAC, /* 00:11:22:33:44:55 */

            CLIENT_MAC_VENDOR, /* Intel */
            SERVER_MAC_VENDOR, /* Intel */

            CLIENT_COUNTRY, /* US (ISO 3166 country code) */
            SERVER_COUNTRY, /* JP (ISO 3166 country code)*/

            /* application specific matchers */
            HTTP_HOST, /* "playboy.com" "any" */
            HTTP_REFERER, /* "playboy.com" "any" */
            HTTP_URI, /* "/foo.html" "any" */
            HTTP_URL, /* UrlMatcher syntax "playboy.com/foo.html" */
            HTTP_CONTENT_TYPE, /* "image/jpeg" "any" */
            HTTP_CONTENT_LENGTH, /* "800" "any" */
            HTTP_USER_AGENT, /* "playboy.com" "any" */
            HTTP_USER_AGENT_OS, /* "*Mozilla*" "any" */
            PROTOCOL_CONTROL_SIGNATURE, /* "Bittorrent" "*" */
            PROTOCOL_CONTROL_CATEGORY, /* "Networking" "*" */
            PROTOCOL_CONTROL_DESCRIPTION, /* "description" "*" */
            APPLICATION_CONTROL_APPLICATION, /* GOOGLE */
            APPLICATION_CONTROL_CATEGORY, /* Proxy */
            APPLICATION_CONTROL_PROTOCHAIN, /* /IP/TCP/HTTP/GOOGLE */
            APPLICATION_CONTROL_DETAIL, /* blahblahblah */
            APPLICATION_CONTROL_CONFIDENCE, /* 100 */
            APPLICATION_CONTROL_PRODUCTIVITY, /* productivity index */
            APPLICATION_CONTROL_RISK, /* risk index */
            DIRECTORY_CONNECTOR_GROUP, /* "teachers" or "none" or "*" */
            WEB_FILTER_CATEGORY, /* "Pornography" or "Porn*" */ 
            WEB_FILTER_CATEGORY_DESCRIPTION, /* *Nudity* */
            WEB_FILTER_FLAGGED, /* boolean */
            WEB_FILTER_REQUEST_METHOD, /* GET, PUT, OPTIONS, etc */
            WEB_FILTER_REQUEST_FILE_PATH, /* /some/locaion/somefile.txt */
            WEB_FILTER_REQUEST_FILE_NAME, /* somefile.txt */
            WEB_FILTER_REQUEST_FILE_EXTENSION, /* txt */
            WEB_FILTER_RESPONSE_CONTENT_TYPE, /* video/mp4 */
            WEB_FILTER_RESPONSE_FILE_NAME, /* somefile.exe */
            WEB_FILTER_RESPONSE_FILE_EXTENSION, /* zip */
            SSL_INSPECTOR_SNI_HOSTNAME, /* "microsoft.com" */
            SSL_INSPECTOR_SUBJECT_DN, /* "CN=dropbox.com" */
            SSL_INSPECTOR_ISSUER_DN, /* "O=Thawte" */
    }

    protected RuleCondition.ConditionType matcherType = null;
    protected String value = null;
    protected Boolean invert = Boolean.FALSE;

    /**
     * These internal are used in various matchers
     * They are stored here so that repatitive evaluation is quicker
     * They are prepared by calling _computerMatchers()
     */
    private IPMatcher        ipMatcher        = null;
    private IntMatcher       intMatcher       = null;
    private IntfMatcher      intfMatcher      = null;
    private UserMatcher      userMatcher      = null;
    private GroupMatcher     groupMatcher     = null;
    private GlobMatcher      globMatcher      = null;
    private ProtocolMatcher  protocolMatcher  = null;
    private TimeOfDayMatcher timeOfDayMatcher = null;
    private DayOfWeekMatcher dayOfWeekMatcher = null;
    private UrlMatcher       urlMatcher       = null;
    
    public RuleCondition( )
    {

    }
    
    public RuleCondition( ConditionType matcherType, String value )
    {
        this.setValue(value);
        this.setConditionType(matcherType);
        computeMatchers();
    }

    public RuleCondition( ConditionType matcherType, String value, Boolean invert )
    {
        this.setValue(value);
        this.setConditionType(matcherType);
        this.setInvert(invert);
        computeMatchers();
    }
    
    public ConditionType getConditionType()
    {
        return this.matcherType;
    }

    public void setConditionType( ConditionType type ) 
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
    
    @Override
    public String toJSONString()
    {
        JSONObject jO = new JSONObject(this);
        return jO.toString();
    }

    @Override
    public String toString()
    {
        return "RuleCondition[" + this.matcherType + "," + this.value + "]";
    }
    
    /**
     * Returns true if this matcher matches the specified session
     */
    public boolean matches( NodeSession sess )
    {
        try {
            if (this.getInvert())
                return !_matches(sess);
            else
                return _matches(sess);
        } catch (Exception e) {
            logger.warn("Failed to evaluate rule condition: " + this ,e);
            return false;
        }
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
        try {
            if (this.getInvert())
                return !_matches( protocol, srcIntf, dstIntf, srcAddress, dstAddress, srcPort, dstPort );
            else
                return _matches( protocol, srcIntf, dstIntf, srcAddress, dstAddress, srcPort, dstPort );
        } catch (Exception e) {
            logger.warn("Failed to evaluate rule condition: " + this ,e);
            return false;
        }

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

        case WEB_FILTER_FLAGGED:
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
            
        case SRC_MAC:
        case DST_MAC:
        case CLIENT_MAC_VENDOR:
        case SERVER_MAC_VENDOR:
        case CLIENT_COUNTRY:
        case SERVER_COUNTRY:
        case CLIENT_HOSTNAME:
        case SERVER_HOSTNAME:
        case HTTP_HOST:
        case HTTP_REFERER:
        case HTTP_CONTENT_TYPE:
        case HTTP_USER_AGENT:
        case HTTP_USER_AGENT_OS:
        case PROTOCOL_CONTROL_SIGNATURE:
        case PROTOCOL_CONTROL_CATEGORY:
        case PROTOCOL_CONTROL_DESCRIPTION:
        case WEB_FILTER_CATEGORY:
        case WEB_FILTER_CATEGORY_DESCRIPTION:
        case WEB_FILTER_REQUEST_METHOD:
        case WEB_FILTER_REQUEST_FILE_PATH:
        case WEB_FILTER_REQUEST_FILE_NAME:
        case WEB_FILTER_REQUEST_FILE_EXTENSION:
        case WEB_FILTER_RESPONSE_CONTENT_TYPE:
        case WEB_FILTER_RESPONSE_FILE_NAME:
        case WEB_FILTER_RESPONSE_FILE_EXTENSION:
        case APPLICATION_CONTROL_APPLICATION:
        case APPLICATION_CONTROL_CATEGORY:
        case APPLICATION_CONTROL_PROTOCHAIN:
        case APPLICATION_CONTROL_DETAIL:
        case SSL_INSPECTOR_SNI_HOSTNAME:
        case SSL_INSPECTOR_SUBJECT_DN:
        case SSL_INSPECTOR_ISSUER_DN:
        case HTTP_URI:
            this.globMatcher = new GlobMatcher(value);
            break;

        case APPLICATION_CONTROL_CONFIDENCE:
        case APPLICATION_CONTROL_PRODUCTIVITY:
        case APPLICATION_CONTROL_RISK:
        case HTTP_CONTENT_LENGTH:
        case CLIENT_QUOTA_ATTAINMENT:
        case SERVER_QUOTA_ATTAINMENT:
            try {
                this.intMatcher = new IntMatcher(this.value);
            } catch (Exception e) {
                logger.warn("Invalid Int Matcher: " + value, e);
            }
            break;
            
        case DST_LOCAL:
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
        Double  attachmentDouble = null;
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

        case CLIENT_QUOTA_ATTAINMENT:
            attachmentDouble = UvmContextFactory.context().hostTable().hostQuotaAttainment( sess.getClientAddr() );
            if ( this.intMatcher == null ) {
                logger.warn("Invalid Int Matcher: " + this.intMatcher);
                return false;
            }
            return this.intMatcher.isMatch( attachmentDouble  );

        case SERVER_QUOTA_ATTAINMENT:
            attachmentDouble = UvmContextFactory.context().hostTable().hostQuotaAttainment( sess.getServerAddr() );
            if ( this.intMatcher == null ) {
                logger.warn("Invalid Int Matcher: " + this.intMatcher);
                return false;
            }
            return this.intMatcher.isMatch( attachmentDouble  );
            
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

        case SRC_MAC:
            entry = UvmContextFactory.context().hostTable().getHostTableEntry( sess.getClientAddr() );
            if (entry == null)
                return false;
            attachment = entry.getMacAddress();
            return globMatcher.isMatch( attachment );

        case DST_MAC:
            entry = UvmContextFactory.context().hostTable().getHostTableEntry( sess.getServerAddr() );
            if (entry == null)
                return false;
            attachment = entry.getMacAddress();
            return globMatcher.isMatch( attachment );

        case CLIENT_MAC_VENDOR:
            entry = UvmContextFactory.context().hostTable().getHostTableEntry( sess.getClientAddr() );
            if (entry == null)
                return false;
            attachment = entry.getMacVendor();
            return globMatcher.isMatch( attachment );

        case SERVER_MAC_VENDOR:
            entry = UvmContextFactory.context().hostTable().getHostTableEntry( sess.getServerAddr() );
            if (entry == null)
                return false;
            attachment = entry.getMacVendor();
            return globMatcher.isMatch( attachment );
            
        case CLIENT_COUNTRY:
            attachment = sess.sessionEvent().getClientCountry();
            if (attachment == null)
                return false;
            return globMatcher.isMatch( attachment );

        case SERVER_COUNTRY:
            attachment = sess.sessionEvent().getServerCountry();
            if (attachment == null)
                return false;
            return globMatcher.isMatch( attachment );

        case HTTP_URL:
            attachment = (String) sess.globalAttachment(NodeSession.KEY_HTTP_URL);
            if ( urlMatcher == null ) {
                logger.warn("Invalid Url Matcher: " + this.urlMatcher);
                return false;
            }
            return urlMatcher.isMatch( attachment );

        case HTTP_HOST:
            attachment = (String) sess.globalAttachment(NodeSession.KEY_HTTP_HOSTNAME);
            return globMatcher.isMatch( attachment );

        case HTTP_REFERER:
            attachment = (String) sess.globalAttachment(NodeSession.KEY_HTTP_REFERER);
            return globMatcher.isMatch( attachment );
            
        case HTTP_URI:
            attachment = (String) sess.globalAttachment(NodeSession.KEY_HTTP_URI);
            return globMatcher.isMatch( attachment );

        case HTTP_CONTENT_TYPE:
            attachment = (String) sess.globalAttachment(NodeSession.KEY_HTTP_CONTENT_TYPE);
            return globMatcher.isMatch( attachment );
            
        case HTTP_USER_AGENT_OS:
        case HTTP_USER_AGENT:
            entry = UvmContextFactory.context().hostTable().getHostTableEntry( sess.getClientAddr() );
            if (entry == null)
                return false;
            attachment = entry.getHttpUserAgent();
            return globMatcher.isMatch( attachment );

        case HTTP_CONTENT_LENGTH:
            attachmentLong = (Long) sess.globalAttachment(NodeSession.KEY_HTTP_CONTENT_LENGTH);
            if ( attachmentLong == null )
                return false;
            if ( this.intMatcher == null ) {
                logger.warn("Invalid Int Matcher: " + this.intMatcher);
                return false;
            }
            return this.intMatcher.isMatch( attachmentLong );
            
        case PROTOCOL_CONTROL_SIGNATURE:
            attachment = (String) sess.globalAttachment(NodeSession.KEY_APPLICATION_CONTROL_LITE_SIGNATURE);
            return globMatcher.isMatch( attachment );
                                                 
        case PROTOCOL_CONTROL_CATEGORY:
            attachment = (String) sess.globalAttachment(NodeSession.KEY_APPLICATION_CONTROL_LITE_SIGNATURE_CATEGORY);
            return globMatcher.isMatch( attachment );

        case PROTOCOL_CONTROL_DESCRIPTION:
            attachment = (String) sess.globalAttachment(NodeSession.KEY_APPLICATION_CONTROL_LITE_SIGNATURE_DESCRIPTION);
            return globMatcher.isMatch( attachment );

        case WEB_FILTER_CATEGORY:
            attachment = (String) sess.globalAttachment(NodeSession.KEY_WEB_FILTER_BEST_CATEGORY_NAME);
            return globMatcher.isMatch( attachment );

        case WEB_FILTER_CATEGORY_DESCRIPTION:
            attachment = (String) sess.globalAttachment(NodeSession.KEY_WEB_FILTER_BEST_CATEGORY_DESCRIPTION);
            return globMatcher.isMatch( attachment );

        case WEB_FILTER_FLAGGED:
            Boolean flagged = (Boolean) sess.globalAttachment(NodeSession.KEY_WEB_FILTER_FLAGGED);
            if (flagged == null)
                return false;
            return flagged.booleanValue();

        case WEB_FILTER_REQUEST_METHOD:            
            attachment = (String) sess.globalAttachment(NodeSession.KEY_WEB_FILTER_REQUEST_METHOD);
            return globMatcher.isMatch( attachment );

        case WEB_FILTER_REQUEST_FILE_PATH:
            attachment = (String) sess.globalAttachment(NodeSession.KEY_WEB_FILTER_REQUEST_FILE_PATH);
            return globMatcher.isMatch( attachment );

        case WEB_FILTER_REQUEST_FILE_NAME:
            attachment = (String) sess.globalAttachment(NodeSession.KEY_WEB_FILTER_REQUEST_FILE_NAME);
            return globMatcher.isMatch( attachment );

        case WEB_FILTER_REQUEST_FILE_EXTENSION:
            attachment = (String) sess.globalAttachment(NodeSession.KEY_WEB_FILTER_REQUEST_FILE_EXTENSION);
            return globMatcher.isMatch( attachment );

        case WEB_FILTER_RESPONSE_CONTENT_TYPE:
            attachment = (String) sess.globalAttachment(NodeSession.KEY_WEB_FILTER_RESPONSE_CONTENT_TYPE);
            return globMatcher.isMatch( attachment );

        case WEB_FILTER_RESPONSE_FILE_NAME:
            attachment = (String) sess.globalAttachment(NodeSession.KEY_WEB_FILTER_RESPONSE_FILE_NAME);
            return globMatcher.isMatch( attachment );

        case WEB_FILTER_RESPONSE_FILE_EXTENSION:
            attachment = (String) sess.globalAttachment(NodeSession.KEY_WEB_FILTER_RESPONSE_FILE_EXTENSION);
            return globMatcher.isMatch( attachment );

        case USERNAME:
            attachment = (String) sess.globalAttachment(NodeSession.KEY_PLATFORM_USERNAME);
            if (this.userMatcher.isMatch(attachment))
                return true;
            return false;

        case DIRECTORY_CONNECTOR_GROUP:
            String username = (String) sess.globalAttachment(NodeSession.KEY_PLATFORM_USERNAME);
            if (username == null)
                return false;

            DirectoryConnector directoryConnector = (DirectoryConnector)UvmContextFactory.context().nodeManager().node("untangle-node-directory-connector");
            boolean isMemberOf = false;

            if (directoryConnector.isMemberOf( username, value )) 
                isMemberOf = true;

            if (!isMemberOf) {
                List<String> groupNames = directoryConnector.memberOf( username );
                for (String groupName : groupNames) {
                    if ( this.groupMatcher.isMatch( groupName ) ) {
                        isMemberOf = true;
                        break;
                    }
                }
            }
            
            logger.debug("Checking if " + username + " is in group \"" + value + "\" : " + isMemberOf);
            return isMemberOf;

        case APPLICATION_CONTROL_APPLICATION:
            attachment = (String) sess.globalAttachment(NodeSession.KEY_APPLICATION_CONTROL_APPLICATION);
            return globMatcher.isMatch( attachment );


        case APPLICATION_CONTROL_CATEGORY:
            attachment = (String) sess.globalAttachment(NodeSession.KEY_APPLICATION_CONTROL_CATEGORY);
            return globMatcher.isMatch( attachment );
            
        case APPLICATION_CONTROL_PROTOCHAIN:
            attachment = (String) sess.globalAttachment(NodeSession.KEY_APPLICATION_CONTROL_PROTOCHAIN);
            return globMatcher.isMatch( attachment );

        case APPLICATION_CONTROL_DETAIL:
            attachment = (String) sess.globalAttachment(NodeSession.KEY_APPLICATION_CONTROL_DETAIL);
            return globMatcher.isMatch( attachment );

        case APPLICATION_CONTROL_CONFIDENCE:
            attachmentInt = (Integer) sess.globalAttachment(NodeSession.KEY_APPLICATION_CONTROL_CONFIDENCE);
            if ( attachmentInt == null )
                return false;
            if (this.intMatcher == null) {
                logger.warn("Invalid Matcher: " + this.intMatcher);
                return false;
            }

            return this.intMatcher.isMatch( attachmentInt );

        case APPLICATION_CONTROL_PRODUCTIVITY:
            attachmentInt = (Integer) sess.globalAttachment(NodeSession.KEY_APPLICATION_CONTROL_PRODUCTIVITY);
            if ( attachmentInt == null )
                return false;
            if (this.intMatcher == null) {
                logger.warn("Invalid Matcher: " + this.intMatcher);
                return false;
            }

            return this.intMatcher.isMatch( attachmentInt );

        case APPLICATION_CONTROL_RISK:
            attachmentInt = (Integer) sess.globalAttachment(NodeSession.KEY_APPLICATION_CONTROL_RISK);
            if ( attachmentInt == null )
                return false;
            if (this.intMatcher == null) {
                logger.warn("Invalid Matcher: " + this.intMatcher);
                return false;
            }

            return this.intMatcher.isMatch( attachmentInt );

        case CLIENT_HOSTNAME:
            entry = UvmContextFactory.context().hostTable().getHostTableEntry( sess.getClientAddr() );
            if (entry == null)
                return false;
            attachment = entry.getHostname();
            return globMatcher.isMatch( attachment );
            
        case SERVER_HOSTNAME:
            entry = UvmContextFactory.context().hostTable().getHostTableEntry( sess.getServerAddr() );
            if (entry == null)
                return false;
            attachment = entry.getHostname();
            return globMatcher.isMatch( attachment );

        case CLIENT_IN_PENALTY_BOX:
            return UvmContextFactory.context().hostTable().hostInPenaltyBox( sess.getClientAddr() );
            
        case SERVER_IN_PENALTY_BOX:
            return UvmContextFactory.context().hostTable().hostInPenaltyBox( sess.getServerAddr() );

        case SSL_INSPECTOR_SNI_HOSTNAME:
            attachment = (String) sess.globalAttachment(NodeSession.KEY_SSL_INSPECTOR_SNI_HOSTNAME);
            return globMatcher.isMatch( attachment );

        case SSL_INSPECTOR_SUBJECT_DN:
            attachment = (String) sess.globalAttachment(NodeSession.KEY_SSL_INSPECTOR_SUBJECT_DN);
            return globMatcher.isMatch( attachment );

        case SSL_INSPECTOR_ISSUER_DN:
            attachment = (String) sess.globalAttachment(NodeSession.KEY_SSL_INSPECTOR_ISSUER_DN);
            return globMatcher.isMatch( attachment );

        default:
            logger.warn("Unsupported Matcher Type: \"" + this.matcherType + "\""); 
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
        Double  attachmentDouble = null;
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

        case SRC_MAC:
            entry = UvmContextFactory.context().hostTable().getHostTableEntry( srcAddress );
            if (entry == null)
                return false;
            attachment = entry.getMacAddress();
            return globMatcher.isMatch( attachment );

        case DST_MAC:
            entry = UvmContextFactory.context().hostTable().getHostTableEntry( dstAddress );
            if (entry == null)
                return false;
            return globMatcher.isMatch( attachment );

        case CLIENT_MAC_VENDOR:
            entry = UvmContextFactory.context().hostTable().getHostTableEntry( srcAddress );
            if (entry == null)
                return false;
            attachment = entry.getMacVendor();
            return globMatcher.isMatch( attachment );

        case SERVER_MAC_VENDOR:
            entry = UvmContextFactory.context().hostTable().getHostTableEntry( dstAddress );
            if (entry == null)
                return false;
            attachment = entry.getMacVendor();
            return globMatcher.isMatch( attachment );

        case CLIENT_HOSTNAME:
            entry = UvmContextFactory.context().hostTable().getHostTableEntry( srcAddress );
            if (entry == null)
                return false;
            attachment = entry.getHostname();
            return globMatcher.isMatch( attachment );
            
        case SERVER_HOSTNAME:
            entry = UvmContextFactory.context().hostTable().getHostTableEntry( dstAddress );
            if (entry == null)
                return false;
            attachment = entry.getHostname();
            return globMatcher.isMatch( attachment );
            
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

        case CLIENT_QUOTA_ATTAINMENT:
            attachmentDouble = UvmContextFactory.context().hostTable().hostQuotaAttainment( srcAddress );
            if ( this.intMatcher == null ) {
                logger.warn("Invalid Int Matcher: " + this.intMatcher);
                return false;
            }
            
            return this.intMatcher.isMatch( attachmentDouble  );

        case SERVER_QUOTA_ATTAINMENT:
            attachmentDouble = UvmContextFactory.context().hostTable().hostQuotaAttainment( dstAddress );
            if ( this.intMatcher == null ) {
                logger.warn("Invalid Int Matcher: " + this.intMatcher);
                return false;
            }

            return this.intMatcher.isMatch( attachmentDouble  );
            
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

            DirectoryConnector directoryConnector = (DirectoryConnector)UvmContextFactory.context().nodeManager().node("untangle-node-directory-connector");
            boolean isMemberOf = false;

            if (directoryConnector.isMemberOf( attachment, value )) 
                isMemberOf = true;

            if (!isMemberOf) {
                List<String> groupNames = directoryConnector.memberOf( attachment );
                for (String groupName : groupNames) {
                    if ( this.groupMatcher.isMatch(groupName) ) {
                        isMemberOf = true;
                        break;
                    }
                }
            }
            
            logger.debug("Checking if " + attachment + " is in group \"" + value + "\" : " + isMemberOf);
            return isMemberOf;

        case HTTP_USER_AGENT_OS:
        case HTTP_USER_AGENT:
            entry = UvmContextFactory.context().hostTable().getHostTableEntry( srcAddress );
            if (entry == null)
                return false;
            attachment = entry.getHttpUserAgent();
            return globMatcher.isMatch( attachment );

        case CLIENT_COUNTRY:
            attachment = UvmContextFactory.context().geographyManager().getCountryCode(srcAddress.getHostAddress());
            if (attachment == null)
                return false;
            return globMatcher.isMatch( attachment );

        case SERVER_COUNTRY:
            attachment = UvmContextFactory.context().geographyManager().getCountryCode(dstAddress.getHostAddress());
            if (attachment == null)
                return false;
            return globMatcher.isMatch( attachment );

        default:
            // this is commented out because some rules are run against sessions and attributes
            // so they will call this method with unsupported matcher types.
            // logger.warn("Unsupported Matcher Type: \"" + this.matcherType + "\""); 
            break;
        }

        return false;
    }
}
