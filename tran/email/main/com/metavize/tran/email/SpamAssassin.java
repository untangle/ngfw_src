/*
 * Copyright (c) 2003 Metavize Inc.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Metavize Inc. ("Confidential Information").  You shall
 * not disclose such Confidential Information.
 *
 * $Id$
 */
package com.metavize.tran.email;

import java.io.*;
import java.nio.*;
import java.nio.channels.*;
import java.util.*;
import org.apache.log4j.Logger;

public class SpamAssassin extends MVScanner
{
    private static final Logger zLog = Logger.getLogger(SpamAssassin.class);

    private static final int DATASZ = 1024;
    
    private final String zSpamCmd;

    /* This is the name of the configuration file to load for this spam session */
    public SpamAssassin( String zCfgFile ) 
    {
        zSpamCmd = command(zCfgFile);
    }

    public byte[] scanEmail (byte[] inbuf, int len) throws IOException,InterruptedException
    {
        byte[] outbuf = new byte[inbuf.length + EXPAND_ROOM];
                
        Process sProc = Runtime.getRuntime().exec(zSpamCmd);
        InputStream sIS = sProc.getInputStream();
        OutputStream sOS = sProc.getOutputStream();

        sOS.write(inbuf,0,len);
        sOS.flush();
        sOS.close();
        
        int total = 0;
        int i = 0;

        while ((i = sIS.read(outbuf,total,outbuf.length-total)) != -1) {
            total += i;
            if (i == 0 || total == outbuf.length) break;
        }
            
        sProc.waitFor();

        if ((i = sProc.exitValue()) != 0) 
            throw new IOException("SpamAssassin exited with: " + i);

        sIS.close();

        return outbuf;
    }

    public ArrayList scanEmail (ArrayList bufs) throws IOException,InterruptedException
    {
        if (bufs == null || bufs.size() == 0)
            return null;

        Process sProc = Runtime.getRuntime().exec(zSpamCmd);
        ReadableByteChannel sIS = Channels.newChannel(sProc.getInputStream());
        WritableByteChannel sOS = Channels.newChannel(sProc.getOutputStream());

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
            sOS.write(bborg);
        }
        sOS.close();

        int icapacity = DATASZ;
        ByteBuffer bbread = ByteBuffer.allocate(icapacity);
        ArrayList al = new ArrayList(bufs.size());

        int istart;
        int iend;
        int isz;
        byte nextb;
        boolean prevIsCR;

        idx = 0;

        while (true) {
            if (sIS.read(bbread) == -1) 
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
            prevIsCR = false;

            while (false != bbread.hasRemaining())
            {
                for (inext = istart; inext < iposition; inext++)
                {
                    nextb = bbread.get(inext);
                    if ((byte) '\r' == nextb)
                    {
                        prevIsCR = true;
                    }
                    else if ((byte) '\n' == nextb)
                    {
                        iend = inext + 1;
                        break; /* for */
                    }
                    else
                    {
                        prevIsCR = false;
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

                isz = false == prevIsCR ? (iend - istart + 1) : (iend - istart);
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
                if (false == prevIsCR)
                {
                    /* SpamAssassin doesn't terminate lines that
                     * it adds to message
                     * with CRLF sequence
                     * so we'll replace LF with CRLF
                     */
                    bborg.put((isz - 2), (byte) '\r'); /* CR */
                    bborg.put((isz - 1), (byte) '\n'); /* LF */
                    /* we must manually update position for this put method */
                    bborg.position(isz);
                }
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
                    icapacity += DATASZ;
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

        int iexit;

        sProc.waitFor();
        if ((iexit = sProc.exitValue()) != 0) 
            throw new IOException("SpamAssassin exited with: " + iexit);
        sIS.close();

        return al;
    }

    private String command(String zCfgFile)
    {
        //zLog.debug( "scanEmail configFile: " + zCfgFile );

        /* Flags.
         * -x: Do not create a user configuration
         * -p: use cfgFile as the configuration file 
         */
        String zCmd = "nice -n 19 spamassassin" + (( null != zCfgFile ) ? " -x -p " + zCfgFile : "");
        zLog.debug("spamassassin cmd: " + zCmd);
        return zCmd;
    }
}
