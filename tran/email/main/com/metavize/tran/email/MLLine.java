/*
 * Copyright (c) 2004 Metavize Inc.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Metavize Inc. ("Confidential Information").  You shall
 * not disclose such Confidential Information.
 *
 * $Id: MLLine.java,v 1.5 2005/03/17 02:18:58 cng Exp $
 */
package com.metavize.tran.email;

import java.nio.*;
import java.util.*;
import java.util.regex.*;
import org.apache.log4j.Logger;

import com.metavize.tran.util.*;

public class MLLine
{
    /* constants */
    private static final Logger zLog = Logger.getLogger(MLLine.class.getName());

    /* class variables */

    /* instance variables */

    /* constructors */
    private MLLine() {}

    /* public methods */
    /* copy src list of buffers or text lines into dst list of text lines */
    public static void fromBuffer(ArrayList zSrcList, ArrayList zDstList, boolean bSrcByteBuffer)
    {
        if (true == bSrcByteBuffer)
        {
            CBufferWrapper zCLine;
            ByteBuffer zLine;
            Iterator zDstIter;
            Iterator zSrcIter;

            /* renew CBufferWrappers with new ByteBuffers */
            for (zDstIter = zDstList.iterator(), zSrcIter = zSrcList.iterator(); true == zDstIter.hasNext() && true == zSrcIter.hasNext(); )
            {
                zCLine = (CBufferWrapper) zDstIter.next();
                zLine = (ByteBuffer) zSrcIter.next();
                zCLine.renew(zLine);
                //zLog.debug("src line (replace): " + zCLine + ", " + zLine);
            }

            /* remove extra CBufferWrappers */
            for (; true == zDstIter.hasNext() && false == zSrcIter.hasNext(); )
            {
                zDstIter.next();
                zDstIter.remove();
                //zLog.debug("src line (remove)");
            }

            /* add new CBufferWrappers */
            for ( ; true == zSrcIter.hasNext(); )
            {
                zLine = (ByteBuffer) zSrcIter.next();
                zCLine = new CBufferWrapper(zLine);
                zDstList.add(zCLine);
                //zLog.debug("src line (add): " + zCLine + ", " + zLine);
            }
        }
        else
        {
            zDstList.clear();
            zDstList.addAll(zSrcList);
        }

        return;
    }


    /* copy src list of text lines into new list of buffers or text lines
     * - we do not release src list objects
     */
    public static ArrayList toBuffer(ArrayList zSrcList, boolean bSrcByteBuffer)
    {
        if (true == bSrcByteBuffer)
        {
            ArrayList zNewList = new ArrayList(zSrcList.size());

            CBufferWrapper zCLine;
            ByteBuffer zLine;

            for (Iterator zSrcIter = zSrcList.iterator(); true == zSrcIter.hasNext(); )
            {
                zCLine = (CBufferWrapper) zSrcIter.next();
                zLine = zCLine.get();
                zCLine.renew(null);
                zNewList.add(zLine);
            }

            return zNewList; /* return new list of ByteBuffers */
        }

        return zSrcList; /* return original list of CBufferWrappers */
    }

    /* copy and break list of buffers into list of individual text lines
     * (disassemble buffers into text lines)
     */
    public static ArrayList toTextNotUsed(ArrayList zList, Pattern zEOLPattern)
    {
        Matcher zMatcher;
        CBufferWrapper zCLine;
        CBufferWrapper zTmpCLine;
        ByteBuffer zLine;
        ByteBuffer zTmpLine;
        byte azBytes[];
        byte azTmpBytes[];
        int iPosition;
        int iLimit;
        int iStart;
        int iEnd;
        int iIdx;
        int iTmpIdx;
        boolean bFoundEOL;
        boolean bRelease;

        ArrayList zNewList = new ArrayList(zList.size());

        for (Iterator zIter = zList.iterator(); true == zIter.hasNext(); )
        {
            zCLine = (CBufferWrapper) zIter.next();
            zLine = zCLine.get();
            //zLog.debug("original buffer: " + zCLine + ", " + zLine);

            azBytes = null;
            iStart = 0;
            bFoundEOL = false;
            bRelease = false;

            zMatcher = zEOLPattern.matcher(zCLine);
            while (true == zMatcher.find())
            {
                iEnd = zMatcher.end();
                bFoundEOL = true;

                if (0 == iStart &&
                    zCLine.length() == iEnd)
                {
                    /* buffer only contains single text line - keep as is */
                    zNewList.add(zCLine);

                    //zLog.debug("reuse original buffer (complete)");
                    break; /* while loop */
                }
                else
                {
                    /* buffer contains multiple text lines - break up buffer */

                    if (null == azBytes)
                    {
                        /* create snapshot of buffer */
                        iPosition = zLine.position();
                        iLimit = zLine.limit();

                        /* copy contents of buffer to byte array */
                        azBytes = new byte[iPosition];
                        zLine.rewind();
                        zLine.get(azBytes, 0, iPosition);

                        /* restore buffer state */
                        zLine.limit(iLimit);
                        zLine.position(iPosition);

                        bRelease = true;
                    }

                    /* copy bytes up to this EOL from current to new buffer */
                    azTmpBytes = new byte[iEnd - iStart];
                    for (iTmpIdx = 0, iIdx = iStart; iEnd > iIdx; iTmpIdx++, iIdx++)
                    {
                        azTmpBytes[iTmpIdx] = azBytes[iIdx];
                    }

                    zTmpLine = ByteBuffer.wrap(azTmpBytes);
                    zTmpLine.position(azTmpBytes.length);
                    zTmpCLine = new CBufferWrapper(zTmpLine);
                    zNewList.add(zTmpCLine);

                    iStart = iEnd;

                    zLog.debug("break buffer (partial): " + zTmpCLine + ", " + zTmpLine);
                    /* fall through (and continue) */
                }
            }

            if (false == bFoundEOL)
            {
                /* buffer contains single text line w/no EOL - keep as is */
                zNewList.add(zCLine);

                zLog.debug("reuse original buffer (no EOL)");
            }

            if (true == bRelease)
            {
                /* release backing buffer; let GC process it */
                zCLine.renew(null);
                /* we recycle text buffers so we don't clear original list */
            }
        }

        return zNewList; /* return new list of CBufferWrappers */
    }

    /* combine list of text lines into list of buffers
     * (reassemble text lines into buffers)
     */
    public static ArrayList toBufferNotUsed(ArrayList zList, boolean bByteBuffer)
    {
        CBufferWrapper zCLine;
        ByteBuffer zLine;
        Iterator zIter;

        ArrayList zNewList = new ArrayList();
        ByteBuffer zNewLine = ByteBuffer.allocate(MLHandler.READSZ);
        zNewList.add(zNewLine);

        for (zIter = zList.iterator(); true == zIter.hasNext(); )
        {
            zCLine = (CBufferWrapper) zIter.next();
            zLine = zCLine.get();
            zLine.rewind();

            try
            {
                zNewLine.put(zLine);
            }
            catch (BufferOverflowException e)
            {
                zNewLine.limit(zNewLine.position()); /* we set limit since we didn't allocate buffer to exact size */

                /* out of space - add this text line to new buffer */
                zNewLine = ByteBuffer.allocate(MLHandler.READSZ);
                zNewLine.put(zLine);

                zNewList.add(zNewLine);
                continue; /* for loop */
            }
        }

        if (true == bByteBuffer)
        {
            return zNewList; /* return new list of ByteBuffers */
        }
        /* else return original list of CBufferWrappers
         * (modified and renewed with updated ByteBuffers)
         */

        Iterator zNewIter;

        /* swap original ByteBuffers, that back CBufferWrappers,
         * with new ByteBuffers
         */
        for (zIter = zList.iterator(), zNewIter = zNewList.iterator(); true == zNewIter.hasNext(); )
        {
            zNewLine = (ByteBuffer) zNewIter.next();
            zCLine = (CBufferWrapper) zIter.next();
            zCLine.renew(zNewLine);
        }

        /* remove extra CBufferWrappers from original list */
        for ( ; true == zIter.hasNext(); )
        {
            zIter.next();
            zIter.remove();
        }

        return zList; /* return original list of CBufferWrappers */
    }

    /* private methods */
}
