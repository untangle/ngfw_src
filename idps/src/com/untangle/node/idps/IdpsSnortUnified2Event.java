/*
 * $Id: IdpsUnified2Event.java 31685 2014-11-24 15:50:30Z cblaise $
 */
package com.untangle.node.idps;

import java.net.InetAddress;


class IdpsSnortUnified2IdsEvent {
    
	private long eventType;
    
	private long sensorId;
	private long eventId;
	private long eventSecond;
	private long eventMicrosecond;
	private long signatureId;
	private long generatorId;
	private long signatureRevision;
	private long classificationId;
	private long priorityId;
	private InetAddress ipSource;
	private InetAddress ipDestination;
	private int sportItype;
	private int dportIcode;
	private short protocol;
	private short impactFlag;
	private short impact;
	private short blocked;
    private long mplsLabel;
    private int vlanId;
    private int padding;

    private String msg;
    private String classtype;
    private String category;
	
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
	
	public short getBlocked() { return this.blocked; }
	public void setBlocked(short blocked) { this.blocked = blocked; }
    
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
	
	public void clear() {
	    this.eventType = 0;
	    this.sensorId = 0;
	    this.eventId = 0;
	    this.eventSecond = 0;
	    this.eventMicrosecond = 0;
	    this.signatureId = 0;
	    this.generatorId = 0;
	    this.signatureRevision = 0;
	    this.classificationId = 0;
	    this.priorityId = 0;
	    this.ipSource = null;
	    this.ipDestination = null;
	    this.sportItype = 0;
	    this.dportIcode = 0;
	    this.protocol = 0;
	    this.impactFlag = 0;
	    this.impact = 0;
	    this.blocked = 0;
        this.mplsLabel = 0;
        this.vlanId = 0;
        this.padding = 0;

        this.msg = "";
        this.classtype = "";
        this.category = "";
	}
}
