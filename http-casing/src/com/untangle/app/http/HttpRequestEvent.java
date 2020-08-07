/**
 * $Id$
 */
package com.untangle.app.http;

import java.net.URI;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Pattern;

import com.untangle.uvm.logging.LogEvent;
import com.untangle.uvm.app.SessionEvent;
import com.untangle.uvm.util.I18nUtil;

/**
 * Log event for a request.
 * This modifies the http_events table with all the information in the request
 */
@SuppressWarnings("serial")
public class HttpRequestEvent extends LogEvent
{
    private static final Pattern DOT_MATCH = Pattern.compile("\\.");

    private Long requestId;
    private HttpMethod method;
    private URI requestUri;
    private SessionEvent sessionEvent;
    private String host;
    private String domain;
    private String referer;
    private long contentLength;

    public static final Set<String> COUNTRY_CODES;

    static {
        COUNTRY_CODES = new HashSet<>();
        COUNTRY_CODES.add("ao");
        COUNTRY_CODES.add("ar");
        COUNTRY_CODES.add("arpa");
        COUNTRY_CODES.add("au");
        COUNTRY_CODES.add("bd");
        COUNTRY_CODES.add("bn");
        COUNTRY_CODES.add("br");
        COUNTRY_CODES.add("co");
        COUNTRY_CODES.add("cr");
        COUNTRY_CODES.add("cy");
        COUNTRY_CODES.add("do");
        COUNTRY_CODES.add("eg");
        COUNTRY_CODES.add("et");
        COUNTRY_CODES.add("fj");
        COUNTRY_CODES.add("fk");
        COUNTRY_CODES.add("gh");
        COUNTRY_CODES.add("gn");
        COUNTRY_CODES.add("gu");
        COUNTRY_CODES.add("id");
        COUNTRY_CODES.add("il");
        COUNTRY_CODES.add("jm");
        COUNTRY_CODES.add("ke");
        COUNTRY_CODES.add("kh");
        COUNTRY_CODES.add("kw");
        COUNTRY_CODES.add("kz");
        COUNTRY_CODES.add("lb");
        COUNTRY_CODES.add("lc");
        COUNTRY_CODES.add("lr");
        COUNTRY_CODES.add("ls");
        COUNTRY_CODES.add("ml");
        COUNTRY_CODES.add("mm");
        COUNTRY_CODES.add("mv");
        COUNTRY_CODES.add("mw");
        COUNTRY_CODES.add("mx");
        COUNTRY_CODES.add("my");
        COUNTRY_CODES.add("ng");
        COUNTRY_CODES.add("ni");
        COUNTRY_CODES.add("np");
        COUNTRY_CODES.add("nz");
        COUNTRY_CODES.add("om");
        COUNTRY_CODES.add("pa");
        COUNTRY_CODES.add("pe");
        COUNTRY_CODES.add("pg");
        COUNTRY_CODES.add("pw");
        COUNTRY_CODES.add("py");
        COUNTRY_CODES.add("qa");
        COUNTRY_CODES.add("sa");
        COUNTRY_CODES.add("sb");
        COUNTRY_CODES.add("sv");
        COUNTRY_CODES.add("sy");
        COUNTRY_CODES.add("th");
        COUNTRY_CODES.add("tn");
        COUNTRY_CODES.add("tz");
        COUNTRY_CODES.add("uk");
        COUNTRY_CODES.add("uy");
        COUNTRY_CODES.add("va");
        COUNTRY_CODES.add("ve");
        COUNTRY_CODES.add("ye");
        COUNTRY_CODES.add("yu");
        COUNTRY_CODES.add("za");
        COUNTRY_CODES.add("zm");
        COUNTRY_CODES.add("zw");
    }

    /**
     * Create an HttpRequestEvent.
     */
    public HttpRequestEvent() { }

    /**
     * Create an HttpRequestEvent.
     * @param requestLine 
     * @param host 
     * @param referer 
     * @param contentLength 
     */
    public HttpRequestEvent( RequestLine requestLine, String host, String referer, long contentLength )
    {
        this.host = host;
        this.domain = getDomainForHost( host );
        this.contentLength = contentLength;
        this.requestId = requestLine.getRequestId();
        this.timeStamp = requestLine.getTimeStamp();
        this.method = requestLine.getMethod();
        this.requestUri = requestLine.getRequestUri();
        this.sessionEvent = requestLine.getSessionEvent();
        this.referer = referer;
    }

    /**
     * The host, as specified by the request header.
     * @return 
     */
    public String getHost() { return host; }

    /**
     * setHost.
     * @param newValue 
     */
    public void setHost( String newValue )
    {
        this.host = newValue;
        this.domain = getDomainForHost( host );
    }

    /**
     * The host, as specified by the request header.
     * @return 
     */
    public String getDomain() { return domain; }

    /**
     * setDomain.
     * @param newValue 
     */
    public void setDomain( String newValue ) { this.domain = newValue; }
    
    /**
     * Content length, as counted by the parser.
     * @return 
     */
    public long getContentLength() { return contentLength; }

    /**
     * setContentLength.
     * @param newValue 
     */
    public void setContentLength( long newValue ) { this.contentLength = newValue; }

    /**
     * Get the requestId
     * @return 
     */
    public Long getRequestId() { return this.requestId; }

    /**
     * setRequestId.
     * @param newValue 
     */
    public void setRequestId(  Long newValue  ) { this.requestId = newValue; }
    
    /**
     * Request method.
     * @return 
     */
    public HttpMethod getMethod() { return method; }

    /**
     * setMethod.
     * @param newValue 
     */
    public void setMethod( HttpMethod newValue ) { this.method = newValue; }

    /**
     * Request URI.
     * @return 
     */
    public URI getRequestUri() { return requestUri; }

    /**
     * setRequestUri.
     * @param newValue 
     */
    public void setRequestUri( URI newValue ) { this.requestUri = newValue; }

    /**
     * The referer, as specified in the header.
     * @return 
     */
    public String getReferer() { return referer; }

    /**
     * setReferer.
     * @param newValue 
     */
    public void setReferer( String newValue ) { this.referer = newValue; }

    /**
     * The Session event for this request
     * @return 
     */
    public SessionEvent getSessionEvent() { return sessionEvent; }

    /**
     * setSessionEvent.
     * @param newValue 
     */
    public void setSessionEvent( SessionEvent newValue ) { this.sessionEvent = newValue; }
    
    /**
     * Compile SQL statements
     * @param conn 
     * @param statementCache
     * @throws Exception 
     */
    @Override
    public void compileStatements( java.sql.Connection conn, java.util.Map<String,java.sql.PreparedStatement> statementCache ) throws Exception
    {
        String sql = "INSERT INTO " + schemaPrefix() + "http_events" + getPartitionTablePostfix() + " " +
            "(time_stamp, " +
            "session_id, client_intf, server_intf, " +
            "c_client_addr, c_client_port, c_server_addr, c_server_port, " + 
            "s_client_addr, s_client_port, s_server_addr, s_server_port, " + 

            "client_country, client_latitude, client_longitude, " + 
            "server_country, server_latitude, server_longitude, " +

            "policy_id, username, " + 
            "request_id, method, uri, " + 
            "host, domain, referer, c2s_content_length, " + 
            "hostname) " + 
            "values " +
            "( ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

        java.sql.PreparedStatement pstmt = getStatementFromCache( sql, statementCache, conn );        

        int i=0;
        pstmt.setTimestamp(++i, getSqlTimeStamp());
        pstmt.setLong(++i, getSessionEvent().getSessionId());
        pstmt.setInt(++i, getSessionEvent().getClientIntf());
        pstmt.setInt(++i, getSessionEvent().getServerIntf());
        pstmt.setObject(++i, getSessionEvent().getCClientAddr().getHostAddress(), java.sql.Types.OTHER);
        pstmt.setInt(++i, getSessionEvent().getCClientPort());
        pstmt.setObject(++i, getSessionEvent().getCServerAddr().getHostAddress(), java.sql.Types.OTHER);
        pstmt.setInt(++i, getSessionEvent().getCServerPort());
        pstmt.setObject(++i, getSessionEvent().getSClientAddr().getHostAddress(), java.sql.Types.OTHER);
        pstmt.setInt(++i, getSessionEvent().getSClientPort());
        pstmt.setObject(++i, getSessionEvent().getSServerAddr().getHostAddress(), java.sql.Types.OTHER);
        pstmt.setInt(++i, getSessionEvent().getSServerPort());

        pstmt.setString(++i, getSessionEvent().getClientCountry());
        pstmt.setDouble(++i, (getSessionEvent().getClientLatitude() == null ? 0 : getSessionEvent().getClientLatitude()) );
        pstmt.setDouble(++i, (getSessionEvent().getClientLongitude() == null ? 0 : getSessionEvent().getClientLongitude()) );
        pstmt.setString(++i, getSessionEvent().getServerCountry());
        pstmt.setDouble(++i, (getSessionEvent().getServerLatitude() == null ? 0 : getSessionEvent().getServerLatitude()) );
        pstmt.setDouble(++i, (getSessionEvent().getServerLongitude() == null ? 0 : getSessionEvent().getServerLongitude()) );

        pstmt.setLong(++i, getSessionEvent().getPolicyId());
        pstmt.setString(++i, getSessionEvent().getUsername());
        pstmt.setLong(++i, getRequestId());
        pstmt.setString(++i, Character.toString(getMethod().getKey()));
        pstmt.setString(++i, getRequestUri().toString());
        pstmt.setString(++i, getHost());
        pstmt.setString(++i, getDomain());
        pstmt.setString(++i, getReferer());
        pstmt.setLong(++i, getContentLength());
        pstmt.setString(++i, getSessionEvent().getHostname());

        pstmt.addBatch();
        return;
    }

    /**
     * Human readable string
     * @return string
     */
    public String toString()
    {
        return "HttpRequestEvent: " + toSummaryString();
    }

    /**
     * A human readable summary string
     * @return string
     */
    @Override
    public String toSummaryString()
    {
        String summary = sessionEvent.getCClientAddr().getHostAddress() + " " + I18nUtil.marktr("requested") +
            " (" + getMethod().toString() + ") " + 
            ( sessionEvent.getSServerPort() == 443 ? "https" : "http" ) + "://" +
            getHost() + getRequestUri();
        return summary;
    }


    /**
     * Translates a host to a "domain"
     * Examples:
     *  foo.bar.yahoo.com -> yahoo.com
     *  foor.bar.co.uk -> bar.co.uk
     * This tries to take country domains into account.
     * @param host
     * @return domain
     */
    private String getDomainForHost( String host )
    {
        if ( host == null )
            return null;
        
        int portPos = host.indexOf(':');
        if(portPos > -1){
            host = host.substring(0, portPos);
        }

        // String[] parts = host.split("\\.");
        String[] parts = DOT_MATCH.split(host);
        int len = parts.length;
        
        if (parts.length <= 2) {
            return host;
        }
        
        String lastPart = parts[len-1];

        boolean isNumber = true;
        for(var i = 0; i < lastPart.length(); i++){
            if(Character.isDigit(lastPart.charAt(i))){
                isNumber = false;
                break;
            }
        }
        if(isNumber){
            /*
             * If the last part is an int, its probably an IP
             * Just return the whole IP.
             */
            return host;
        }

        if(COUNTRY_CODES.contains(lastPart)){
            // If last part is a country suffix, include the next previous part.
            // https://wiki.mozilla.org/TLD_List
            if ( parts.length > 2 )
                return parts[len-3] + "." + parts[len-2] + "." + parts[len-1];
            else
                return host;
        }else{
            return parts[len-2] + "." + parts[len-1];
        }
    }
    
}
