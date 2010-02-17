/*
 * $HeadURL$
 * Copyright (c) 2003-2007 Untangle, Inc. 
 *
 * This library is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License, version 2,
 * as published by the Free Software Foundation.
 *
 * This library is distributed in the hope that it will be useful, but
 * AS-IS and WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE, TITLE, or
 * NONINFRINGEMENT.  See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Linking this library statically or dynamically with other modules is
 * making a combined work based on this library.  Thus, the terms and
 * conditions of the GNU General Public License cover the whole combination.
 *
 * As a special exception, the copyright holders of this library give you
 * permission to link this library with independent modules to produce an
 * executable, regardless of the license terms of these independent modules,
 * and to copy and distribute the resulting executable under terms of your
 * choice, provided that you also meet, for each linked independent module,
 * the terms and conditions of the license of that module.  An independent
 * module is a module which is not derived from or based on this library.
 * If you modify this library, you may extend this exception to your version
 * of the library, but you are not obligated to do so.  If you do not wish
 * to do so, delete this exception statement from your version.
 */

package com.untangle.node.mail.papi.pop;

import java.nio.ByteBuffer;

import org.apache.commons.codec.binary.Base64;
import org.apache.log4j.Logger;

import com.untangle.node.token.Token;
import com.untangle.node.util.AsciiCharBuffer;

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
            byte azDecodedBuf[] = Base64.decodeBase64(zTmp.getBytes());
            zUser = new String(azDecodedBuf);
        } catch (Exception exn) {
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
