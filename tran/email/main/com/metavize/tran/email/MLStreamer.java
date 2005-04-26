/*
 * Copyright (c) 2005 Metavize Inc.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Metavize Inc. ("Confidential Information").  You shall
 * not disclose such Confidential Information.
 *
 * $Id: $
 */
package com.metavize.tran.email;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.ListIterator;
import org.apache.log4j.Logger;

import com.metavize.mvvm.tapi.event.TCPStreamer;

public class MLStreamer implements TCPStreamer
{
    /* constants */
    private final Logger zLog = Logger.getLogger(MLStreamer.class.getName());

    /* class variables */

    /* instance variables */
    private ArrayList zList;
    private ListIterator zLIter = null;
    private int iSz = 0;

    /* constructors */
    public MLStreamer(ArrayList zList)
    {
        this.zList = zList;
        if (null != zList)
        {
            zLIter = zList.listIterator();
        }
    }

    /* public methods */
    public ByteBuffer nextChunk()
    {
        if (null == zLIter)
        {
            zLog.debug("stream: empty");
            return null;
        }

        if (true == zLIter.hasNext())
        {
            ByteBuffer zLine = (ByteBuffer) zLIter.next();
            iSz += zLine.limit();
            //zLog.debug("stream: " + zLine);
            zLIter.set(null); /* release reference; let GC process it */
            return zLine;
        }

        zLog.debug("stream: sent " + iSz + " bytes, " + zList.size() + " lines");
        zList.clear(); /* release; let GC process */
        zList = null; /* release; let GC process */
        zLIter = null; /* release; let GC process */
        return null;
    }

    public boolean closeWhenDone()
    {
        zLog.debug("stream: done (close = false)");
        return false; /* never close write socket queue */
    }

    /* private methods */
}
