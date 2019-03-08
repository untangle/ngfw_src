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
    private long eventType = 0;

    private long sensorId = 0;
    private long eventId = 0;
    private long eventSecond = 0;
    private long eventMicrosecond = 0;
    private long signatureId = 0;
    private long generatorId = 0;
    private long signatureRevision = 0;
    private long classificationId = 0;
    private long priorityId = 0;
    private InetAddress ipSource = null;
    private InetAddress ipDestination = null;
    private int sportItype = 0;
    private int dportIcode = 0;
    private short protocol = 0;
    private short impactFlag = 0;
    private short impact = 0;
    private boolean blocked = false;
    private long mplsLabel = 0;
    private int vlanId = 0;
    private int padding = 0;

    private String msg = "";
    private String classtype = "";
    private String category = "";
    private String rid = "";

    public IntrusionPreventionLogEvent( ) {}

    public long getEventType() { return this.eventType; }
    public void setEventType(long eventType) { this.eventType = eventType; }

    public long getSensorId() { return this.sensorId; }
    public void setSensorId(long sensorId) { this.sensorId = sensorId; }

    public long getEventId() { return this.eventId; }
    public void setEventId(long eventId) { this.eventId = eventId; }

    public long getEventSecond() { return this.eventSecond; }
    public void setEventSecond(long eventSecond) { this.eventSecond = eventSecond; }

    public long getEventMicrosecond() { return this.eventMicrosecond; }
    public void setEventMicrosecond(long eventMicrosecond) { this.eventMicrosecond = eventMicrosecond; }

    public long getSignatureId() { return this.signatureId; }
    public void setSignatureId(long signatureId) { this.signatureId = signatureId; }

    public long getGeneratorId() { return this.generatorId; }
    public void setGeneratorId(long generatorId) { this.generatorId = generatorId; }

    public long getSignatureRevision() { return this.signatureRevision; }
    public void setSignatureRevision(long signatureRevision) { this.signatureRevision = signatureRevision; }

    public long getClassificationId() { return this.classificationId; }
    public void setClassificationId(long classificationId) { this.classificationId = classificationId; }

    public long getPriorityId() { return this.priorityId; }
    public void setPriorityId(long priorityId) { this.priorityId = priorityId; }

    public InetAddress getIpSource() { return this.ipSource; }
    public void setIpSource( InetAddress ipSource) { this.ipSource = ipSource; }

    public InetAddress getIpDestination() { return this.ipDestination; }
    public void setIpDestination( InetAddress ipDestination) { this.ipDestination = ipDestination; }

    public int getSportItype() { return this.sportItype; }
    public void setSportItype(int sportItype) { this.sportItype = sportItype; }

    public int getDportIcode() { return this.dportIcode; }
    public void setDportIcode(int dportIcode) { this.dportIcode = dportIcode; }

    public short getProtocol() { return this.protocol; }
    public void setProtocol(short protocol) { this.protocol = protocol; }

    public short getImpactFlag() { return this.impactFlag; }
    public void setImpactFlag(short impactFlag) { this.impactFlag = impactFlag; }

    public short getImpact() { return this.impact; }
    public void setImpact(short impact) { this.impact = impact; }

    public boolean getBlocked() { return this.blocked; }
    public void setBlocked(boolean blocked) { this.blocked = blocked; }

    public long getMplsLabel() { return this.mplsLabel; }
    public void setMplsLabel( long mplsLabel) { this.mplsLabel = mplsLabel; }

    public int getVlanId() { return this.vlanId; }
    public void setVlanId( int vlanId ) { this.vlanId = vlanId; }

    public int getPadding() { return this.padding; }
    public void setPadding( int padding ) { this.padding = padding; }

    public String getMsg() { return this.msg; }
    public void setMsg( String msg) { this.msg = msg; }

    public String getCategory() { return this.category; }
    public void setCategory( String category) { this.category = category; }

    public String getClasstype() { return this.classtype; }
    public void setClasstype( String classtype) { this.classtype = classtype; }

    public String getRid() { return this.rid; }
    public void setRid( String rid) { this.rid = rid; }

    @Override
    public void compileStatements( java.sql.Connection conn, java.util.Map<String,java.sql.PreparedStatement> statementCache ) throws Exception
    {
        Timestamp ts = new Timestamp( ( getEventSecond() * 1000 ) + ( getEventMicrosecond() / 1000 ) );

        String sql = "INSERT INTO " + schemaPrefix() + "intrusion_prevention_events" + getPartitionTablePostfix(ts) + " " +
            "( time_stamp, sig_id, gen_id, class_id, source_addr, source_port, dest_addr, dest_port, protocol, blocked, category, classtype, msg, rule_id)" +
            " values " +
            "( ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ? ); ";

        java.sql.PreparedStatement pstmt = getStatementFromCache( sql, statementCache, conn );        

        int i=0;

        pstmt.setTimestamp(++i, ts );
        pstmt.setLong(++i, getSignatureId() );
        pstmt.setLong(++i, getGeneratorId() );
        pstmt.setLong(++i, getClassificationId() );
        pstmt.setObject(++i, getIpSource().getHostAddress(), java.sql.Types.OTHER);
        pstmt.setInt(++i, ( getSportItype() & 0xffff ) );
        pstmt.setObject(++i, getIpDestination().getHostAddress(), java.sql.Types.OTHER);
        pstmt.setInt(++i, ( getDportIcode() & 0xffff ) );
        pstmt.setInt(++i, getProtocol() );
        pstmt.setBoolean(++i, getBlocked() );

        pstmt.setString(++i, getCategory() );
        pstmt.setString(++i, getClasstype() );
        pstmt.setString(++i, getMsg() );
        pstmt.setString(++i, getRid() );

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
