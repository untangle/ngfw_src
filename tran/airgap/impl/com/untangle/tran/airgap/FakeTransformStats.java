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
package com.untangle.tran.airgap;

import java.io.*;
import java.util.*;

import com.untangle.mvvm.tran.TransformStats;
import org.apache.log4j.Logger;

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
    // 11/05 jdi:
    // Since there can now be more than two interfaces, we no longer try to map client to inside
    // and server to outside.  Instead we total up the counts over all interfaces, where
    // C->S arbitrarily means receive, S->C means transmit.  We only fill in the chunk and byte counts.
    // So:
    //     C->T and T->S is count of all received bytes/chunks
    //     S->T and T->C is count of all transmitted bytes/chunks
    //
 * Describe class <code>FakeTransformStats</code> here.
 *
 * @author <a href="mailto:jdi@slab.ninthwave.com">John Irwin</a>
 * @version 1.0
 */
class FakeTransformStats {

    private static final String PATH_PROCNET_DEV = "/proc/net/dev";
    private static final String ETH_DEV_PREFIX = "eth";
    private static final String TUN_DEV_PREFIX = "tun";
    private static final String TAP_DEV_PREFIX = "tap";

    private static final Logger logger = Logger.getLogger(FakeTransformStats.class);

    public static void update(TransformStats stats) {
        String line = null;

        long totRxBytes = 0;
        long totRxChunks = 0;
        long totTxBytes = 0;
        long totTxChunks = 0;

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
                    if (iface.startsWith(ETH_DEV_PREFIX) ||
                        iface.startsWith(TUN_DEV_PREFIX) ||
                        iface.startsWith(TAP_DEV_PREFIX)) {
                        StringTokenizer st = new StringTokenizer(rest);
                        String rxbytes = st.nextToken();
                        String rxchunks = st.nextToken();
                        st.nextToken(); // errors
                        st.nextToken(); // dropped
                        st.nextToken(); // fifo_errors
                        st.nextToken(); // frame_errors
                        st.nextToken(); // compressed
                        st.nextToken(); // multicast
                        String txbytes = st.nextToken();
                        String txchunks = st.nextToken();
                        totRxBytes += Long.parseLong(rxbytes);
                        totRxChunks += Long.parseLong(rxchunks);
                        totTxBytes += Long.parseLong(txbytes);
                        totTxChunks += Long.parseLong(txchunks);
                    }
                } catch (NumberFormatException x) {
                    logger.warn("Unable to parse number in stats line " + line);
                } catch (NoSuchElementException x) {
                    logger.warn("Unable to parse stats line " + line);
                }
            }
            rdr.close();

            /* Check for overflow */
            long l = incrementCount( stats.c2tBytes(),  totRxBytes );
            stats.t2sBytes(l); stats.c2tBytes(l);
            l = incrementCount( stats.c2tChunks(), totRxChunks );
            stats.t2sChunks(l); stats.c2tChunks(l);

            l = incrementCount( stats.s2tBytes(),  totTxBytes );
            stats.t2cBytes(l); stats.s2tBytes(l);
            l = incrementCount( stats.s2tChunks(), totTxChunks );
            stats.t2cChunks(l); stats.s2tChunks(l);
        } catch (FileNotFoundException x) {
            logger.warn("Cannot open " + PATH_PROCNET_DEV + "(" + x.getMessage() +
                        "), no stats available");
        } catch (IOException x) {
            logger.warn("Unable to read " + PATH_PROCNET_DEV + "(" + x.getMessage() +
                        "), last line " + line);
        }
    }

    private static long incrementCount( long previousCount, long kernelCount )
    {
        /* If the kernel is counting in 64-bits, just return the kernel count */
        if ( kernelCount >= ( 1L << 32 ) ) return kernelCount;

        long previousKernelCount = previousCount & 0xFFFFFFFFL;
        if ( previousKernelCount > kernelCount ) previousCount += ( 1L << 32 );

        return (( previousCount & 0x7FFFFFFF00000000L ) + kernelCount );
    }
}
