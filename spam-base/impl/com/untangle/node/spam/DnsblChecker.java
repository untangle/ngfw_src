/**
 * $Id$
 */
package com.untangle.node.spam;

import java.util.HashMap;
import java.util.List;
import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;

import org.apache.log4j.Logger;

import com.untangle.uvm.UvmContextFactory;
import com.untangle.uvm.vnet.TCPNewSessionRequest;

/**
 * A class that checks if an SMTP server is listed on a Realtime Blackhole List (Dnsbl).
 * You may choose which Dnsbl to check. Addresses of several Dnsbl services are included.
 *
 * @author Kai Blankenhorn &lt;<a href="mailto:pub01@bitfolge.de">pub01@bitfolge.de</a>&gt;
 */
public class DnsblChecker
{
    private final Logger logger = Logger.getLogger(getClass());

    private static final int MSECS_PER_SEC = 1000;
    private static final int SKIP_COUNT = 20;

    private static Object rblCntMonitor = new Object();
    private static int rblCnt = 0;

    private Map<DnsblClient, DnsblClientContext> clientMap;
    private List<SpamDnsbl> spamDnsblList;
    private final SpamNodeImpl m_spamImpl;

    public DnsblChecker(List<SpamDnsbl> spamDnsblList,SpamNodeImpl m_spamImpl)
    {
        this.spamDnsblList = spamDnsblList;
        this.m_spamImpl = m_spamImpl;
    }

    /**
     * Checks if a mail server is listed on an Dnsbl service.
     * This method connects to a Dnsbl service to check
     * if a record for <code>ipAddr</code> exists.
     *
     * @param ipAddr - check the IP address of this mail server
     * @return true if the mail server is listed on a blacklist,
     *         false if there is no record
     */

    /*
      http://relays.osirusoft.com/faq.html:
      For a given address, a.b.c.d will return one of the following values if a dns lookup of d.c.b.a.relays.osirusoft.com is performed.

      127.0.0.2 // bl.spamcop.net, dul.dnsbl.sorbs.net, list.dsbl.org return hit
      127.0.0.4 // list.dsbl.org returns hit

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

    public boolean check(TCPNewSessionRequest tsr, long timeoutSec)
    {
        String ipAddr = tsr.clientAddr().getHostAddress();
        String invertedIPAddr = invertIPAddress(ipAddr);

        DnsblClient[] clients = createClients(ipAddr, invertedIPAddr); // create checkers

        for (DnsblClient clientStart : clients) {
            clientStart.startScan(); // start checking
        }

        // wait for results or stop checking if too much time has passed
        long timeout = timeoutSec * MSECS_PER_SEC; // in millisecs
        long remainingTime = timeout;
        long startTime = System.currentTimeMillis();
        for (DnsblClient clientWait : clients) {
            if (0 < remainingTime) {
                // time remains; let other clients continue
                logger.debug("DNSBL: " + clientWait + ", wait: " + remainingTime);
                clientWait.checkProgress(remainingTime);
                remainingTime = timeout - (System.currentTimeMillis() - startTime);
            } else {
                // no time remains; stop other clients
                logger.warn("DNSBL: " + clientWait + ", stop (timed out)");
                clientWait.stopScan();
            }
        }

        Collection<DnsblClientContext> cContexts = clientMap.values(); // get contexts

        // examine results
        // - if any confirmation is found, log it and then report it
        boolean isBlacklisted = false;

        Boolean result;
        for (DnsblClientContext cContext : cContexts) {
            result = cContext.getResult();
            if (null == result) {
                continue; // assume not blacklisted
            }

            if (result.equals(Boolean.TRUE)) {
                logger.debug("DNSBL: " + ipAddr + " is blacklisted.");
                isBlacklisted = logDnsblEvent(cContext, tsr, ipAddr); // log/done
                break;
            }
        }

        freeClients(); // destroy checkers
        return isBlacklisted; // report
    }

    private boolean logDnsblEvent(DnsblClientContext cContext, TCPNewSessionRequest tsr, String ipAddr)
    {
        boolean isBlacklisted = true;

        // we have a confirmed hit
        // but we may decide not to reject the connection from this hit
        // - if we do not reject, then do not log
        synchronized(rblCntMonitor) {
            if (SKIP_COUNT == rblCnt) {
                // accept every '1 out of SKIP_COUNT' connections
                // from a blacklisted SMTP server
                // to test the emails that this server will try to send
                // -> functionality requested by dmorris
                logger.debug(cContext.getHostname() + " confirmed that " + ipAddr + " is on its blacklist but ignoring this time");
                
                rblCnt = 0;
                isBlacklisted = false;
            } else {
                logger.debug(cContext.getHostname() + " confirmed that " + ipAddr + " is on its blacklist");
                tsr.attach(new SpamSmtpTarpitEvent(tsr.pipelineEndpoints(), cContext.getHostname(), tsr.clientAddr(), this.m_spamImpl.getVendor()));

                /* Indicate that there was a block event */
                this.m_spamImpl.incrementBlockCount();

                rblCnt++;
            }
        }

        return isBlacklisted;
    }

    /**
     * Inverts an IP address to match the requirements of most Dnsbl services.<br>
     * Example:
     * <code>invertIPAddress("127.0.0.2")</code> --&gt; <code>"2.0.0.127"</code>
     *
     * @param orgIPAddr - invert this IP address
     * @return the inverted form of orgIPAddr
     */
    private String invertIPAddress(String orgIPAddr) {
        StringTokenizer strTokenizer = new StringTokenizer(orgIPAddr, ".");
        String invertedIPAddr = strTokenizer.nextToken();

        while(strTokenizer.hasMoreTokens()) {
            invertedIPAddr = strTokenizer.nextToken() + "." + invertedIPAddr;
        }

        return invertedIPAddr;
    }

    private DnsblClient createClient(DnsblClientContext cContext) 
    {
        DnsblClient client = new DnsblClient(cContext);
        Thread thread = UvmContextFactory.context().newThread(client);
        client.setThread(thread);
        clientMap.put(client, cContext);
        return client;
    }

    private Object[] getClients() {
        Set<DnsblClient> clientSet = clientMap.keySet();
        return clientSet.toArray();
    }

    private DnsblClient[] createClients(String ipAddr, String invertedIPAddr) 
    {
        clientMap = new HashMap<DnsblClient, DnsblClientContext>();

        DnsblClientContext cContext;
        for (SpamDnsbl spamDnsbl : spamDnsblList) {
            if (false == spamDnsbl.getActive()) {
                logger.debug(spamDnsbl.getHostname() + " is not active; skipping it");
                continue;
            }
            cContext = new DnsblClientContext(spamDnsbl.getHostname(), ipAddr, invertedIPAddr);
            createClient(cContext);
        }

        Object[] cObjects = getClients();
        DnsblClient[] clients = new DnsblClient[cObjects.length];
        int idx = 0;

        for (Object cObject : cObjects) {
            clients[idx] = (DnsblClient) cObject;
            idx++;
        }

        return clients;
    }

    private void freeClients()
    {
        clientMap.clear();
        return;
    }
}
