/*
 * Copyright (c) 2003, 2004 Metavize Inc.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Metavize Inc. ("Confidential Information").  You shall
 * not disclose such Confidential Information.
 *
 * $Id: FakeTransformStats.java,v 1.1 2005/02/10 20:55:22 jdi Exp $
 */
package com.metavize.tran.airgap;

import java.io.*;
import java.util.*;
import org.apache.log4j.Logger;
import com.metavize.mvvm.tran.TransformStats;

/**
    // Airgap isn't real, so we get stats from /proc/net/dev, which looks like:
    // Inter-|   Receive                                                |  Transmit
    //  face |bytes    packets errs drop fifo frame compressed multicast|bytes    packets errs drop fifo colls carrier compressed
    //   eth0:10006263   19526    0    0    8     0          0         0  2902562   15012    0    0    0     0       0          0
    //     lo:    2564      37    0    0    0     0          0         0     2564      37    0    0    0     0       0          0
    // dummy0:       0       0    0    0    0     0          0         0        0       0    0    0    0     0       0          0
    //   eth1: 1260552    4645    1    0    0     0          0         0   642460    4512    0    0    0     0       0          0
    //    br0: 9086448   10976    0    0    0     0          0         0  1645622   10402    0    0    0     0       0          0
    //
    // We only fill in the chunk and byte counts, and C means inside, S means outside.
    // So:
    //     C->T is count of inside to pipeline,  T->S is count from pipeline to outside.
    //     S->T is count of outside to pipeline, T->C is count from pipeline to inside.
    //
 * Describe class <code>FakeTransformStats</code> here.
 *
 * @author <a href="mailto:jdi@slab.ninthwave.com">John Irwin</a>
 * @version 1.0
 */
public class FakeTransformStats extends TransformStats {

    private static final String PATH_PROCNET_DEV = "/proc/net/dev";
    private static final String OUTSIDE_DEV = "eth0"; // XXXX
    private static final String INSIDE_DEV = "eth1"; // XXXX

    private static final Logger logger = Logger
        .getLogger(FakeTransformStats.class.getName());

    public void update() {
        String line = null;

        try {
            BufferedReader rdr = new BufferedReader(new FileReader(PATH_PROCNET_DEV));

            // Eat header
            rdr.readLine();
            rdr.readLine();

            while ((line = rdr.readLine()) != null) {
                String iface = null;
                String rest = null;
                line = line.trim();
                for (int i = 0; i < line.length(); i++) {
                    char c = line.charAt(i);
                    if (Character.isWhitespace(c)) {
                        iface = line.substring(0, i);
                        rest = line.substring(i);
                        break;
                    }
                    if (c == ':') {
                        // Check for alias
                        int colPos = i++;
                        while (i < line.length() && Character.isDigit(line.charAt(i)))
                            i++;
                        if (i < line.length() && line.charAt(i) == ':') {
                            iface = line.substring(0, i);
                            rest = line.substring(i + 1);
                        } else {
                            iface = line.substring(0, colPos);
                            rest = line.substring(colPos + 1);
                        }
                        break;
                    }
                }
                if (iface == null) {
                    logger.warn("Got weird line in " + PATH_PROCNET_DEV + " (" + line + ")");
                    continue;
                }
                try {
                    if (iface.equals(OUTSIDE_DEV)) {
                        StringTokenizer st = new StringTokenizer(rest);
                        String srxbytes = st.nextToken();
                        String srxchunks = st.nextToken();
                        st.nextToken(); // errors
                        st.nextToken(); // dropped
                        st.nextToken(); // fifo_errors
                        st.nextToken(); // frame_errors
                        st.nextToken(); // compressed
                        st.nextToken(); // multicast
                        String stxbytes = st.nextToken();
                        String stxchunks = st.nextToken();
                        s2tBytes = Long.parseLong(srxbytes);
                        s2tChunks = Long.parseLong(srxchunks);
                        t2sBytes = Long.parseLong(stxbytes);
                        t2sChunks = Long.parseLong(stxchunks);
                    } else if (iface.equals(INSIDE_DEV)) {
                        StringTokenizer st = new StringTokenizer(rest);
                        String crxbytes = st.nextToken();
                        String crxchunks = st.nextToken();
                        st.nextToken(); // errors
                        st.nextToken(); // dropped
                        st.nextToken(); // fifo_errors
                        st.nextToken(); // frame_errors
                        st.nextToken(); // compressed
                        st.nextToken(); // multicast
                        String ctxbytes = st.nextToken();
                        String ctxchunks = st.nextToken();
                        c2tBytes = Long.parseLong(crxbytes);
                        c2tChunks = Long.parseLong(crxchunks);
                        t2cBytes = Long.parseLong(ctxbytes);
                        t2cChunks = Long.parseLong(ctxchunks);
                    }
                } catch (NumberFormatException x) {
                    logger.warn("Unable to parse number in stats line " + line);
                } catch (NoSuchElementException x) {
                    logger.warn("Unable to parse stats line " + line);
                }
            }
            rdr.close();
        } catch (FileNotFoundException x) {
            logger.warn("Cannot open " + PATH_PROCNET_DEV + "(" + x.getMessage() +
                        "), no stats available");
        } catch (IOException x) {
            logger.warn("Unable to read " + PATH_PROCNET_DEV + "(" + x.getMessage() +
                        "), last line " + line);
        }
    }
}
