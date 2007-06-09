/*
 * Copyright (c) 2003-2007 Untangle, Inc.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Untangle, Inc. ("Confidential Information"). You shall
 * not disclose such Confidential Information.
 *
 * $Id$
 */

package com.untangle.node.mail.papi.pop;

import java.io.IOException;
import java.nio.ByteBuffer;

import com.untangle.node.token.Token;
import com.untangle.node.util.AsciiCharBuffer;
import org.apache.log4j.Logger;
import sun.misc.BASE64Decoder;

public class PopCommandMore implements Token
{
    private final static String NO_USER = "unknown";

    private final ByteBuffer zBuf;
    private final String zUser;

    // constructors -----------------------------------------------------------

    public PopCommandMore(ByteBuffer zBuf)
    {
        this.zBuf = zBuf;
        this.zUser = null;
    }

    private PopCommandMore(ByteBuffer zBuf, String zUser)
    {
        this.zBuf = zBuf;
        this.zUser = zUser;
    }

    // static factories -------------------------------------------------------

    public static PopCommandMore parseAuthUser(ByteBuffer buf)
    {
        Logger logger = Logger.getLogger(PopCommandMore.class);

        ByteBuffer zDup = buf.duplicate();
        String zTmp = AsciiCharBuffer.wrap(zDup).toString();

        String zUser;

        try {
            byte azDecodedBuf[] = (new BASE64Decoder()).decodeBuffer(zTmp);
            zUser = new String(azDecodedBuf);
        } catch (IOException exn) {
            logger.warn("cannot decode encoded auth login user name: " + zTmp + ", " + exn);
            zUser = null;
            /* fall through */
        }

        //logger.debug("user name is: " + zUser);
        return new PopCommandMore(buf, zUser);
    }

    // accessors --------------------------------------------------------------

    public ByteBuffer getBuf()
    {
        return zBuf;
    }

    public String getUser()
    {
        return (null == zUser) ? NO_USER : zUser;
    }

    public boolean isUser()
    {
        return (null == zUser) ? false : true;
    }

    // Token methods ----------------------------------------------------------

    public ByteBuffer getBytes()
    {
        return zBuf.duplicate();
    }

    public int getEstimatedSize()
    {
        return zBuf.remaining();
    }
}
