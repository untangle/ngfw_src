/*
 * $HeadURL: svn://chef/work/src/webfilter-base/api/com/untangle/node/webfilter/UnblockEvent.java $
 * Copyright (c) 2003-2009 Untangle, Inc.
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License, version 2,
 * as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful, but
 * AS-IS and WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE, TITLE, or
 * NONINFRINGEMENT.  See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 */

package com.untangle.node.webfilter;

import java.net.InetAddress;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.persistence.Transient;

import org.hibernate.annotations.Type;

import com.untangle.uvm.logging.LogEvent;
import com.untangle.uvm.logging.SyslogBuilder;
import com.untangle.uvm.logging.SyslogPriority;
import com.untangle.uvm.policy.Policy;

/**
 * Log event for a blocked request.
 *
 * @author <a href="mailto:seb@untangle.com">Sebastien Delafond</a>
 * @version 1.0
 */
@Entity
@org.hibernate.annotations.Entity(mutable=false)
@Table(name="n_webfilter_evt_unblock", schema="events")
@SuppressWarnings("serial")
public class UnblockEvent extends LogEvent
{
    // action types
    private String vendorName;
	private InetAddress clientAddress;
	private boolean isPermanent;
	private String requestUri;
	private Policy policy;

    // non-persistent fields -----------------------------------------------


    // constructors --------------------------------------------------------

    public UnblockEvent() { }

    public UnblockEvent(InetAddress clientAddress,
                        boolean isPermanent,
                        String requestUri,
                        String vendorName,
                        Policy policy)
    {
	    this.clientAddress = clientAddress;
	    this.isPermanent = isPermanent;
	    this.requestUri = requestUri;
        this.vendorName = vendorName;
	    this.policy = policy;
	    //	    setTimeStamp(null);
    }

    // accessors -----------------------------------------------------------

	/**
	 * Get the <code>Policy</code> value.
	 *
	 * @return a <code>Policy</code> value
	 */
	@ManyToOne(fetch=FetchType.EAGER)
	@JoinColumn(name="policy_id")
	public Policy getPolicy() {
	    return policy;
	}

	/**
	 * Set the <code>Policy</code> value.
	 *
	 * @param newPolicy The new Policy value.
	 */
	public void setPolicy(Policy newPolicy) {
	    this.policy = newPolicy;
	}

	/**
	 * Get the <code>RequestUri</code> value.
	 *
	 * @return a <code>String</code> value
	 */
	@Column(name="request_uri")
	public String getRequestUri() {
	    return requestUri;
	}

	/**
	 * Set the <code>RequestUri</code> value.
	 *
	 * @param newRequestUri The new RequestUri value.
	 */
	public void setRequestUri(String newRequestUri) {
	    this.requestUri = newRequestUri;
	}

	/**
	 * Get the <code>IsPermanent</code> value.
	 *
	 * @return a <code>boolean</code> value
	 */
	@Column(name="is_permanent")
	public boolean isIsPermanent() {
	    return isPermanent;
	}

	/**
	 * Set the <code>IsPermanent</code> value.
	 *
	 * @param newIsPermanent The new IsPermanent value.
	 */
	public void setIsPermanent(boolean newIsPermanent) {
	    this.isPermanent = newIsPermanent;
	}

	/**
	 * Get the <code>ClientAddress</code> value.
     n	 *
	 * @return an <code>InetAddress</code> value
	 */
    @Column(name="client_address")
	@Type(type="com.untangle.uvm.type.InetAddressUserType")
	public InetAddress getClientAddress() {
	    return clientAddress;
	}

	/**
	 * Set the <code>ClientAddress</code> value.
	 *
	 * @param newClientAddress The new ClientAddress value.
	 */
	public void setClientAddress(InetAddress newClientAddress) {
	    this.clientAddress = newClientAddress;
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

    // Syslog methods ------------------------------------------------------

    public void appendSyslog(SyslogBuilder sb)
    {
        sb.startSection("info");
        sb.addField("url", requestUri);
        sb.addField("client-addr", null == clientAddress ? "none" : clientAddress.toString());
        sb.addField("create-date", null == getTimeStamp() ? "none" : getTimeStamp().toString());
    }

    @Transient
    public String getSyslogId()
    {
        return "Unblock";
    }

    @Transient
    public SyslogPriority getSyslogPriority()
    {
	    return SyslogPriority.INFORMATIONAL;
    }

    // Object methods ------------------------------------------------------

    public String toString()
    {
        return "UnblockEvent id: " + getId() + " RequestUri: "
            + requestUri + "ClientAddress: " + clientAddress
            + "Permanent: " + isPermanent;
    }
}
