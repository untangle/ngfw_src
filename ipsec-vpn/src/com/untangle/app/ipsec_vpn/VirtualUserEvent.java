/**
 * $Id: VirtualUserEvent.java 37267 2014-02-26 23:42:19Z dmorris $
 */

package com.untangle.app.ipsec_vpn;

import java.util.Date;
import java.net.InetAddress;
import java.sql.Timestamp;

import com.untangle.uvm.logging.LogEvent;

/**
 * Class for recording the details for VPN clients that connect to the server.
 * When a client connects, the status record is created. When the client
 * disconnects, the status record is updated with the total connect time and the
 * amount of data sent and received by the client.
 * 
 * @author mahotz
 * 
 */

@SuppressWarnings("serial")
public class VirtualUserEvent extends LogEvent
{
    private InetAddress clientAddress;
    private String clientProtocol;
    private String clientUsername;
    private String netInterface;
    private String netProcess;
    private String elapsedTime;
    private Long netRXbytes;
    private Long netTXbytes;
    private Long eventId;

    private boolean updateMode = false;

    private static long eventSequence = 0;

    public VirtualUserEvent(InetAddress clientAddress, String clientProtocol, String clientUsername, String netInterface, String netProcess)
    {
        setClientAddress(clientAddress);
        setClientProtocol(clientProtocol);
        setClientUsername(clientUsername);
        setNetInterface(netInterface);
        setNetProcess(netProcess);
        setEventId(new Long(generateEventId()));
    }

    public void updateEvent(String elapsedTime, Long netRXbytes, Long netTXbytes)
    {
        setTimeStamp(new Timestamp((new Date()).getTime()));
        setElapsedTime(elapsedTime);
        setNetRXbytes(netRXbytes);
        setNetTXbytes(netTXbytes);
        updateMode = true;
    }

    private long generateEventId()
    {
        // Since we update the database event on session disconnect, we need
        // a large range of event id values that will always be unique.
        if (eventSequence == 0) {
            long current = (System.currentTimeMillis() / 1000);
            long spread = 1000000000;
            eventSequence = (current * spread);
        }

        return (++eventSequence);
    }

    public InetAddress getClientAddress()
    {
        return clientAddress;
    }

    public void setClientAddress(InetAddress clientAddress)
    {
        this.clientAddress = clientAddress;
    }

    public String getClientProtocol()
    {
        return clientProtocol;
    }

    public void setClientProtocol(String clientProtocol)
    {
        this.clientProtocol = clientProtocol;
    }

    public String getClientUsername()
    {
        return clientUsername;
    }

    public void setClientUsername(String clientUsername)
    {
        this.clientUsername = clientUsername;
    }

    public String getElapsedTime()
    {
        return elapsedTime;
    }

    public void setElapsedTime(String elapsedTime)
    {
        this.elapsedTime = elapsedTime;
    }

    public String getNetInterface()
    {
        return netInterface;
    }

    public void setNetInterface(String netInterface)
    {
        this.netInterface = netInterface;
    }

    public String getNetProcess()
    {
        return netProcess;
    }

    public void setNetProcess(String netProcess)
    {
        this.netProcess = netProcess;
    }

    public Long getNetRXbytes()
    {
        return netRXbytes;
    }

    public void setNetRXbytes(Long netRXbytes)
    {
        this.netRXbytes = netRXbytes;
    }

    public Long getNetTXbytes()
    {
        return netTXbytes;
    }

    public void setNetTXbytes(Long netTXbytes)
    {
        this.netTXbytes = netTXbytes;
    }

    public Long getEventId()
    {
        return eventId;
    }

    public void setEventId(Long eventId)
    {
        this.eventId = eventId;
    }

    @Override
    public void compileStatements(java.sql.Connection conn, java.util.Map<String, java.sql.PreparedStatement> statementCache) throws Exception
    {
        String insert = "INSERT INTO " + schemaPrefix() + "ipsec_user_events" + getPartitionTablePostfix() + " " + "(event_id, time_stamp, connect_stamp, client_address, client_protocol, client_username, net_interface, net_process) " + "values ( ?, ?, ?, ?, ?, ?, ?, ? )";

        String update = "UPDATE " + schemaPrefix() + "ipsec_user_events" + getPartitionTablePostfix() + " " + "SET goodbye_stamp = ?, elapsed_time = ?, rx_bytes = ?, tx_bytes = ? " + "WHERE event_id = ?";

        int i = 0;

        // when updateMode is false we prepare the insert statement
        if (updateMode == false) {
            java.sql.PreparedStatement pstmt = getStatementFromCache(insert, statementCache, conn);

            pstmt.setLong(++i, getEventId().longValue());
            pstmt.setTimestamp(++i, getTimeStamp());
            pstmt.setTimestamp(++i, getTimeStamp());
            pstmt.setString(++i, getClientAddress().getHostAddress().toString());
            pstmt.setString(++i, getClientProtocol());
            pstmt.setString(++i, getClientUsername());
            pstmt.setString(++i, getNetInterface());
            pstmt.setString(++i, getNetProcess());
            pstmt.addBatch();
            return;
        }

        // the updateMode flag is set so we prepare the update statement
        java.sql.PreparedStatement pstmt = getStatementFromCache(update, statementCache, conn);

        pstmt.setTimestamp(++i, getTimeStamp());
        pstmt.setString(++i, getElapsedTime());
        pstmt.setLong(++i, getNetRXbytes());
        pstmt.setLong(++i, getNetTXbytes());
        pstmt.setLong(++i, getEventId().longValue());

        pstmt.addBatch();
        return;
    }

    @Override
    public String toSummaryString()
    {
        String summary = "VirtualUserEvent: " + getClientUsername() + " " + getClientAddress().getHostAddress();
        return summary;
    }
}
