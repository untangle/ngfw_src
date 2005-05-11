/*
 * Copyright (c) 2004 Metavize Inc.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Metavize Inc. ("Confidential Information").  You shall
 * not disclose such Confidential Information.
 *
 * $Id$
 */
package com.metavize.tran.email;

import java.lang.InterruptedException;
import java.nio.*;
import java.util.*;
import java.util.regex.*;

import com.metavize.mvvm.tapi.event.*;
import com.metavize.tran.util.CBufferWrapper;

/* driver =  endpoint on this side of bookend drives protocol
 * (e.g., issues cmds and receives replies)
 * passenger = endpoint on this side of bookend waits for driver
 * (e.g., receives cmds and sends replies)
 */
public class XMSEnv
{
    /* constants */

    /* class variables */

    /* instance variables */
    private MLHandler zHandler = null;
    private MLMessage zMsg = null;
    private CBufferWrapper zReadCLine = null; /* most recently read line */
    private IPDataResult zFixedResult = null; /* return this fixed result */
    /* to-driver queue - e-mail client = MUA - not always not Smith client
     * to-passenger queue - e-mail server = MTA - not always Smith server
     */
    private ArrayList zToDrivers; /* array of ByteBuffers */
    private ArrayList zToPassengers; /* array of ByteBuffers */

    private int iReadDataCt; /* running count - # of message data bytes */

    private boolean bException;

    /* constructors */
    public XMSEnv()
    {
        zToDrivers = new ArrayList();
        zToPassengers = new ArrayList();

        clearReadDataCt();

        bException = false;
    }

    /* public methods */
    public synchronized void setHandler(MLHandler zHandler)
    {
        this.zHandler = zHandler;
        return;
    }

    public void setMsg(MLMessage zMsg)
    {
        this.zMsg = zMsg;
        return;
    }

    /* reset read text line
     * - release backing buffer (private copy - let GC process it)
     * - we no longer need contents of backing buffer
     */
    public void resetReadCLine()
    {
        if (null == zReadCLine)
        {
            return;
        }

        zReadCLine.renew(null);
        return;
    }

    /* clear read text line
     * - we've saved/buffered current text line (with its backing buffer)
     * - by clearing read text line,
     *   next readline will create new text line (for new backing buffer)
     */
    public void clearReadCLine()
    {
        zReadCLine = null;
        return;
    }

    /* set new read text line with new backing buffer */
    public void setReadCLine(CBufferWrapper zReadCLine)
    {
        this.zReadCLine = zReadCLine;
        return;
    }

    public void setFixedResult(IPDataResult zFixedResult)
    {
        this.zFixedResult = zFixedResult;
        return;
    }

    /* queue this result line to send to driver */
    public void sendToDriver(ByteBuffer zLine)
    {
        zToDrivers.add(zLine);
        return;
    }

    /* queue these result lines to send to driver */
    public void sendToDriver(ArrayList zResults)
    {
        zToDrivers.addAll(zResults);
        return;
    }

    /* queue this result line to send to driver */
    public void convertToDriver(CBufferWrapper zCLine)
    {
        zToDrivers.add(zCLine.get());
        return;
    }

    /* queue these result lines to send to driver */
    public void convertToDriver(ArrayList zResults)
    {
        CBufferWrapper zCLine;

        for (Iterator zIter = zResults.iterator(); true == zIter.hasNext(); )
        {
            zCLine = (CBufferWrapper) zIter.next();
            zToDrivers.add(zCLine.get());
        }
        return;
    }

    /* queue this result line to send to passenger */
    public void sendToPassenger(ByteBuffer zLine)
    {
        zToPassengers.add(zLine);
        return;
    }

    /* queue these result lines to send to passenger */
    public void sendToPassenger(ArrayList zResults)
    {
        zToPassengers.addAll(zResults);
        return;
    }

    /* queue this result line to send to passenger */
    public void convertToPassenger(CBufferWrapper zCLine)
    {
        zToPassengers.add(zCLine.get());
        return;
    }

    /* queue these result lines to send to passenger */
    public void convertToPassenger(ArrayList zResults)
    {
        CBufferWrapper zCLine;

        for (Iterator zIter = zResults.iterator(); true == zIter.hasNext(); )
        {
            zCLine = (CBufferWrapper) zIter.next();
            zToPassengers.add(zCLine.get());
        }
        return;
    }

    public void clearReadDataCt()
    {
        iReadDataCt = 0;
        return;
    }

    public void incrementReadDataCt(int iCt)
    {
        iReadDataCt += iCt;
        return;
    }

    public synchronized void setException(boolean bException)
    {
        this.bException = bException;
        return;
    }

    public synchronized MLHandler getHandler()
    {
        return zHandler;
    }

    public MLMessage getMsg()
    {
        return zMsg;
    }

    public CBufferWrapper getReadCLine()
    {
        return zReadCLine;
    }

    public IPDataResult getFixedResult()
    {
        return zFixedResult;
    }

    public ArrayList getToDriver()
    {
        return zToDrivers;
    }

    public ArrayList getToPassenger()
    {
        return zToPassengers;
    }

    public int getReadDataCt()
    {
        return iReadDataCt;
    }

    public synchronized boolean getException()
    {
        return bException;
    }

    /* private methods */
}
