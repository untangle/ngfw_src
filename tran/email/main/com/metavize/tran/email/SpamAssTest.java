/*
 * Copyright (c) 2003 Metavize Inc.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Metavize Inc. ("Confidential Information").  You shall
 * not disclose such Confidential Information.
 *
 * $Id: SpamAssTest.java,v 1.2 2005/02/08 08:23:55 rbscott Exp $
 */
package com.metavize.tran.email;

import java.util.*;
import java.io.*;
import java.nio.*;
import java.nio.channels.*;

public class SpamAssTest {

    public SpamAssTest()
    {

    }

    public static void test(String file)
    {
        byte[] buf = new byte[65536];
        byte[] outbuf;
        SpamAssassin sa = new SpamAssassin( null );
        FileInputStream fis = null;
        int len = 0;
        
        try {
            fis = new FileInputStream(file);
        } catch (FileNotFoundException e) {
            System.err.println(e);
            System.exit(-1);
        }
        
        try {
            len = fis.read(buf);
        } catch (IOException e) {
            System.err.println(e);
            System.exit(-1);
        }
        
        try {
            outbuf = sa.scanEmail(buf,len);
            System.out.print(new String(outbuf));
            System.exit(0);
        }
        catch (IOException e) {
            System.err.println("SPAM scan failed: " + e);
        }
        catch (InterruptedException e) {
            System.err.println("SPAM scan failed: " + e);
        }

        System.exit(-1);
    }

    public static void test2(String file)
    {
        ArrayList list = new ArrayList();
        SpamAssassin sa = new SpamAssassin( null );
        ReadableByteChannel is = null;
        int nbyt;
        
        try {
            is = Channels.newChannel(new FileInputStream(file));
        } catch (FileNotFoundException e) {
            System.err.println(e);
            System.exit(-1);
        }
        
        try{
            ByteBuffer bb = ByteBuffer.allocate(100);
            while (true) {
                if ((nbyt = is.read(bb)) == -1)
                    break;

                if (bb.position() == bb.limit()) {
                    list.add(bb);

                    bb = ByteBuffer.allocate(100);
                    bb.position(0);
                    bb.limit(bb.capacity());
                }
                else
                    bb.limit(bb.position());
            }
        }
        catch (IOException e) {
            System.err.println(e.toString());
            System.exit(-1);
        }

        try {
            list = sa.scanEmail(list);

            for (int i = 0; i < list.size() ; i++) {
                ByteBuffer bb = (ByteBuffer)list.get(i);
                bb.position(0);
                while (bb.position() < bb.limit()) {
                    System.out.print((char)bb.get());
                }
            }
            System.out.println();
            System.exit(0);

            if (true) throw new IOException();
            if (true) throw new InterruptedException();
        }
        catch (IOException e) {
            System.err.println("SPAM scan failed: " + e);
        }
        catch (InterruptedException e) {
            System.err.println("SPAM scan failed: " + e);
        }

        System.exit(-1);
    }

    public static void main (String args[])
    {
        if (args.length < 1) {
            System.err.println("missing file arg\n");
            System.exit(-1);
        }

        System.out.println("Opening file: " + args[0]);
        test2(args[0]);
    }
}
