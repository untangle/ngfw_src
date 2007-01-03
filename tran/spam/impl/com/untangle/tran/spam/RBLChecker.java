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

package com.untangle.tran.spam;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.*;

/**
 * A class that checks if an SMTP server is listed on a Realtime Blackhole List (RBL).
 * You may choose which RBL to check. Addresses of several RBL services are included.
 *
 * @author Kai Blankenhorn &lt;<a href="mailto:pub01@bitfolge.de">pub01@bitfolge.de</a>&gt;
 */
public class RBLChecker {

    private RBLChecker()
    {}

    /**
     * The <a href="http://www.ordb.org">Open Relay Database</a> lists open relays.
     */
    public final static String RBL_ORDB = "relays.ordb.org";

    /**
     * <a href="http://spamcop.net/bl.shtml">SpamCop</a> lists mail servers that have a high spam-to-legitimate-mail ratio.
     */
    public final static String RBL_SPAMCOP = "bl.spamcop.net";

    /**
     * <a href="http://dsbl.org/">DSBL</a> publishes the IP addresses of hosts which have sent special test email to listme@listme.dsbl.org
     */
    public final static String RBL_DSBL = "list.dsbl.org";

    /**
     * <a href="http://relays.osirusoft.com/">OsiruSoft</a>
     */
    public final static String RBL_OSIRUSOFT = "relays.osirusoft.com";

    /**
     * the default RBL
     */
    private static String RBL = RBL_SPAMCOP;

    public static void main(String[] args)
    {
        System.out.println(checkRelay("127.0.0.2")); //true
        System.out.println(checkRelay("127.0.0.1")); //false
        System.out.println(checkRelay("mail.untangle.com")); //false
    }

    /**
     * Sets the RBL used for checks to a new value. There are several predefined values, accessible as
     * static variables of this class. However, any host which provides RBL services is suitable.<br>
     * To reset the RBL server name to its default value, use <pre>setRBL(null)</pre> or <pre>setRBL("")</pre>.
     *
     * @param rbl the hostname of an RBL service
     */
    public static void setRBL(String rbl)
    {

        if(rbl != null && !rbl.equals("")) {
            RBL = rbl;
        } else {
            RBL = RBL_ORDB;
        }
    }

    /**
     * Checks if a mail server is an open relay using an RBL service. Upon calling, this method connects
     * to the currently set RBL service to check if a record for <code>hostname</code> exists.
     *
     * @param hostName the host name of the mail server to check
     * @return true if the mail server is listed as an open relay, false if there is no record
     */

    /*    http://relays.osirusoft.com/faq.html:
    For a given address, a.b.c.d will return one of the following values if a dns lookup of d.c.b.a.relays.osirusoft.com is performed.

    The DNS addressing is as follows:

    127.0.0.2 Verified Open Relay
    127.0.0.3 Dialup Spam Source
    Dialup Spam Sources are imported into the Zone file from other sources and some known sources are manually added to the local include file.
    127.0.0.4 Confirmed Spam Source
    A site has been identified as a constant source of spam, and is manually added. Submissions for this type of spam require multiple nominations from multiple sites. Test Blockers also find themselves in this catagory.
    127.0.0.5 Smart Host (In progress)
    A Smart host is a site determined to be secure, but relays for those who are not, defeating one level of security. When this is ready, it will be labeled outputs.osirusoft.com. NOTE: I strongly discourage using outputs due to it being way too effective to be useful.
    127.0.0.6 A Spamware software developer or spamvertized site. This information is maintained by spamsites.org and spamhaus.org.
    127.0.0.7 A list server that automatically opts users in without confirmation
    127.0.0.8 An insecure formmail.cgi script. (Planned)
    127.0.0.9 Open proxy servers
    */
    public static boolean checkRelay(String hostName)
    {
        try {
            InetAddress.getByName(invertIPAddress(hostName) + "." + RBL);

            return true;
        } catch(UnknownHostException e) {

            return false;
        }

        /* alternative code, but needs a nameserver
        try {
        DirContext ictx = new InitialDirContext();
        String u = "dns://a.ns.ordb.org/"+inverseIPAddress(hostName)+"."+RBL;
        Attributes attr = ictx.getAttributes(u, new String[] {"A"});
        return attr.getAll().hasMoreElements();
        } catch (NamingException e) {
        //e.printStackTrace();
        }
        return false;
        */
    }

    /**
     * Inverts an IP address to match the requirements of most RBL DNS services.<br>
     * Example: <code>inverseIPAddress("127.0.0.2")</code> --&gt; <code>2.0.0.127</code>
     *
     * @param originalIPAddress the IP address to invert
     * @return the inverted form of the passed IP address
     */
    protected static String invertIPAddress(String originalIPAddress)
    {
        StringTokenizer t = new StringTokenizer(originalIPAddress, ".");
        String inverted = t.nextToken();

        while(t.hasMoreTokens()) {
            inverted = t.nextToken() + "." + inverted;
        }

        return inverted;
    }
}
