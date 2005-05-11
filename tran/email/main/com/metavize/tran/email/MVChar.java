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
import org.apache.log4j.Logger;

import com.metavize.tran.util.CBufferWrapper;

public class MVChar
{
    /* constants */
    private static final Logger zLog = Logger.getLogger(MVChar.class);

    /* class variables */

    /* instance variables */
    private static CBufferWrapper zCDummy; /* for temp use only */

    /* constructors */
    private MVChar() {}

    /* public methods */
    public static String stripNonASCII(ByteBuffer zLine)
    {
        ByteBuffer zASCIILine = ByteBuffer.allocate(zLine.limit());

        /* create snapshot of ByteBuffer state */
        int iPosition = zLine.position();
        int iLimit = zLine.limit();

        byte bNext;

        while (false != zLine.hasRemaining())
        {
            bNext = zLine.get();
            if (0 > bNext || 127 < bNext)
            {
                continue;
            }

            zASCIILine.put(bNext);
        }

        /* restore ByteBuffer state */
        zLine.limit(iLimit);
        zLine.position(iPosition);

        iPosition = zASCIILine.position();
        if (0 == iPosition)
        {
            zASCIILine = null; /* release, let GC process */
            return Constants.EMPTYSTR;
        }

        zASCIILine.limit(iPosition); /* we set limit since we may not have allocated buffer to exact size */
        if (null == zCDummy)
        {
            zCDummy = new CBufferWrapper(zASCIILine);
        }
        else
        {
            zCDummy.renew(zASCIILine);
        }
        String zStr = zCDummy.toString().trim();
        zASCIILine = null; /* release, let GC process */
        return zStr;
    }

    /* private methods */
}
