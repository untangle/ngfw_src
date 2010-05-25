package com.untangle.node.cpd;

import java.io.Serializable;
import java.net.InetAddress;
import java.util.Date;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;

import org.hibernate.annotations.Type;

@Entity
@Table(name="n_cpd_host_database_entry", schema="events")
public class HostDatabaseEntry implements Serializable
{
    private static final long serialVersionUID = 42L;

    private Long id;
    private String hardwareAddress;
    private InetAddress ipv4Address;
    private String username;
    private Date lastSession;
    private Date sessionStart;
    private Date expirationDate;
    
    public HostDatabaseEntry()
    {
    }
    
    @SuppressWarnings("unused")
    @Id
    @Column(name="entry_id")
    @GeneratedValue
    private Long getId()
    {
        return id;
    }

    @SuppressWarnings("unused")
    private void setId(Long id)
    {
        this.id = id;
    }

    @Column(name="hw_addr")
    public String getHardwareAddress() {
        return hardwareAddress;
    }
    
    public void setHardwareAddress(String hardwareAddress) {
        this.hardwareAddress = hardwareAddress;
    }

    @Column(name="ipv4_addr")
    @Type(type="com.untangle.uvm.type.InetAddressUserType")
    public InetAddress getIpv4Address() {
        return ipv4Address;
    }
    
    public void setIpv4Address(InetAddress ipv4Address) {
        this.ipv4Address = ipv4Address;
    }

    @Column(name="username")
    public String getUsername() {
        return username;
    }
    
    public void setUsername(String username) {
        this.username = username;
    }

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name="last_session")
    public Date getLastSession() {
        return lastSession;
    }

     public void setLastSession(Date lastSession) {
        this.lastSession = lastSession;
    }

   @Temporal(TemporalType.TIMESTAMP)
    @Column(name="session_start")
    public Date getSessionStart() {
        return sessionStart;
    }
    
    public void setSessionStart(Date sessionStart) {
        this.sessionStart = sessionStart;
    }
    
    @Temporal(TemporalType.TIMESTAMP)
    @Column(name="expiration_date")
    public Date getExpirationDate() {
        return expirationDate;
    }
    
    public void setExpirationDate(Date expirationDate) {
        this.expirationDate = expirationDate;
    }

}
