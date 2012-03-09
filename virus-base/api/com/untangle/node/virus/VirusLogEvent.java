/**
 * $Id$
 */
package com.untangle.node.virus;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.persistence.Transient;

import org.hibernate.annotations.Columns;
import org.hibernate.annotations.Type;

import com.untangle.uvm.logging.LogEvent;
import com.untangle.uvm.logging.SyslogBuilder;
import com.untangle.uvm.logging.SyslogPriority;
import com.untangle.uvm.node.SessionEvent;

/**
 * Log for FTP virus events.
 */
@Entity
@org.hibernate.annotations.Entity(mutable=false)
@Table(name="n_virus_evt", schema="events")
@SuppressWarnings("serial")
public class VirusLogEvent extends LogEvent
{
    private SessionEvent sessionEvent;
    private VirusScannerResult result;
    private String vendorName;

    public VirusLogEvent() { }

    public VirusLogEvent(SessionEvent pe, VirusScannerResult result, String vendorName)
    {
        this.sessionEvent = pe;
        this.result = result;
        this.vendorName = vendorName;
    }

    @Transient
    public String getType()
    {
        return "FTP";
    }

    @Transient
    public String getLocation()
    {
        return null == sessionEvent ? "" : sessionEvent.getSServerAddr().getHostAddress();
    }

    @Transient
    public boolean isInfected()
    {
        return !result.isClean();
    }

    @Transient
    public String getVirusName()
    {
        String n = result.getVirusName();

        return null == n ? "" : n;
    }

    /**
     * Get the session Id
     *
     * @return the the session Id
     */
    @Column(name="session_id", nullable=false)
    public Long getSessionId()
    {
        return sessionEvent.getSessionId();
    }

    public void setSessionId( Long sessionId )
    {
        this.sessionEvent.setSessionId(sessionId);
    }

    @Transient
    public SessionEvent getSessionEvent()
    {
        return sessionEvent;
    }

    public void setSessionEvent(SessionEvent sessionEvent)
    {
        this.sessionEvent = sessionEvent;
    }

    /**
     * Virus scan result.
     *
     * @return the scan result.
     */
    @Columns(columns = {
    @Column(name="clean"),
    @Column(name="virus_name"),
    @Column(name="virus_cleaned")})
    @Type(type="com.untangle.node.virus.VirusScannerResultUserType")
    public VirusScannerResult getResult()
    {
        return result;
    }

    public void setResult(VirusScannerResult result)
    {
        this.result = result;
    }

    /**
     * Spam scanner vendor.
     *
     * @return the vendor
     */
    @Column(name="vendor_name")
    public String getVendorName()
    {
        return vendorName;
    }

    public void setVendorName(String vendorName)
    {
        this.vendorName = vendorName;
    }

    public void appendSyslog(SyslogBuilder sb)
    {
        SessionEvent pe = getSessionEvent();
        if (null != pe) {
            pe.appendSyslog(sb);
        }

        sb.startSection("info");
        sb.addField("location", getLocation());
        sb.addField("infected", isInfected());
        sb.addField("virus-name", getVirusName());
    }
}
