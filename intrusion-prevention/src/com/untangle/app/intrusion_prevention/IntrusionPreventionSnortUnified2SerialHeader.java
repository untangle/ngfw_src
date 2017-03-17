/*
 * $Id: IntrusionPreventionUnified2SerialHeader.java 31685 2014-11-24 15:50:30Z cblaise $
 */
package com.untangle.app.intrusion_prevention;

class IntrusionPreventionSnortUnified2SerialHeader {
    public static final int TYPE_PACKET = 2;
    public static final int TYPE_IDS_EVENT = 7;
    public static final int TYPE_IDS_EVENT_IPV6 = 72;
    public static final int TYPE_IDS_EVENT_V2 = 104;
    public static final int TYPE_IDS_EVENT_V2_IPV6 = 105;
    public static final int TYPE_EXTRA_DATA = 110;

	private long length;
	private long type;
	
	public long getType() { return this.type; }
	public void setType(long type) { this.type = type; }

	public long getLength() { return this.length; }

	public void setLength(long length) { this.length = length; }
	
	public void clear() {
		this.length = 0;
		this.type = 0;
	}
}
