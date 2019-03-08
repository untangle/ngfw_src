/**
 * $Id: IntrusionPreventionUnified2SerialHeader.java 31685 2014-11-24 15:50:30Z cblaise $
 */
package com.untangle.app.intrusion_prevention;

/**
 * Snort Unified 2 serial header
 */
class IntrusionPreventionSnortUnified2SerialHeader {
    public static final int TYPE_PACKET = 2;
    public static final int TYPE_IDS_EVENT = 7;
    public static final int TYPE_IDS_EVENT_IPV6 = 72;
    public static final int TYPE_IDS_EVENT_V2 = 104;
    public static final int TYPE_IDS_EVENT_V2_IPV6 = 105;
    public static final int TYPE_EXTRA_DATA = 110;

	private long length;
	private long type;
	
    /**
     * Return type
     * 
     * @return
     *  Type identifier.
     */
	public long getType() { return this.type; }
    /**
     * Set type
     * 
     * @param type
     *  Type identifier.
     */
	public void setType(long type) { this.type = type; }

    /**
     * Return log entry length
     * 
     * @return
     *  Long of length
     */
	public long getLength() { return this.length; }
    /**
     * Set log entry length
     * 
     * @param length
     *  Length of log entry.
     */
	public void setLength(long length) { this.length = length; }

    /**
     * Clear header.
     */	
	public void clear() {
		this.length = 0;
		this.type = 0;
	}
}
