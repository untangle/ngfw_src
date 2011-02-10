/*
 * $HeadURL$
 * Copyright (c) 2003-2007 Untangle, Inc.
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

package com.untangle.node.cpd;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Table;

import org.hibernate.annotations.Type;

import com.untangle.uvm.node.Rule;
import com.untangle.uvm.node.firewall.intf.IntfMatcher;
import com.untangle.uvm.node.firewall.ip.IPMatcher;

@Entity
@Table(name="n_cpd_capture_rule", schema="settings")
@SuppressWarnings("serial")
public class CaptureRule extends Rule
{
    
    public static final String START_OF_DAY = "00:00";
    public static final String END_OF_DAY = "23:59";
    public static final String ALL_DAYS = "mon,tue,wed,thu,fri,sat,sun";
    
    private boolean capture = true;
    private IntfMatcher clientInterface = IntfMatcher.getAnyMatcher();
    private IPMatcher clientAddress = IPMatcher.getAnyMatcher();
    private IPMatcher serverAddress = IPMatcher.getAnyMatcher();
    
    String startTime = START_OF_DAY;
    String endTime = END_OF_DAY;
    
    String days = ALL_DAYS;

    public CaptureRule()
    {
    }
	
	public CaptureRule( boolean live, boolean capture, String description, 
	        IntfMatcher clientInterface, IPMatcher clientAddress, IPMatcher serverAddress,
	        String startTime, String endTime, String days )
	{
	    setLive(live);
	    setDescription(description);
	    
	    this.capture = capture;
	    this.clientInterface = clientInterface;
	    this.clientAddress = clientAddress;
	    this.serverAddress = serverAddress;
	    this.startTime = startTime;
	    this.endTime = endTime;
	    this.days = days;
	}
   
    @Column(name="capture_enabled", nullable=false)
    public boolean getCapture()
    {
        return this.capture;
    }

    public void setCapture( boolean newValue )
    {
        this.capture = newValue;
    }

    @Column(name="client_interface", nullable=false)
    @Type(type="com.untangle.uvm.type.firewall.IntfMatcherUserType")
    public IntfMatcher getClientInterface()
    {
        return this.clientInterface;
    }

    public void setClientInterface( IntfMatcher newValue )
    {
        this.clientInterface = newValue;
    }

    @Column(name="client_address", nullable=false)
    @Type(type="com.untangle.uvm.type.firewall.IPMatcherUserType")
    public IPMatcher getClientAddress()
    {
        return this.clientAddress;
    }

    public void setClientAddress( IPMatcher newValue )
    {
        this.clientAddress = newValue;
    }

    @Column(name="server_address", nullable=false)
    @Type(type="com.untangle.uvm.type.firewall.IPMatcherUserType")
    public IPMatcher getServerAddress()
    {
        return this.serverAddress;
    }

    public void setServerAddress( IPMatcher newValue )
    {
        this.serverAddress = newValue;
    }

    @Column(name="start_time", nullable=false)
    public String getStartTime()
    {
        return this.startTime;
    }

    public void setStartTime( String newValue )
    {
        this.startTime = newValue;
    }

    @Column(name="end_time", nullable=false)
    public String getEndTime()
    {
        return this.endTime;
    }

    public void setEndTime( String newValue )
    {
        this.endTime = newValue;
    }

    @Column(name="days", nullable=false)
    public String getDays()
    {
        return this.days;
    }

    public void setDays( String newValue )
    {
        this.days = newValue;
    }
}
