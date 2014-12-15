/**
 * $Id: IdpsLogEvent.java 33539 2012-12-03 23:45:01Z dmorris $
 */
package com.untangle.node.idps;

import java.sql.Timestamp;

import com.untangle.uvm.logging.LogEvent;
import com.untangle.uvm.node.SessionEvent;
import com.untangle.uvm.util.I18nUtil;

import com.untangle.node.idps.IdpsSnortUnified2IdsEvent;

/**
 * Log intrusion event
 */
@SuppressWarnings("serial")
public class IdpsLogEvent extends LogEvent
{
    private IdpsSnortUnified2IdsEvent idpsEvent;

    // constructors -----------------------------------------------------------

    // Pass event object
    public IdpsLogEvent( IdpsSnortUnified2IdsEvent idpsEvent )
    {
        this.idpsEvent = idpsEvent;
    }

    // accessors --------------------------------------------------------------

    private static String sql = "INSERT INTO reports.idps_events " +
        "( time_stamp, sig_id, gen_id, class_id, source_addr, source_port, dest_addr, dest_port, protocol, blocked, category, classtype, description)" +
        " values " +
        "( ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ? ); ";

    @Override
    public java.sql.PreparedStatement getDirectEventSql( java.sql.Connection conn ) throws Exception
    {
        java.sql.PreparedStatement pstmt = conn.prepareStatement( sql );

        int i=0;

        Timestamp ts = new Timestamp( ( idpsEvent.getEventSecond() * 1000 ) + ( idpsEvent.getEventMicrosecond() / 1000 ) );
        pstmt.setTimestamp(++i, ts );
        pstmt.setLong(++i, idpsEvent.getSignatureId() );
        pstmt.setLong(++i, idpsEvent.getGeneratorId() );
        pstmt.setLong(++i, idpsEvent.getClassificationId() );
        pstmt.setObject(++i, idpsEvent.getIpSource().getHostAddress(), java.sql.Types.OTHER);
        pstmt.setInt(++i, idpsEvent.getSportItype() );
        pstmt.setObject(++i, idpsEvent.getIpDestination().getHostAddress(), java.sql.Types.OTHER);
        pstmt.setInt(++i, idpsEvent.getDportIcode() );
        pstmt.setInt(++i, idpsEvent.getProtocol() );
        pstmt.setBoolean(++i, ( idpsEvent.getBlocked() == 1 ) ? true : false  );

        pstmt.setString(++i, idpsEvent.getCategory() );
        pstmt.setString(++i, idpsEvent.getClasstype() );
        pstmt.setString(++i, idpsEvent.getDescription() );

        return pstmt;
    }

    @Override
    public String toSummaryString()
    {
        String action;
        if ( idpsEvent.getBlocked() == 1 ? true : false )
            action = I18nUtil.marktr("blocked");
        else
            action = I18nUtil.marktr("detected");
        String summary = "Intrusion Prevention" + " " + action;
        return summary;
    }
    
    
}
