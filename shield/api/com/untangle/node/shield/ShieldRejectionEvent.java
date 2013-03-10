/**
 * $Id$
 */
package com.untangle.node.shield;

import java.io.Serializable;
import java.net.InetAddress;

import com.untangle.uvm.logging.LogEvent;

/**
 * Log event for the shield rejection.
 */
@SuppressWarnings("serial")
public class ShieldRejectionEvent extends LogEvent implements Serializable
{
    private InetAddress clientAddr;
    private int        clientIntf;
    private double      reputation;
    private int         mode;
    private int         limited;
    private int         rejected;
    private int         dropped;

    // Constructors
    public ShieldRejectionEvent() { }

    public ShieldRejectionEvent( InetAddress clientAddr, int clientIntf, double reputation, int mode, int limited, int dropped, int rejected )
    {
        this.clientAddr = clientAddr;
        this.clientIntf = clientIntf;
        this.reputation = reputation;
        this.mode       = mode;
        this.limited    = limited;
        this.rejected   = rejected;
        this.dropped    = dropped;
    }


    /**
     * IP of the user that generated the event
     */
    public InetAddress getClientAddr() { return this.clientAddr; }
    public void setClientAddr(  InetAddress clientAddr  ) { this.clientAddr = clientAddr; }

    /**
     * Interface where all of the events were received.
     */
    public int getClientIntf() { return this.clientIntf; }
    public void setClientIntf(  int clientIntf  ) { this.clientIntf = clientIntf; }

    /**
     * Reputation of the user at the time of the event.
     */
    public double getReputation() { return reputation; }
    public void setReputation(  double reputation  ) { this.reputation = reputation; }

    /**
     * Mode of the system when this event occured.
     */
    public int getMode() { return mode; }
    public void setMode(  int mode  ) { this.mode = mode; }

    /**
     * Number of limited sessions since the last time the user generated an event.
     */
    public int getLimited() { return limited; }
    public void setLimited(  int limited  ) { this.limited = limited; }

    /**
     * Number of rejected sessions since the last time the user generated an event.
     */
    public int getRejected() { return rejected; }
    public void setRejected(  int rejected  ) { this.rejected = rejected; }

    /**
     * Number of dropped sessions since the last time the user generated an event.
     */
    public int getDropped() { return dropped; }
    public void setDropped(  int dropped  ) { this.dropped = dropped; }

    private static String sql = "INSERT INTO reports.shield_rejection_totals " +
        "(time_stamp, client_addr, client_intf, mode, reputation, limited, dropped, rejected) " +
        "values " +
        "( ?, ?, ?, ?, ?, ?, ?, ? )" ;
    
    @Override
    public java.sql.PreparedStatement getDirectEventSql( java.sql.Connection conn ) throws Exception
    {
        java.sql.PreparedStatement pstmt = conn.prepareStatement( sql );

        int i=0;
        pstmt.setTimestamp(++i, getTimeStamp());
        pstmt.setObject(++i, getClientAddr().getHostAddress(), java.sql.Types.OTHER);
        pstmt.setInt(++i, getClientIntf());
        pstmt.setInt(++i, getMode());
        pstmt.setDouble(++i, getReputation());
        pstmt.setInt(++i, getLimited());
        pstmt.setInt(++i, getDropped());
        pstmt.setInt(++i, getRejected());

        return pstmt;
    }
}
