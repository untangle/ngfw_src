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

package com.metavize.tran.spam;

import java.io.Serializable;
import java.util.Date;

import com.metavize.mvvm.tran.Direction;

public class SpamLog implements Comparable<SpamLog>, Serializable
{
    private static final long serialVersionUID = -394881366677267209L;

    public enum Action { PASS, MARK, BLOCK, QUARANTINE };

    private final Date timeStamp;
    private final float score;
    private final Action action;
    private final String subject;
    private final String receiver;
    private final String sender;
    private final String clientAddr;
    private final int clientPort;
    private final String serverAddr;
    private final int serverPort;
    private final Direction direction;

    public SpamLog(Date timeStamp, float score, String actionStr,
                   String subject, String receiver, String sender,
                   String clientAddr, int clientPort, String serverAddr,
                   int serverPort, Direction direction)
    {
        this.timeStamp = timeStamp;
        this.score = score;
        this.action = null == actionStr ? null : Action.valueOf(actionStr);
        this.subject = subject;
        this.receiver = receiver;
        this.sender = sender;
        this.clientAddr = clientAddr;
        this.clientPort = clientPort;
        this.serverAddr = serverAddr;
        this.serverPort = serverPort;
        this.direction = direction;
    }

    // accessors --------------------------------------------------------------

    public Date getTimeStamp()
    {
        return timeStamp;
    }

    public float getScore()
    {
        return score;
    }

    public Action getAction()
    {
        return action;
    }

    public String getSubject()
    {
        return subject;
    }

    public String getReceiver()
    {
        return (receiver!=null?receiver:"unknown");
    }

    public String getSender()
    {
        return sender;
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

    // Comparable methods -----------------------------------------------------

    public int compareTo(SpamLog sl)
    {
        // Sort descending instead of ascending.
        return -timeStamp.compareTo(sl.timeStamp);
    }
}
