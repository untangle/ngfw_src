/*
 * Copyright (c) 2005 Metavize Inc.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Metavize Inc. ("Confidential Information").  You shall
 * not disclose such Confidential Information.
 *
 * $Id$
 */

package com.metavize.tran.virus;

import java.io.Serializable;
import java.util.Date;

import com.metavize.mvvm.tran.Direction;

public class VirusLog implements Comparable<VirusLog>, Serializable
{
    private static final long serialVersionUID = 5822180413983083372L;

    public enum Type { HTTP, FTP, MAIL };
    public enum Action { BLOCKED, PASSED, CLEANED, REMOVED };

    private final Date createDate;
    private final Type type;
    private final String location;
    private final boolean infected;
    private final Action action;
    private final String clientAddr;
    private final int clientPort;
    private final String serverAddr;
    private final int serverPort;
    private final Direction direction;

    public VirusLog(Date createDate, String type, String location,
                    boolean infected, String action,
                    String clientAddr, int clientPort, String serverAddr,
                    int serverPort, Direction direction)
    {
        this.createDate = createDate;
        this.type = Type.valueOf(type);
        this.location = location;
        this.infected = infected;
        this.action = Action.valueOf(action);
        this.clientAddr = clientAddr;
        this.clientPort = clientPort;
        this.serverAddr = serverAddr;
        this.serverPort = serverPort;
        this.direction = direction;
    }

    // util -------------------------------------------------------------------
    public String getAction(){
	if( Action.BLOCKED.equals(action) )
	    return "block";
	else if( Action.PASSED.equals(action) )
	    return "pass";
	else if( Action.CLEANED.equals(action) )
	    return "clean virus";
	else if( Action.REMOVED.equals(action) )
	    return "removed virus";
	else
	    return null;
    }

    public String getReason(){
	if(infected)
	    return "virus found";
	else
	    return "no virus found";
    }

    public String getTraffic(){
	return "(" + type.toString() + ")  " + location;
    }

    // accessors --------------------------------------------------------------

    public Date getCreateDate()
    {
        return createDate;
    }

    public Type getType()
    {
        return type;
    }

    public String getLocation()
    {
        return location;
    }

    public boolean isInfected()
    {
        return infected;
    }

    public String getClientAddr()
    {
        return clientAddr;
    }

    public int getClientPort()
    {
        return clientPort;
    }

    public String getServerAddr()
    {
        return serverAddr;
    }

    public int getServerPort()
    {
        return serverPort;
    }

    public Direction getDirection()
    {
        return direction;
    }

    // Comparable<VirusLog> methods -------------------------------------------

    public int compareTo(VirusLog vl)
    {
        return createDate.compareTo(vl.createDate);
    }
}
