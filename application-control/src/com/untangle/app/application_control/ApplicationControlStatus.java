/**
 * $Id: ApplicationControlStatus.java 37325 2014-03-04 19:37:11Z dmorris $
 */

package com.untangle.app.application_control;

import com.untangle.uvm.vnet.IPNewSessionRequest;
import org.apache.log4j.Logger;

/**
 * Class to track the classification status of a network session.
 * 
 * @author mahotz
 * 
 */

public class ApplicationControlStatus
{
    private final Logger logger = Logger.getLogger(getClass());

    public enum StatusCode
    {
        EMPTY, FOUND, FAILURE
    }

    public String sessionInfo;

    public String clientAddr;
    public int clientPort;
    public String serverAddr;
    public int serverPort;
    public long sessionId;

    public String application;
    public String protochain;
    public String detail;
    public int confidence;
    public int state;

    public boolean tarpit;
    public int modcount;
    public int chunkCount;

    /**
     * Constructor
     * 
     * @param sessionInfo
     *        The sessionn information
     * @param ipr
     *        The IP session request
     */
    public ApplicationControlStatus(String sessionInfo, IPNewSessionRequest ipr)
    {
        this.sessionInfo = sessionInfo;

        this.clientAddr = ipr.getOrigClientAddr().getHostAddress();
        this.clientPort = ipr.getOrigClientPort();
        this.serverAddr = ipr.getNewServerAddr().getHostAddress();
        this.serverPort = ipr.getNewServerPort();
        this.sessionId = ipr.id();

        // use the IP protocol to set the initial app and protochain
        // names since it is possible the session will come and go
        // before ever being classified

        if (ipr.getProtocol() == IPNewSessionRequest.PROTO_TCP) {
            application = "TCP";
            protochain = "/TCP";
        }

        else if (ipr.getProtocol() == IPNewSessionRequest.PROTO_UDP) {
            application = "UDP";
            protochain = "/UDP";
        }

        else {
            application = "unknown";
            protochain = "unknown";
        }

        this.detail = "";
        this.confidence = 0;
        this.state = 0;

        this.tarpit = false;
        this.modcount = 1;
        this.chunkCount = 0;
    }

    /**
     * Function to update a status record with the status information returned
     * from the classd daemon
     * 
     * @param traffic
     *        The status string received from the classd daemon
     * @return The status of the update operation
     */
    public StatusCode updateStatus(String traffic)
    {
        // THIS IS FOR ECLIPSE - @formatter:off
        
    /*
        The daemon will give us the status in the following format
        ----------------------------------------------------------
        FOUND: 1234567890
        APPLICATION: GOOGLE
        PROTOCHAIN: /TCP/HTTP/GOOGLE
        DETAIL: blahblahblah (SSL=tls host, FBOOKAPP=app, HTTP=content type)
        CONFIDENCE: 100
        STATE: 1

        If we ask about a session that the daemon doesn't know about for
        some reason, the response will be a single line like this
        ----------------------------------------------------------
        EMPTY: 1234567890
    */

        // THIS IS FOR ECLIPSE - @formatter:on

        String application;
        String protochain;
        String detail;
        int confidence;
        int state;

        // first we look for an empty response
        if (traffic.startsWith("EMPTY: ") == true) return (StatusCode.EMPTY);

        if (traffic.indexOf('\0') >= 0) {
            logger.warn("Detected NULL characters: " + traffic);
            return (StatusCode.EMPTY);
        }

        // not empty so search for all the parsing tags in the response.
        // note how we include the space following the colon in the
        // strings below so the substring will go to the right offset
        String appStr = "APPLICATION: ";
        int appLoc = traffic.indexOf(appStr);
        if (appLoc < 0) return (StatusCode.FAILURE);

        String protoStr = "PROTOCHAIN: ";
        int protoLoc = traffic.indexOf(protoStr);
        if (protoLoc < 0) return (StatusCode.FAILURE);

        String detailStr = "DETAIL: ";
        int detailLoc = traffic.indexOf(detailStr);
        if (detailLoc < 0) return (StatusCode.FAILURE);

        String confStr = "CONFIDENCE: ";
        int confLoc = traffic.indexOf(confStr);
        if (confLoc < 0) return (StatusCode.FAILURE);

        String stateStr = "STATE: ";
        int stateLoc = traffic.indexOf(stateStr);
        if (stateLoc < 0) return (StatusCode.FAILURE);

        // extract each of the fields from the response
        application = traffic.substring(appLoc + appStr.length(), traffic.indexOf("\r\n", appLoc));
        protochain = traffic.substring(protoLoc + protoStr.length(), traffic.indexOf("\r\n", protoLoc));
        detail = traffic.substring(detailLoc + detailStr.length(), traffic.indexOf("\r\n", detailLoc));
        confidence = Integer.parseInt(traffic.substring(confLoc + confStr.length(), traffic.indexOf("\r\n", confLoc)));
        state = Integer.parseInt(traffic.substring(stateLoc + stateStr.length(), traffic.indexOf("\r\n", stateLoc)));

        // update our member variables only when stuff has changed
        if (application.equals(this.application) == false) {
            this.application = application;
            this.modcount++;
        }

        if (protochain.equals(this.protochain) == false) {
            this.protochain = protochain;
            this.modcount++;
        }

        if (detail.equals(this.detail) == false) {
            this.detail = detail;
            this.modcount++;
        }

        if (confidence != this.confidence) {
            this.confidence = confidence;
            this.modcount++;
        }

        if (state != this.state) {
            this.state = state;
            this.modcount++;
        }

        return (StatusCode.FOUND);
    }

    /**
     * Used to determine if the record has been changed
     * 
     * @return The number of modifications since the last call to this function
     */
    public int getChangeCount()
    {
        int modcount = this.modcount;
        this.modcount = 0;
        return (modcount);
    }

    /**
     * @return A String representation of this object used for logging
     */
    public String toString()
    {
        String string = new String();
        string = "[" + state + "|" + confidence + "|" + application + "|" + protochain + "|" + detail + "]";
        return (string);
    }
}
