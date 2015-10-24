/**
 * $Id: IntrusionPreventionLogEvent.java 33539 2012-12-03 23:45:01Z dmorris $
 */
package com.untangle.node.intrusion_prevention;

import java.sql.Timestamp;

import com.untangle.uvm.logging.LogEvent;
import com.untangle.uvm.node.SessionEvent;
import com.untangle.uvm.util.I18nUtil;

import com.untangle.node.intrusion_prevention.IntrusionPreventionSnortUnified2IdsEvent;

/**
 * Log intrusion event
 */
@SuppressWarnings("serial")
public class IntrusionPreventionLogEvent extends LogEvent
{
    private IntrusionPreventionSnortUnified2IdsEvent ipsEvent;

    // Pass event object
    public IntrusionPreventionLogEvent( IntrusionPreventionSnortUnified2IdsEvent ipsEvent )
    {
        this.ipsEvent = ipsEvent;
    }

    @Override
    public void compileStatements( java.sql.Connection conn, java.util.Map<String,java.sql.PreparedStatement> statementCache ) throws Exception
    {
        String sql = "INSERT INTO reports.intrusion_prevention_events" + getPartitionTablePostfix() + " " +
            "( time_stamp, sig_id, gen_id, class_id, source_addr, source_port, dest_addr, dest_port, protocol, blocked, category, classtype, msg)" +
            " values " +
            "( ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ? ); ";

        java.sql.PreparedStatement pstmt = getStatementFromCache( sql, statementCache, conn );        

        int i=0;

        Timestamp ts = new Timestamp( ( ipsEvent.getEventSecond() * 1000 ) + ( ipsEvent.getEventMicrosecond() / 1000 ) );
        pstmt.setTimestamp(++i, ts );
        pstmt.setLong(++i, ipsEvent.getSignatureId() );
        pstmt.setLong(++i, ipsEvent.getGeneratorId() );
        pstmt.setLong(++i, ipsEvent.getClassificationId() );
        pstmt.setObject(++i, ipsEvent.getIpSource().getHostAddress(), java.sql.Types.OTHER);
        pstmt.setInt(++i, ( ipsEvent.getSportItype() & 0xffff ) );
        pstmt.setObject(++i, ipsEvent.getIpDestination().getHostAddress(), java.sql.Types.OTHER);
        pstmt.setInt(++i, ( ipsEvent.getDportIcode() & 0xffff ) );
        pstmt.setInt(++i, ipsEvent.getProtocol() );
        pstmt.setBoolean(++i, ( ipsEvent.getBlocked() == 1 ) ? true : false  );

        pstmt.setString(++i, ipsEvent.getCategory() );
        pstmt.setString(++i, ipsEvent.getClasstype() );
        pstmt.setString(++i, ipsEvent.getMsg() );

        pstmt.addBatch();
        return;
    }

    @Override
    public String toSummaryString()
    {
        String action;
        if ( ipsEvent.getBlocked() == 1 ? true : false )
            action = I18nUtil.marktr("blocked");
        else
            action = I18nUtil.marktr("detected");
        String summary = "Intrusion Prevention" + " " + action;
        return summary;
    }
    
    
}
