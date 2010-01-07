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
import com.untangle.uvm.node.firewall.intf.IntfDBMatcher;
import com.untangle.uvm.node.firewall.intf.IntfSimpleMatcher;
import com.untangle.uvm.node.firewall.ip.IPDBMatcher;
import com.untangle.uvm.node.firewall.ip.IPSimpleMatcher;

@Entity
@Table(name="n_cpd_capture_rule", schema="settings")
public class CaptureRule extends Rule
{
	public CaptureRule()
    {
    }

    private boolean capture = true;
    private IntfDBMatcher clientInterface = IntfSimpleMatcher.getAllMatcher();
    private IPDBMatcher clientAddress = IPSimpleMatcher.getAllMatcher();
    private IPDBMatcher serverAddress = IPSimpleMatcher.getAllMatcher();
    
    String startTime = "00:00";
    String endTime = "11:59";
    
    String days = "mon,tue,wed,thu,fri,sat,sun";
   
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
    public IntfDBMatcher getClientInterface()
    {
        return this.clientInterface;
    }

    public void setClientInterface( IntfDBMatcher newValue )
    {
        this.clientInterface = newValue;
    }

    @Column(name="client_address", nullable=false)
    @Type(type="com.untangle.uvm.type.firewall.IPMatcherUserType")
    public IPDBMatcher getClientAddress()
    {
        return this.clientAddress;
    }

    public void setClientAddress( IPDBMatcher newValue )
    {
        this.clientAddress = newValue;
    }

    @Column(name="server_address", nullable=false)
    @Type(type="com.untangle.uvm.type.firewall.IPMatcherUserType")
    public IPDBMatcher getServerAddress()
    {
        return this.serverAddress;
    }

    public void setServerAddress( IPDBMatcher newValue )
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
