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

import com.untangle.uvm.node.Rule;
import com.untangle.uvm.node.IntfMatcher;
import com.untangle.uvm.node.IPMatcher;

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
   
    public boolean getCapture()
    {
        return this.capture;
    }

    public void setCapture( boolean newValue )
    {
        this.capture = newValue;
    }

    public IntfMatcher getClientInterface()
    {
        return this.clientInterface;
    }

    public void setClientInterface( IntfMatcher newValue )
    {
        this.clientInterface = newValue;
    }

    public IPMatcher getClientAddress()
    {
        return this.clientAddress;
    }

    public void setClientAddress( IPMatcher newValue )
    {
        this.clientAddress = newValue;
    }

    public IPMatcher getServerAddress()
    {
        return this.serverAddress;
    }

    public void setServerAddress( IPMatcher newValue )
    {
        this.serverAddress = newValue;
    }

    public String getStartTime()
    {
        return this.startTime;
    }

    public void setStartTime( String newValue )
    {
        this.startTime = newValue;
    }

    public String getEndTime()
    {
        return this.endTime;
    }

    public void setEndTime( String newValue )
    {
        this.endTime = newValue;
    }

    public String getDays()
    {
        return this.days;
    }

    public void setDays( String newValue )
    {
        this.days = newValue;
    }
}
