/**
 * $Id: IntrusionPreventionLogEvent.java 33539 2012-12-03 23:45:01Z dmorris $
 */
package com.untangle.app.intrusion_prevention;

import java.sql.Timestamp;
import java.net.InetAddress;

import com.untangle.uvm.logging.LogEvent;
import com.untangle.uvm.util.I18nUtil;

/**
 * Log intrusion event
 */
@SuppressWarnings("serial")
public class IntrusionPreventionLogEvent extends LogEvent
{
    private long timestamp = 0L;
    private long signatureId = 0;
    private long generatorId = 0;
    private InetAddress ipSource = null;
    private InetAddress ipDestination = null;
    private int sportItype = 0;
    private int dportIcode = 0;
    private boolean blocked = false;
    private String msg = "";
    private String classtype = "";
    private String category = "";
    private String ruleId = "";
    private String protocol = "";

    public IntrusionPreventionLogEvent( ) {}

    public IntrusionPreventionLogEvent(long timestamp, long generatorId, long signatureId, InetAddress ipSource, int sourcePort, InetAddress ipDestination, int destinationPort, String msg, String classtype, String category, String ruleId, String protocol, boolean blocked)
    {
        this.timestamp = timestamp;
        this.generatorId = generatorId;
        this.signatureId = signatureId;

        this.ipSource = ipSource;
        this.sportItype = sourcePort;
        this.ipDestination = ipDestination;
        this.dportIcode = destinationPort;

        this.msg = msg;
        this.classtype = classtype;
        this.category = category;
        this.ruleId = ruleId;
        this.protocol = protocol;
        
        this.blocked = blocked;

    }

    public long getTimestamp() { return this.timestamp; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }

    public long getSignatureId() { return this.signatureId; }
    public void setSignatureId(long signatureId) { this.signatureId = signatureId; }

    public long getGeneratorId() { return this.generatorId; }
    public void setGeneratorId(long generatorId) { this.generatorId = generatorId; }

    public InetAddress getIpSource() { return this.ipSource; }
    public void setIpSource( InetAddress ipSource) { this.ipSource = ipSource; }

    public InetAddress getIpDestination() { return this.ipDestination; }
    public void setIpDestination( InetAddress ipDestination) { this.ipDestination = ipDestination; }

    public int getSportItype() { return this.sportItype; }
    public void setSportItype(int sportItype) { this.sportItype = sportItype; }

    public int getDportIcode() { return this.dportIcode; }
    public void setDportIcode(int dportIcode) { this.dportIcode = dportIcode; }

    public String getProtocol() { return this.protocol; }
    public void setProtocol(String protocol) { this.protocol = protocol; }

    public boolean getBlocked() { return this.blocked; }
    public void setBlocked(boolean blocked) { this.blocked = blocked; }

    public String getMsg() { return this.msg; }
    public void setMsg( String msg) { this.msg = msg; }

    public String getCategory() { return this.category; }
    public void setCategory( String category) { this.category = category; }

    public String getClasstype() { return this.classtype; }
    public void setClasstype( String classtype) { this.classtype = classtype; }

    public String getRuleId() { return this.ruleId; }
    public void setRuleId( String ruleId) { this.ruleId = ruleId; }

    @Override
    public void compileStatements( java.sql.Connection conn, java.util.Map<String,java.sql.PreparedStatement> statementCache ) throws Exception
    {
        Timestamp ts = new Timestamp( getTimeStamp() );

        String sql = "INSERT INTO " + schemaPrefix() + "intrusion_prevention_events" + getPartitionTablePostfix(ts) + " " +
            "( time_stamp, sig_id, gen_id, source_addr, source_port, dest_addr, dest_port, protocol_name, blocked, category, classtype, msg, rule_id)" +
            " values " +
            "( ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?); ";

        java.sql.PreparedStatement pstmt = getStatementFromCache( sql, statementCache, conn );        

        int i=0;

        pstmt.setTimestamp(++i, ts );
        pstmt.setLong(++i, getSignatureId() );
        pstmt.setLong(++i, getGeneratorId() );
        pstmt.setObject(++i, getIpSource().getHostAddress(), java.sql.Types.OTHER);
        pstmt.setInt(++i, ( getSportItype() & 0xffff ) );
        pstmt.setObject(++i, getIpDestination().getHostAddress(), java.sql.Types.OTHER);
        pstmt.setInt(++i, ( getDportIcode() & 0xffff ) );
        pstmt.setString(++i, getProtocol() );
        pstmt.setBoolean(++i, getBlocked() );

        pstmt.setString(++i, getCategory() );
        pstmt.setString(++i, getClasstype() );
        pstmt.setString(++i, getMsg() );
        pstmt.setString(++i, getRuleId() );

        pstmt.addBatch();
        return;
    }

    @Override
    public String toSummaryString()
    {
        String action;
        if ( getBlocked() )
            action = I18nUtil.marktr("blocked");
        else
            action = I18nUtil.marktr("detected");
        String summary = "Intrusion Prevention" + " " + action;
        return summary;
    }
        
}
