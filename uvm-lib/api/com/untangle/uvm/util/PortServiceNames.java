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

package com.untangle.uvm.util;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.StringTokenizer;

import org.apache.log4j.Logger;



/**
 * ----------------------------------------------------------------------------
 * This program demonstrates how to lookup port numbers in the
 * /etc/services file (UNIX) or NIS service lookup isn't provided in
 * this implementation.
 * ----------------------------------------------------------------------------
 */
public class PortServiceNames {

    public final static String SERVICES_FILENAME = "/etc/services";
    private final static String UDP_PROTO_NAME = "udp";
    private final static String TCP_PROTO_NAME = "tcp";

    private static PortServiceNames thePSN;

    private Map<Integer, String> tcpMap;
    private Map<Integer, String> udpMap;

    private PortServiceNames() {
        tcpMap = new HashMap<Integer, String>();
        udpMap = new HashMap<Integer, String>();
    }

    static private synchronized void init(String servicesFile) {
        if( thePSN == null ) {
            thePSN = new PortServiceNames();

            // Look for our service, line-by-line:
            try {
                String line;
                BufferedReader br = new BufferedReader(new FileReader(servicesFile));

                // Read /etc/services file.
                // Skip comments and empty lines.
                while ( (line = br.readLine()) != null) {
                    if ((line.length() != 0) && (line.charAt(0) != '#')) {
                        thePSN.parseServicesLine(line);
                    }
                }

                br.close();
            } catch (IOException ioe) {
                String msg = "Services file " + servicesFile + " not found";
                Logger logger = Logger.getLogger(PortServiceNames.class);
                logger.error(msg);
                throw new IllegalArgumentException(msg);
            }
        }
    }

    static {
        init(SERVICES_FILENAME);
    }

    /**
     * The <code>parseServicesLine()</code> method is called by
     * <code>getPortNumberForTcpIpService()</code> to parse a
     * non-comment line in the <tt>/etc/services</tt> file and save
     * the values.
     *
     * @param line A line to compare from the <tt>/etc/services</tt>
     * file.
     *
     * @param tcpipService The name of a TCP/IP "well-known" service
     * found in the <tt>/etc/services</tt> file
     *
     * @param tcpipClass Either "tcp" or "udp", depending on the
     * TCP/IP service desired.
     *
     * @return A port number for a TCP or UDP service (depending on
     * tcpipClass).  Return -1 on error.
     */
    private void parseServicesLine(String line) {
        // Parse line
        StringTokenizer st = new StringTokenizer(line, " \t/#");

        // First get the name on the line (parameter 1):
        if (! st.hasMoreTokens()) {
            return; // bad line
        }
        String name = st.nextToken().trim();

        // Next get the service name on the line (parameter 2):
        if (! st.hasMoreTokens()) {
            return; // bad line
        }
        String portValue = st.nextToken().trim();
        int port;
        try {
            port = Integer.parseInt(portValue);
        } catch (NumberFormatException nfe) {
            // Ignore corrupt /etc/services lines:
            return; // bad line
        }

        // Finally get the class on the line (parameter 3):
        if (! st.hasMoreTokens()) {
            return; // bad line
        }
        String protoValue = st.nextToken().trim();

        // Note: We ignore the aliases. XX

        // Class doesn't match--reject:
        if (protoValue.equals(TCP_PROTO_NAME)) {
            tcpMap.put(port, name);
        } else if (protoValue.equals(UDP_PROTO_NAME)) {
            udpMap.put(port, name);
        } else {
            return; // bad linie
        }
    }


    public static PortServiceNames get() {
        return thePSN;
    }

    public String getTCPServiceName(int port) {
        return tcpMap.get(port);
    }

    public String getUDPServiceName(int port) {
        return udpMap.get(port);
    }



    // For testing.
    public static void main(String[] args) throws IOException {

        int portUserTest, portOk, portFail;
        int errorCount = 0;

        try {
            System.out.println("Port\t Name");
            PortServiceNames psn = get();
            for (Iterator<Integer> iter = psn.tcpMap.keySet().iterator(); iter.hasNext();) {
                int port = iter.next();
                String name = psn.tcpMap.get(port);
                System.out.println("" + port + "\t" + name + "\n");
            }
        } catch (Exception e) {

            System.err.println("Java Exception: " + e + " (exiting)");
            e.printStackTrace(System.err);
        }
    }
}
