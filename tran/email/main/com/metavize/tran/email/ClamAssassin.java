/*
 * Copyright (c) 2004 Metavize Inc.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Metavize Inc. ("Confidential Information").  You shall
 * not disclose such Confidential Information.
 *
 * $Id: ClamAssassin.java,v 1.3 2005/01/29 03:24:00 cng Exp $
 */
package com.metavize.tran.email;

import java.io.*;
import java.nio.*;
import java.nio.channels.*;
import java.util.*;
import org.apache.log4j.Logger;

/* requires clamav 0.75.1-2 or higher debian package and
 * clamassassin front-end shell script
 * (http://drivel.com/clamassassin/clamassassin-1.2.1.tar.gz)
 * $/tmp/clamassassin> ./configure -prefix /usr # configure for /usr/bin
 * $/tmp/clamassassin> sudo make install
 */
public class ClamAssassin extends MVScanner
{
    private static final Logger zLog = Logger.getLogger(ClamAssassin.class.getName());

    private static final int READSZ = 1024;

    public ClamAssassin() {}

    public byte[] scanEmail (byte[] inbuf, int len) throws IOException,InterruptedException
    {
        byte[] outbuf = new byte[inbuf.length + EXPAND_ROOM];
        Process proc = Runtime.getRuntime().exec("clamassassin");
        InputStream is  = proc.getInputStream();
        OutputStream os = proc.getOutputStream();

        os.write(inbuf,0,len);
        os.flush();
        os.close();
        
        int total = 0;
        int i = 0;

        while ((i = is.read(outbuf,total,outbuf.length-total)) != -1) {
            total += i;
            if (i == 0 || total == outbuf.length) break;
        }
            
        proc.waitFor();

        if ((i = proc.exitValue()) != 0) 
            throw new IOException("ClamAssassin exited with: " + i);

        is.close();
        
        return outbuf;
    }

    public ArrayList scanEmail (ArrayList bufs) throws IOException,InterruptedException
    {
        if (bufs == null || bufs.size() == 0)
            return null;

        Process proc = Runtime.getRuntime().exec("clamassassin");
        ReadableByteChannel is  = Channels.newChannel(proc.getInputStream());
        WritableByteChannel os = Channels.newChannel(proc.getOutputStream());

        ByteBuffer bborg;
        int idx;
        int inext;
        int iposition;

        for (idx = 0; idx < bufs.size(); idx++) {
            bborg = (ByteBuffer)bufs.get(idx);

            //zLog.debug("write buf[" + idx + "], " + bborg);
            //iposition = bborg.position(); /* save position(), get() increments it */
            //bborg.flip();
            //inext = 0;
            //while (inext < iposition) 
                //zLog.debug("[" + inext++ + "]" + (char)bborg.get());

            bborg.flip();
            os.write(bborg);
        }
        os.close();

        int icapacity = READSZ;
        ByteBuffer bbread = ByteBuffer.allocate(icapacity);
        ArrayList al = new ArrayList(bufs.size());

        int istart;
        int iend;
        int isz;

        idx = 0;

        while (true) {
            if (is.read(bbread) == -1) 
                break;

            iposition = bbread.position();
            /* update limit so that we don't process past position */
            bbread.limit(iposition);

            //zLog.debug("newline: " + (int)'\n');
            //zLog.debug("read buf[" + idx + "], " + bbread);
            //bbread.flip();
            //inext = 0;
            //while (inext < iposition)
            //{
                //zLog.debug("[" + inext + "]" + (char)bbread.get(inext));
                //zLog.debug("(" + (int)bbread.get(inext) + ")");
                //inext++;
            //}
            //bbread.position(iposition); /* restore */
            //bbread.limit(iposition); /* restore */

            istart = 0; /* for every read, read buffer position starts at 0 */
            iend = 0;
            bbread.flip();

            while (false != bbread.hasRemaining())
            {
                for (inext = istart; inext < iposition; inext++)
                {
                    if ('\n' == bbread.get(inext))
                    {
                        iend = inext + 1;
                        break; /* for */
                    }
                }

                if (iend == istart)
                {
                    /* last line in read buffer is incomplete
                     * so read rest of line
                     */
                    break; /* while */
                }

                //bbread.position(istart);
                bbread.limit(iend);

                isz = iend - istart;
                if (idx < bufs.size())
                {
                    /* recycle org buffer */
                    bborg = (ByteBuffer)bufs.get(idx++);
                    if (isz > bborg.capacity())
                    {
                        /* org buffer is too small, create exact fit buffer */
                        bborg = ByteBuffer.allocate(isz);
                        --idx; /* try to use org buffer again later */
                    }
                    bborg.clear();
                }
                else
                {
                    /* we're out of org buffers, create new exact fit buffer */
                    bborg = ByteBuffer.allocate(isz);
                }

                bborg.put(bbread);
                bborg.limit(isz); /* we set limit since we may not have allocated buffer to exact size (we may be using one of original buffers) */

                istart = iend;
                bbread.position(istart);
                bbread.limit(iposition);

                al.add(bborg);
            }

            if (false == bbread.hasRemaining())
            {
                bbread.clear();
            }
            else
            {
                if (0 == istart)
                {
                    /* read buffer is full but too small so increase its size */
                    icapacity += READSZ;
                    ByteBuffer bbtmp = ByteBuffer.allocate(icapacity);
                    bbtmp.put(bbread);
                    bbread = bbtmp;
                }
                else
                {
                    /* else shift read buffer contents to start of buffer */
                    bbread.compact();
                }
            }
        }

        proc.waitFor();

        int iexit;
        if ((iexit = proc.exitValue()) != 0) 
            throw new IOException("ClamAssassin exited with: " + iexit);

        is.close();

        return al;
    }
}
