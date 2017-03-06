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
import com.untangle.uvm.UserTable;
import com.untangle.uvm.UserTableEntry;
import com.untangle.uvm.DeviceTable;
import com.untangle.uvm.DeviceTableEntry;
import com.untangle.uvm.GeographyManager;
import com.untangle.uvm.Tag;
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
        TAGGED, /* glob */
        HOST_HOSTNAME, /* glob */
        CLIENT_HOSTNAME, /* glob */
        SERVER_HOSTNAME, /* glob */
        HOST_IN_PENALTY_BOX, /* none */
        CLIENT_IN_PENALTY_BOX, /* none */
        SERVER_IN_PENALTY_BOX, /* none */
        HOST_HAS_NO_QUOTA, /* none */
        USER_HAS_NO_QUOTA, /* none */
        CLIENT_HAS_NO_QUOTA, /* none */
        SERVER_HAS_NO_QUOTA, /* none */
        HOST_QUOTA_EXCEEDED, /* none */
        USER_QUOTA_EXCEEDED, /* none */
        CLIENT_QUOTA_EXCEEDED, /* none */
        SERVER_QUOTA_EXCEEDED, /* none */
        HOST_QUOTA_ATTAINMENT, /* 0.9 1.1 */
        USER_QUOTA_ATTAINMENT, /* 0.9 1.1 */
        CLIENT_QUOTA_ATTAINMENT, /* float .9 1.1 */
        SERVER_QUOTA_ATTAINMENT,/* float */
        HOST_MAC_VENDOR, /* glob */
        CLIENT_MAC_VENDOR, /* glob */
        SERVER_MAC_VENDOR, /* glob */
        
        DAY_OF_WEEK, /* "monday" "monday,tuesday" "any" */
        TIME_OF_DAY, /* "any" "10:00-11:00" */

        DST_LOCAL, /* none - ONLY available in iptables rules */
        HOST_MAC, /* 00:11:22:33:44:55 */
        SRC_MAC, /* 00:11:22:33:44:55 */
        DST_MAC, /* 00:11:22:33:44:55 */

        REMOTE_HOST_COUNTRY, /* US (ISO 3166 country code) */
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
     * They are prepared by calling _computeMatchers()
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
         * Convert old style conditions
         */
        switch (this.matcherType) {
        case HOST_IN_PENALTY_BOX:
        case CLIENT_IN_PENALTY_BOX:
        case SERVER_IN_PENALTY_BOX:
            /**
             * These explicit conditions have been replaced by a condition
             * to just check if the host is tagged with "penalty box"
             */
            this.matcherType = ConditionType.TAGGED;
            this.value = "penalty-box";
            break;
        default:
            break;
        }
        
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
        case HOST_HAS_NO_QUOTA:
        case CLIENT_HAS_NO_QUOTA:
        case SERVER_HAS_NO_QUOTA:
        case USER_HAS_NO_QUOTA:
        case HOST_QUOTA_EXCEEDED:
        case CLIENT_QUOTA_EXCEEDED:
        case SERVER_QUOTA_EXCEEDED:
        case USER_QUOTA_EXCEEDED:
            // nothing necessary
            break;
            
        case HTTP_URL: 
            try {
                this.urlMatcher = new UrlMatcher( this.value );
            } catch (Exception e) {
                logger.warn("Invalid Url Matcher: " + value, e);
            }
            break;
            
        case TAGGED:
        case HOST_MAC:
        case SRC_MAC:
        case DST_MAC:
        case HOST_HOSTNAME:
        case CLIENT_HOSTNAME:
        case SERVER_HOSTNAME:
        case HOST_MAC_VENDOR:
        case CLIENT_MAC_VENDOR:
        case SERVER_MAC_VENDOR:
        case REMOTE_HOST_COUNTRY:
        case CLIENT_COUNTRY:
        case SERVER_COUNTRY:
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
        case HOST_QUOTA_ATTAINMENT:
        case CLIENT_QUOTA_ATTAINMENT:
        case SERVER_QUOTA_ATTAINMENT:
        case USER_QUOTA_ATTAINMENT:
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
        String  tmpStr = null;
        Integer tmpInt = null;
        Long    tmpLong = null;
        Double  tmpDouble = null;
        HostTableEntry hostEntry;
        UserTableEntry userEntry;

        if ( sess == null ) {
            logger.warn("Invalid session: " + sess );
            return false;
        }
        if ( sess.sessionEvent() == null ) {
            logger.warn("Missing session event: " + sess );
            return false;
        }
        
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

        case HOST_HAS_NO_QUOTA:
            hostEntry = UvmContextFactory.context().hostTable().getHostTableEntry( sess.sessionEvent().getLocalAddr() );
            if (hostEntry == null)
                return true;
            return ( hostEntry.getQuotaSize() == 0 );

        case CLIENT_HAS_NO_QUOTA:
            hostEntry = UvmContextFactory.context().hostTable().getHostTableEntry( sess.getClientAddr() );
            if (hostEntry == null)
                return true;
            return ( hostEntry.getQuotaSize() == 0 );

        case SERVER_HAS_NO_QUOTA:
            hostEntry = UvmContextFactory.context().hostTable().getHostTableEntry( sess.getServerAddr() );
            if (hostEntry == null)
                return true;
            return ( hostEntry.getQuotaSize() == 0 );

        case USER_HAS_NO_QUOTA:
            if ( sess.user() == null )
                return false; //no user
            userEntry = UvmContextFactory.context().userTable().getUserTableEntry( sess.user() );
            if (userEntry == null)
                return true;
            return ( userEntry.getQuotaSize() == 0 );
            
        case HOST_QUOTA_EXCEEDED:
            return UvmContextFactory.context().hostTable().hostQuotaExceeded( sess.sessionEvent().getLocalAddr() );

        case CLIENT_QUOTA_EXCEEDED:
            return UvmContextFactory.context().hostTable().hostQuotaExceeded( sess.getClientAddr() );

        case SERVER_QUOTA_EXCEEDED:
            return UvmContextFactory.context().hostTable().hostQuotaExceeded( sess.getServerAddr() );

        case USER_QUOTA_EXCEEDED:
            if ( sess.user() == null )
                return false; //no user
            return UvmContextFactory.context().userTable().userQuotaExceeded( sess.user() );
            
        case HOST_QUOTA_ATTAINMENT:
            if ( this.intMatcher == null ) {
                logger.warn("Invalid Int Matcher: " + this.intMatcher);
                return false;
            }
            tmpDouble = UvmContextFactory.context().hostTable().hostQuotaAttainment( sess.sessionEvent().getLocalAddr() );
            if (tmpDouble == null)
                return false;
            
            return this.intMatcher.isMatch( tmpDouble  );
            
        case CLIENT_QUOTA_ATTAINMENT:
            if ( this.intMatcher == null ) {
                logger.warn("Invalid Int Matcher: " + this.intMatcher);
                return false;
            }
            tmpDouble = UvmContextFactory.context().hostTable().hostQuotaAttainment( sess.getClientAddr() );
            return this.intMatcher.isMatch( tmpDouble  );

        case SERVER_QUOTA_ATTAINMENT:
            if ( this.intMatcher == null ) {
                logger.warn("Invalid Int Matcher: " + this.intMatcher);
                return false;
            }
            tmpDouble = UvmContextFactory.context().hostTable().hostQuotaAttainment( sess.getServerAddr() );
            return this.intMatcher.isMatch( tmpDouble  );

        case USER_QUOTA_ATTAINMENT:
            if ( this.intMatcher == null ) {
                logger.warn("Invalid Int Matcher: " + this.intMatcher);
                return false;
            }
            if ( sess.user() == null )
                return false; //no user
            tmpDouble = UvmContextFactory.context().userTable().userQuotaAttainment( sess.user() );
            return this.intMatcher.isMatch( tmpDouble  );
            
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

        case HOST_MAC:
            hostEntry = UvmContextFactory.context().hostTable().getHostTableEntry( sess.sessionEvent().getLocalAddr() );
            if (hostEntry == null)
                return false;
            tmpStr = hostEntry.getMacAddress();
            return globMatcher.isMatch( tmpStr );

        case SRC_MAC:
            hostEntry = UvmContextFactory.context().hostTable().getHostTableEntry( sess.getClientAddr() );
            if (hostEntry == null)
                return false;
            tmpStr = hostEntry.getMacAddress();
            return globMatcher.isMatch( tmpStr );

        case DST_MAC:
            hostEntry = UvmContextFactory.context().hostTable().getHostTableEntry( sess.getServerAddr() );
            if (hostEntry == null)
                return false;
            tmpStr = hostEntry.getMacAddress();
            return globMatcher.isMatch( tmpStr );

        case HOST_MAC_VENDOR:
            hostEntry = UvmContextFactory.context().hostTable().getHostTableEntry( sess.sessionEvent().getLocalAddr() );
            if (hostEntry == null)
                return false;
            tmpStr = hostEntry.getMacVendor();
            return globMatcher.isMatch( tmpStr );

        case CLIENT_MAC_VENDOR:
            hostEntry = UvmContextFactory.context().hostTable().getHostTableEntry( sess.getClientAddr() );
            if (hostEntry == null)
                return false;
            tmpStr = hostEntry.getMacVendor();
            return globMatcher.isMatch( tmpStr );

        case SERVER_MAC_VENDOR:
            hostEntry = UvmContextFactory.context().hostTable().getHostTableEntry( sess.getServerAddr() );
            if (hostEntry == null)
                return false;
            tmpStr = hostEntry.getMacVendor();
            return globMatcher.isMatch( tmpStr );
            
        case REMOTE_HOST_COUNTRY: {
            if ( sess.getServerAddr() != null && sess.getServerAddr().equals( sess.sessionEvent().getRemoteAddr() ) ) {
                tmpStr = sess.sessionEvent().getServerCountry();
            }
            if ( sess.getClientAddr() != null && sess.getClientAddr().equals( sess.sessionEvent().getRemoteAddr() ) ) {
                tmpStr = sess.sessionEvent().getClientCountry();
            }
            if (tmpStr == null)
                return false;
            return globMatcher.isMatch( tmpStr );
        }

        case CLIENT_COUNTRY:
            tmpStr = sess.sessionEvent().getClientCountry();
            if (tmpStr == null)
                return false;
            return globMatcher.isMatch( tmpStr );

        case SERVER_COUNTRY:
            tmpStr = sess.sessionEvent().getServerCountry();
            if (tmpStr == null)
                return false;
            return globMatcher.isMatch( tmpStr );

        case HTTP_URL:
            tmpStr = (String) sess.globalAttachment(NodeSession.KEY_HTTP_URL);
            if ( urlMatcher == null ) {
                logger.warn("Invalid Url Matcher: " + this.urlMatcher);
                return false;
            }
            return urlMatcher.isMatch( tmpStr );

        case HTTP_HOST:
            tmpStr = (String) sess.globalAttachment(NodeSession.KEY_HTTP_HOSTNAME);
            return globMatcher.isMatch( tmpStr );

        case HTTP_REFERER:
            tmpStr = (String) sess.globalAttachment(NodeSession.KEY_HTTP_REFERER);
            return globMatcher.isMatch( tmpStr );
            
        case HTTP_URI:
            tmpStr = (String) sess.globalAttachment(NodeSession.KEY_HTTP_URI);
            return globMatcher.isMatch( tmpStr );

        case HTTP_CONTENT_TYPE:
            tmpStr = (String) sess.globalAttachment(NodeSession.KEY_HTTP_CONTENT_TYPE);
            return globMatcher.isMatch( tmpStr );
            
        case HTTP_USER_AGENT_OS:
        case HTTP_USER_AGENT:
            hostEntry = UvmContextFactory.context().hostTable().getHostTableEntry( sess.getClientAddr() );
            if (hostEntry == null)
                return false;
            tmpStr = hostEntry.getHttpUserAgent();
            return globMatcher.isMatch( tmpStr );

        case HTTP_CONTENT_LENGTH:
            tmpLong = (Long) sess.globalAttachment(NodeSession.KEY_HTTP_CONTENT_LENGTH);
            if ( tmpLong == null )
                return false;
            if ( this.intMatcher == null ) {
                logger.warn("Invalid Int Matcher: " + this.intMatcher);
                return false;
            }
            return this.intMatcher.isMatch( tmpLong );
            
        case PROTOCOL_CONTROL_SIGNATURE:
            tmpStr = (String) sess.globalAttachment(NodeSession.KEY_APPLICATION_CONTROL_LITE_SIGNATURE);
            return globMatcher.isMatch( tmpStr );
                                                 
        case PROTOCOL_CONTROL_CATEGORY:
            tmpStr = (String) sess.globalAttachment(NodeSession.KEY_APPLICATION_CONTROL_LITE_SIGNATURE_CATEGORY);
            return globMatcher.isMatch( tmpStr );

        case PROTOCOL_CONTROL_DESCRIPTION:
            tmpStr = (String) sess.globalAttachment(NodeSession.KEY_APPLICATION_CONTROL_LITE_SIGNATURE_DESCRIPTION);
            return globMatcher.isMatch( tmpStr );

        case WEB_FILTER_CATEGORY:
            tmpStr = (String) sess.globalAttachment(NodeSession.KEY_WEB_FILTER_BEST_CATEGORY_NAME);
            return globMatcher.isMatch( tmpStr );

        case WEB_FILTER_CATEGORY_DESCRIPTION:
            tmpStr = (String) sess.globalAttachment(NodeSession.KEY_WEB_FILTER_BEST_CATEGORY_DESCRIPTION);
            return globMatcher.isMatch( tmpStr );

        case WEB_FILTER_FLAGGED:
            Boolean flagged = (Boolean) sess.globalAttachment(NodeSession.KEY_WEB_FILTER_FLAGGED);
            if (flagged == null)
                return false;
            return flagged.booleanValue();

        case WEB_FILTER_REQUEST_METHOD:            
            tmpStr = (String) sess.globalAttachment(NodeSession.KEY_WEB_FILTER_REQUEST_METHOD);
            return globMatcher.isMatch( tmpStr );

        case WEB_FILTER_REQUEST_FILE_PATH:
            tmpStr = (String) sess.globalAttachment(NodeSession.KEY_WEB_FILTER_REQUEST_FILE_PATH);
            return globMatcher.isMatch( tmpStr );

        case WEB_FILTER_REQUEST_FILE_NAME:
            tmpStr = (String) sess.globalAttachment(NodeSession.KEY_WEB_FILTER_REQUEST_FILE_NAME);
            return globMatcher.isMatch( tmpStr );

        case WEB_FILTER_REQUEST_FILE_EXTENSION:
            tmpStr = (String) sess.globalAttachment(NodeSession.KEY_WEB_FILTER_REQUEST_FILE_EXTENSION);
            return globMatcher.isMatch( tmpStr );

        case WEB_FILTER_RESPONSE_CONTENT_TYPE:
            tmpStr = (String) sess.globalAttachment(NodeSession.KEY_WEB_FILTER_RESPONSE_CONTENT_TYPE);
            return globMatcher.isMatch( tmpStr );

        case WEB_FILTER_RESPONSE_FILE_NAME:
            tmpStr = (String) sess.globalAttachment(NodeSession.KEY_WEB_FILTER_RESPONSE_FILE_NAME);
            return globMatcher.isMatch( tmpStr );

        case WEB_FILTER_RESPONSE_FILE_EXTENSION:
            tmpStr = (String) sess.globalAttachment(NodeSession.KEY_WEB_FILTER_RESPONSE_FILE_EXTENSION);
            return globMatcher.isMatch( tmpStr );

        case TAGGED:
            for( Tag t : sess.getTags() ) {
                if( globMatcher.isMatch( t.getName() ) )
                    return true;
            }
            return false;

        case USERNAME:
            tmpStr = (String) sess.globalAttachment(NodeSession.KEY_PLATFORM_USERNAME);
            if (this.userMatcher.isMatch(tmpStr))
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
            tmpStr = (String) sess.globalAttachment(NodeSession.KEY_APPLICATION_CONTROL_APPLICATION);
            return globMatcher.isMatch( tmpStr );


        case APPLICATION_CONTROL_CATEGORY:
            tmpStr = (String) sess.globalAttachment(NodeSession.KEY_APPLICATION_CONTROL_CATEGORY);
            return globMatcher.isMatch( tmpStr );
            
        case APPLICATION_CONTROL_PROTOCHAIN:
            tmpStr = (String) sess.globalAttachment(NodeSession.KEY_APPLICATION_CONTROL_PROTOCHAIN);
            return globMatcher.isMatch( tmpStr );

        case APPLICATION_CONTROL_DETAIL:
            tmpStr = (String) sess.globalAttachment(NodeSession.KEY_APPLICATION_CONTROL_DETAIL);
            return globMatcher.isMatch( tmpStr );

        case APPLICATION_CONTROL_CONFIDENCE:
            tmpInt = (Integer) sess.globalAttachment(NodeSession.KEY_APPLICATION_CONTROL_CONFIDENCE);
            if ( tmpInt == null )
                return false;
            if (this.intMatcher == null) {
                logger.warn("Invalid Matcher: " + this.intMatcher);
                return false;
            }

            return this.intMatcher.isMatch( tmpInt );

        case APPLICATION_CONTROL_PRODUCTIVITY:
            tmpInt = (Integer) sess.globalAttachment(NodeSession.KEY_APPLICATION_CONTROL_PRODUCTIVITY);
            if ( tmpInt == null )
                return false;
            if (this.intMatcher == null) {
                logger.warn("Invalid Matcher: " + this.intMatcher);
                return false;
            }

            return this.intMatcher.isMatch( tmpInt );

        case APPLICATION_CONTROL_RISK:
            tmpInt = (Integer) sess.globalAttachment(NodeSession.KEY_APPLICATION_CONTROL_RISK);
            if ( tmpInt == null )
                return false;
            if (this.intMatcher == null) {
                logger.warn("Invalid Matcher: " + this.intMatcher);
                return false;
            }

            return this.intMatcher.isMatch( tmpInt );

        case HOST_HOSTNAME:
            hostEntry = UvmContextFactory.context().hostTable().getHostTableEntry( sess.sessionEvent().getLocalAddr() );
            if (hostEntry == null)
                return false;
            tmpStr = hostEntry.getHostname();
            return globMatcher.isMatch( tmpStr );

        case CLIENT_HOSTNAME:
            hostEntry = UvmContextFactory.context().hostTable().getHostTableEntry( sess.getClientAddr() );
            if (hostEntry == null)
                return false;
            tmpStr = hostEntry.getHostname();
            return globMatcher.isMatch( tmpStr );
            
        case SERVER_HOSTNAME:
            hostEntry = UvmContextFactory.context().hostTable().getHostTableEntry( sess.getServerAddr() );
            if (hostEntry == null)
                return false;
            tmpStr = hostEntry.getHostname();
            return globMatcher.isMatch( tmpStr );

        case SSL_INSPECTOR_SNI_HOSTNAME:
            tmpStr = (String) sess.globalAttachment(NodeSession.KEY_SSL_INSPECTOR_SNI_HOSTNAME);
            return globMatcher.isMatch( tmpStr );

        case SSL_INSPECTOR_SUBJECT_DN:
            tmpStr = (String) sess.globalAttachment(NodeSession.KEY_SSL_INSPECTOR_SUBJECT_DN);
            return globMatcher.isMatch( tmpStr );

        case SSL_INSPECTOR_ISSUER_DN:
            tmpStr = (String) sess.globalAttachment(NodeSession.KEY_SSL_INSPECTOR_ISSUER_DN);
            return globMatcher.isMatch( tmpStr );

        case HOST_IN_PENALTY_BOX:
            //return UvmContextFactory.context().hostTable().hostInPenaltyBox( sess.sessionEvent().getLocalAddr() );
        case CLIENT_IN_PENALTY_BOX:
            //return UvmContextFactory.context().hostTable().hostInPenaltyBox( sess.getClientAddr() );
        case SERVER_IN_PENALTY_BOX:
            //return UvmContextFactory.context().hostTable().hostInPenaltyBox( sess.getServerAddr() );
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
        String tmpStr = null;
        Double  tmpDouble = null;
        InetAddress tmpAddress = null;
        HostTableEntry hostEntry;
        UserTableEntry userEntry;
        DeviceTableEntry deviceEntry;
        
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

        case TAGGED:
            hostEntry = UvmContextFactory.context().hostTable().getHostTableEntry( srcAddress );
            if (hostEntry != null) {
                for( Tag t : hostEntry.getTags() ) {
                    if( globMatcher.isMatch( t.getName() ) )
                        return true;
                }
                userEntry = UvmContextFactory.context().userTable().getUserTableEntry( hostEntry.getUsername() );
                if ( userEntry != null ) {
                    for( Tag t : userEntry.getTags() ) {
                        if( globMatcher.isMatch( t.getName() ) )
                            return true;
                    }
                }
                deviceEntry = UvmContextFactory.context().deviceTable().getDevice( hostEntry.getMacVendor() );
                if ( deviceEntry != null ) {
                    for( Tag t : deviceEntry.getTags() ) {
                        if( globMatcher.isMatch( t.getName() ) )
                            return true;
                    }
                }
            }
            return false;

        case USERNAME:
            hostEntry = UvmContextFactory.context().hostTable().getHostTableEntry( srcAddress );
            if (hostEntry == null)
                return false;
            tmpStr = hostEntry.getUsername();
            if (this.userMatcher.isMatch( tmpStr ))
                return true;
            return false;

        case HOST_MAC:
            tmpAddress = getLocalAddress( srcAddress, srcIntf, dstAddress, dstIntf );
            if (tmpAddress == null)
                return false;
            hostEntry = UvmContextFactory.context().hostTable().getHostTableEntry( tmpAddress );
            if (hostEntry == null)
                return false;
            tmpStr = hostEntry.getMacAddress();
            return globMatcher.isMatch( tmpStr );

        case SRC_MAC:
            hostEntry = UvmContextFactory.context().hostTable().getHostTableEntry( srcAddress );
            if (hostEntry == null)
                return false;
            tmpStr = hostEntry.getMacAddress();
            return globMatcher.isMatch( tmpStr );

        case DST_MAC:
            hostEntry = UvmContextFactory.context().hostTable().getHostTableEntry( dstAddress );
            if (hostEntry == null)
                return false;
            return globMatcher.isMatch( tmpStr );

        case HOST_MAC_VENDOR:
            tmpAddress = getLocalAddress( srcAddress, srcIntf, dstAddress, dstIntf );
            if (tmpAddress == null)
                return false;
            hostEntry = UvmContextFactory.context().hostTable().getHostTableEntry( tmpAddress );
            if (hostEntry == null)
                return false;
            tmpStr = hostEntry.getMacVendor();
            return globMatcher.isMatch( tmpStr );

        case CLIENT_MAC_VENDOR:
            hostEntry = UvmContextFactory.context().hostTable().getHostTableEntry( srcAddress );
            if (hostEntry == null)
                return false;
            tmpStr = hostEntry.getMacVendor();
            return globMatcher.isMatch( tmpStr );

        case SERVER_MAC_VENDOR:
            hostEntry = UvmContextFactory.context().hostTable().getHostTableEntry( dstAddress );
            if (hostEntry == null)
                return false;
            tmpStr = hostEntry.getMacVendor();
            return globMatcher.isMatch( tmpStr );

        case HOST_HOSTNAME:
            tmpAddress = getLocalAddress( srcAddress, srcIntf, dstAddress, dstIntf );
            if ( tmpAddress == null )
                return false;
            hostEntry = UvmContextFactory.context().hostTable().getHostTableEntry( tmpAddress );
            if (hostEntry == null)
                return false;
            tmpStr = hostEntry.getHostname();
            return globMatcher.isMatch( tmpStr );

        case CLIENT_HOSTNAME:
            hostEntry = UvmContextFactory.context().hostTable().getHostTableEntry( srcAddress );
            if (hostEntry == null)
                return false;
            tmpStr = hostEntry.getHostname();
            return globMatcher.isMatch( tmpStr );
            
        case SERVER_HOSTNAME:
            hostEntry = UvmContextFactory.context().hostTable().getHostTableEntry( dstAddress );
            if (hostEntry == null)
                return false;
            tmpStr = hostEntry.getHostname();
            return globMatcher.isMatch( tmpStr );
            
        case HOST_HAS_NO_QUOTA:
            tmpAddress = getLocalAddress( srcAddress, srcIntf, dstAddress, dstIntf );
            if ( tmpAddress == null )
                return false;
            hostEntry = UvmContextFactory.context().hostTable().getHostTableEntry( tmpAddress );
            if (hostEntry == null)
                return true;
            return ( hostEntry.getQuotaSize() == 0 );

        case CLIENT_HAS_NO_QUOTA:
            hostEntry = UvmContextFactory.context().hostTable().getHostTableEntry( srcAddress );
            if (hostEntry == null)
                return true;
            return ( hostEntry.getQuotaSize() == 0 );

        case SERVER_HAS_NO_QUOTA:
            hostEntry = UvmContextFactory.context().hostTable().getHostTableEntry( dstAddress );
            if (hostEntry == null)
                return true;
            return ( hostEntry.getQuotaSize() == 0 );

        case USER_HAS_NO_QUOTA:
            hostEntry = UvmContextFactory.context().hostTable().getHostTableEntry( srcAddress );
            if (hostEntry == null)
                return false;
            tmpStr = hostEntry.getUsername();
            if ( tmpStr == null )
                return false; //no user
            userEntry = UvmContextFactory.context().userTable().getUserTableEntry( tmpStr );
            if (userEntry == null)
                return true;
            return ( userEntry.getQuotaSize() == 0 );
            
        case HOST_QUOTA_EXCEEDED:
            tmpAddress = getLocalAddress( srcAddress, srcIntf, dstAddress, dstIntf );
            if ( tmpAddress == null )
                return false;
            else
                return UvmContextFactory.context().hostTable().hostQuotaExceeded( tmpAddress );

        case CLIENT_QUOTA_EXCEEDED:
            return UvmContextFactory.context().hostTable().hostQuotaExceeded( srcAddress );

        case SERVER_QUOTA_EXCEEDED:
            return UvmContextFactory.context().hostTable().hostQuotaExceeded( dstAddress );

        case USER_QUOTA_EXCEEDED:
            hostEntry = UvmContextFactory.context().hostTable().getHostTableEntry( srcAddress );
            if (hostEntry == null)
                return false;
            tmpStr = hostEntry.getUsername();
            if ( tmpStr == null )
                return false; //no user
            return UvmContextFactory.context().userTable().userQuotaExceeded( tmpStr );
            
        case HOST_QUOTA_ATTAINMENT:
            if ( this.intMatcher == null ) {
                logger.warn("Invalid Int Matcher: " + this.intMatcher);
                return false;
            }

            tmpAddress = getLocalAddress( srcAddress, srcIntf, dstAddress, dstIntf );
            if ( tmpAddress == null )
                return false;
            tmpDouble = UvmContextFactory.context().hostTable().hostQuotaAttainment( tmpAddress );
            if ( tmpDouble == null )
                return false;
            
            return this.intMatcher.isMatch( tmpDouble  );
            
        case CLIENT_QUOTA_ATTAINMENT:
            if ( this.intMatcher == null ) {
                logger.warn("Invalid Int Matcher: " + this.intMatcher);
                return false;
            }
            tmpDouble = UvmContextFactory.context().hostTable().hostQuotaAttainment( srcAddress );
            if ( tmpDouble == null )
                return false;
            
            return this.intMatcher.isMatch( tmpDouble  );

        case SERVER_QUOTA_ATTAINMENT:
            if ( this.intMatcher == null ) {
                logger.warn("Invalid Int Matcher: " + this.intMatcher);
                return false;
            }
            tmpDouble = UvmContextFactory.context().hostTable().hostQuotaAttainment( dstAddress );
            if ( tmpDouble == null )
                return false;

            return this.intMatcher.isMatch( tmpDouble  );

        case USER_QUOTA_ATTAINMENT:
            if ( this.intMatcher == null ) {
                logger.warn("Invalid Int Matcher: " + this.intMatcher);
                return false;
            }
            hostEntry = UvmContextFactory.context().hostTable().getHostTableEntry( srcAddress );
            if (hostEntry == null)
                return false;
            tmpStr = hostEntry.getUsername();
            if ( tmpStr == null )
                return false; //no user
            tmpDouble = UvmContextFactory.context().userTable().userQuotaAttainment( tmpStr );
            if ( tmpDouble == null )
                return false;

            return this.intMatcher.isMatch( tmpDouble  );
            
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
            hostEntry = UvmContextFactory.context().hostTable().getHostTableEntry( srcAddress );
            if ( hostEntry == null )
                return false;
            tmpStr = hostEntry.getUsername();
            if ( tmpStr == null )
                return false;

            DirectoryConnector directoryConnector = (DirectoryConnector)UvmContextFactory.context().nodeManager().node("untangle-node-directory-connector");
            boolean isMemberOf = false;

            if (directoryConnector.isMemberOf( tmpStr, value )) 
                isMemberOf = true;

            if (!isMemberOf) {
                List<String> groupNames = directoryConnector.memberOf( tmpStr );
                for (String groupName : groupNames) {
                    if ( this.groupMatcher.isMatch(groupName) ) {
                        isMemberOf = true;
                        break;
                    }
                }
            }
            
            logger.debug("Checking if " + tmpStr + " is in group \"" + value + "\" : " + isMemberOf);
            return isMemberOf;

        case HTTP_USER_AGENT_OS:
        case HTTP_USER_AGENT:
            hostEntry = UvmContextFactory.context().hostTable().getHostTableEntry( srcAddress );
            if (hostEntry == null)
                return false;
            tmpStr = hostEntry.getHttpUserAgent();
            return globMatcher.isMatch( tmpStr );

        case REMOTE_HOST_COUNTRY:
            if (UvmContextFactory.context().networkManager().isWanInterface( dstIntf ) ) {
                tmpStr = getCountry( dstAddress, dstIntf );
            }
            if (UvmContextFactory.context().networkManager().isWanInterface( srcIntf ) ) {
                tmpStr = getCountry( dstAddress, dstIntf );
            }
            return globMatcher.isMatch( tmpStr );

        case CLIENT_COUNTRY:
            tmpStr = getCountry( srcAddress, srcIntf );
            return globMatcher.isMatch( tmpStr );

        case SERVER_COUNTRY:
            tmpStr = getCountry( dstAddress, dstIntf );
            return globMatcher.isMatch( tmpStr );

        case HOST_IN_PENALTY_BOX:
            // tmpAddress = getLocalAddress( srcAddress, srcIntf, dstAddress, dstIntf );
            // if ( tmpAddress == null )
            //     return false;
            // else
            //     return UvmContextFactory.context().hostTable().hostInPenaltyBox( tmpAddress );
            logger.warn("Unsupported Matcher Type: \"" + this.matcherType + "\""); 
            break;

        case CLIENT_IN_PENALTY_BOX:
            // return UvmContextFactory.context().hostTable().hostInPenaltyBox( srcAddress );
            logger.warn("Unsupported Matcher Type: \"" + this.matcherType + "\""); 
            break;
            
        case SERVER_IN_PENALTY_BOX:
            // return UvmContextFactory.context().hostTable().hostInPenaltyBox( dstAddress );
            logger.warn("Unsupported Matcher Type: \"" + this.matcherType + "\""); 
            break;

        default:
            // this is commented out because some rules are run against sessions and attributes
            // so they will call this method with unsupported matcher types.
            // logger.warn("Unsupported Matcher Type: \"" + this.matcherType + "\""); 
            break;
        }

        return false;
    }
        
    /**
     * Returns the country code for address
     * If intf is a non-WAN - returns "XL" (local)
     * If unknown - return "XU"
     */
    private String getCountry( InetAddress address, int intf )
    {
        if ( UvmContextFactory.context().networkManager().isWanInterface( intf ) ) {
            return UvmContextFactory.context().geographyManager().getCountryCode(address.getHostAddress());
        } else {
            return "XL";
        }
    }

    /**
     * Returns the local address based on the parameters provided
     * Can return null if neither address is local (wan to wan)
     */
    private InetAddress getLocalAddress( InetAddress srcAddress, int srcIntf, InetAddress dstAddress, int dstIntf )
    {
        if ( ! UvmContextFactory.context().networkManager().isWanInterface( srcIntf ) )
            return srcAddress;
        if ( ! UvmContextFactory.context().networkManager().isWanInterface( dstIntf ) )
            return dstAddress;
        return null;
    }

    /**
     * Returns the remote address based on the parameters provided
     * Can return null if neither address is local (lan to lan)
     */
    private InetAddress getRemoteAddress( InetAddress srcAddress, int srcIntf, InetAddress dstAddress, int dstIntf )
    {
        if ( UvmContextFactory.context().networkManager().isWanInterface( srcIntf ) )
            return srcAddress;
        if ( UvmContextFactory.context().networkManager().isWanInterface( dstIntf ) )
            return dstAddress;
        return null;
    }
    
    
}
