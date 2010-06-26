/*
 * $HeadURL$
 * Copyright (c) 2003-2007 Untangle, Inc.
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License, version 2,
 * as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful, but
 * AS-IS and WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE, TITLE, or
 * NONINFRINGEMENT.  See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 */
package com.untangle.node.mail.impl.safelist;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.log4j.Logger;
import org.hibernate.Query;
import org.hibernate.Session;

import com.untangle.node.mail.MailNodeImpl;
import com.untangle.node.mail.impl.GlobEmailAddressMapper;
import com.untangle.node.mail.impl.quarantine.Quarantine;
import com.untangle.node.mail.papi.MailNodeSettings;
import com.untangle.node.mail.papi.quarantine.QuarantineUserActionFailedException;
import com.untangle.node.mail.papi.safelist.NoSuchSafelistException;
import com.untangle.node.mail.papi.safelist.SafelistActionFailedException;
import com.untangle.node.mail.papi.safelist.SafelistAdminView;
import com.untangle.node.mail.papi.safelist.SafelistCount;
import com.untangle.node.mail.papi.safelist.SafelistEndUserView;
import com.untangle.node.mail.papi.safelist.SafelistNodeView;
import com.untangle.node.mail.papi.safelist.SafelistRecipient;
import com.untangle.node.mail.papi.safelist.SafelistSender;
import com.untangle.node.mail.papi.safelist.SafelistSettings;
import com.untangle.node.mime.EmailAddress;
import com.untangle.uvm.util.TransactionWork;

/**
 * Implementation of the safelist stuff
 */
public class SafelistManager
    implements SafelistAdminView, SafelistEndUserView, SafelistNodeView
{
    private final Logger m_logger = Logger.getLogger(SafelistManager.class);
    private final Quarantine quarantine;

    private MailNodeImpl mlImpl;
    private MailNodeSettings mlSettings;

    // caches of values held by persistent hibernate mapping objects
    private Map<String, List<String>> m_sndrsByRcpnt = new HashMap<String, List<String>>();
    private Map<String, Map<String, Pattern>> m_allSndrs = new HashMap<String, Map<String, Pattern>>();
    private Object allSndrsLock = new Object ();

    public SafelistManager(Quarantine quarantine)
    {
        this.quarantine = quarantine;
    }

    /**
     * The Safelist manager "cheats" and lets the MailTranformImpl
     * maintain the persistence for settings
     */
    public void setSettings(MailNodeImpl mlImpl, MailNodeSettings mlSettings)
    {
        this.mlImpl = mlImpl;
        this.mlSettings = mlSettings;
        // cast list because xdoclet does not support java 1.5
        renew(mlSettings.getSafelistSettings());
    }

    //-------------------- SafelistNodeView ------------------------

    //See doc on SafelistNodeView.java
    public boolean isSafelisted(EmailAddress envelopeSender,
                                EmailAddress mimeFrom, List<EmailAddress> recipients)
    {
        //Fix for bug 1174 - check not only for a null
        //EmailAddress object, but a null internal address
        //String.  This is because of the special "null"
        //address used by MTAs
        //
        // wrs - 12/05

        String envAddrStr = null;
        if(envelopeSender != null && envelopeSender.getAddress() != null) {
            envAddrStr = envelopeSender.getAddress().toLowerCase();
        }

        String hdrAddrStr = null;
        if(mimeFrom != null && mimeFrom.getAddress() != null) {
            hdrAddrStr  = mimeFrom.getAddress().toLowerCase();
       } 

        boolean bReturn = false;

        Set<String> urs = new HashSet<String>();
        urs.add("GLOBAL".toLowerCase());
        if (null != recipients) {
            for (EmailAddress r : recipients) {
                try {
                    urs.add(quarantine.getUltimateRecipient(r.getAddress().toLowerCase()).toLowerCase());
                } catch (QuarantineUserActionFailedException exn) {
                    m_logger.warn("could not get recipient", exn);
                }
            }
        }

        bReturn = checkAddr(urs, envAddrStr);
        if (!bReturn) {
            bReturn = checkAddr(urs, hdrAddrStr);
        }

        m_logger.debug("sender ( " + envelopeSender + ", " + mimeFrom + "): is safelisted: " + bReturn);
        return bReturn;
    }

    //--------------------- SafelistManipulation -----------------------

    //See doc on SafelistManipulation.java
    public String[] addToSafelist(String rcpnt, String newSndr)
        throws NoSuchSafelistException, SafelistActionFailedException
    {
        m_logger.debug("recipient: " + rcpnt + ", added: " + newSndr + " to safelist");
        List<String> sndrs = getSndrs(rcpnt);
        newSndr = newSndr.toLowerCase();
        if (null == sndrs) {
            sndrs = createSL(rcpnt);
            //throw new NoSuchSafelistException(rcpnt + " has no safelist; cannot add " + newSndr + " to safelist");
        } else if (true == sndrs.contains(newSndr)) {
            // if multiple views manipulate same user and
            // one view has recently added new sender,
            // other view does not refresh itself to pick up change
            // - we explicitly drop duplicates from ArrayList
            m_logger.debug("recipient: " + rcpnt + ", " + newSndr + " already exists in safelist");
            return toStringArray(sndrs);
        }
        // else recipient is adding new sender
        sndrs.add(newSndr);
        synchronized (allSndrsLock) {
            addSndr(m_allSndrs, rcpnt, newSndr);
        }

        setSndrs(rcpnt, sndrs);
        return toStringArray(sndrs);
    }

    //See doc on SafelistManipulation.java
    public String[] removeFromSafelist(String rcpnt, String obsSndr)
        throws NoSuchSafelistException, SafelistActionFailedException
    {
        m_logger.debug("recipient: " + rcpnt + ", removed: " + obsSndr + " from safelist");
        List<String> sndrs = getSndrs(rcpnt);
        obsSndr = obsSndr.toLowerCase();
        if (null == sndrs) {
            return null;
            //throw new NoSuchSafelistException(rcpnt + " has no safelist; cannot remove " + obsSndr + " from safelist");
        } else if (false == sndrs.contains(obsSndr)) {
            // if multiple views manipulate same user and
            // one view has recently removed sender,
            // other view does not refresh itself to pick up change
            // - we explicitly dropped duplicates from ArrayList
            //   so we don't have to remove sender again
            m_logger.debug("recipient: " + rcpnt + ", " + obsSndr + " does not exist in safelist");
            return toStringArray(sndrs);
        }
        // else recipient is removing sender
        sndrs.remove(obsSndr);
        synchronized (allSndrsLock) {
            removeSndr(m_allSndrs, rcpnt, obsSndr);
        }

        setSndrs(rcpnt, sndrs);
        return toStringArray(sndrs);
    }

    public String[] removeFromSafelists(String rcpnt, String[] obsSndrs)
        throws NoSuchSafelistException, SafelistActionFailedException
    {
        List<String> sndrs = getSndrs(rcpnt);
        if (null == sndrs) {
            return null;
            //throw new NoSuchSafelistException(rcpnt + " has no safelist; cannot remove " + obsSndr + " from safelist");
        }
        if (obsSndrs != null && obsSndrs.length > 0) {
            m_logger.debug("recipient: " + rcpnt + ", removed: " + obsSndrs + " from safelist");
            for (int i = 0; i < obsSndrs.length; i++) {
                String obsSndr =  obsSndrs[i].toLowerCase();
                if (false == sndrs.contains(obsSndr)) {
                    // if multiple views manipulate same user and
                    // one view has recently removed sender,
                    // other view does not refresh itself to pick up change
                    // - we explicitly dropped duplicates from ArrayList
                    //   so we don't have to remove sender again
                    m_logger.debug("recipient: " + rcpnt + ", " + obsSndr + " does not exist in safelist");
                } else {
                    // else recipient is removing sender
                    sndrs.remove(obsSndr);
                    synchronized (allSndrsLock) {
                        removeSndr(m_allSndrs, rcpnt, obsSndr);
                    }
                }
            }

            setSndrs(rcpnt, sndrs);
        }
        return toStringArray(sndrs);
    }


    //See doc on SafelistManipulation.java
    public String[] replaceSafelist(String rcpnt, String...newSndrs)
        throws NoSuchSafelistException, SafelistActionFailedException
    {
        m_logger.debug("recipient: " + rcpnt + ", replacing safelist");
        List<String> sndrs = getSndrs(rcpnt);
        if (null == sndrs) {
            sndrs = createSL(rcpnt);
            //throw new NoSuchSafelistException(rcpnt + " has no safelist to replace");
        } else {
            // if multiple views manipulate same user and
            // one view has recently replaced senders,
            // other view does not refresh itself to pick up change
            // - we let this change overwrite previous change
            sndrs.clear();
        }

        synchronized (allSndrsLock) {
            for (String sndr : newSndrs) {
                sndr = sndr.toLowerCase();
                sndrs.add(sndr);
                addSndr(m_allSndrs, rcpnt, sndr);
            }
        }

        setSndrs(rcpnt, sndrs);
        return toStringArray(sndrs);
    }

    //See doc on SafelistManipulation.java
    public String[] getSafelistContents(String rcpnt)
        throws NoSuchSafelistException, SafelistActionFailedException
    {
        m_logger.debug("recipient: " + rcpnt + ", getting safelist");
        List<String> sndrs = getSndrs(rcpnt);
        if (null == sndrs) {
            sndrs = createSL(rcpnt);
            //throw new NoSuchSafelistException(rcpnt + " has no safelist; cannot get safelist");
        }

        return toStringArray(sndrs);
    }

    //See doc on SafelistManipulation.java
    public int getSafelistCnt(String rcpnt)
        throws NoSuchSafelistException, SafelistActionFailedException
    {
        m_logger.debug("recipient: " + rcpnt + ", getting safelist cnt");
        List<String> sndrs = getSndrs(rcpnt);
        if (null == sndrs) {
            return 0;
            //throw new NoSuchSafelistException(rcpnt + " has no safelist; cannot get safelist cnt");
        }

        return sndrs.size();
    }

    //See doc on SafelistManipulation.java
    public boolean hasOrCanHaveSafelist(String rcpnt)
    {
        m_logger.debug("recipient: " + rcpnt + ", has or can have safelist");
        return true;
    }

    //See doc on SafelistManipulation.java
    public void test()
    {
        return;
    }

    //--------------------- SafelistAdminView -----------------------

    //See doc on SafelistAdminView.java
    public List<String> listSafelists() throws SafelistActionFailedException
    {
        m_logger.debug("returning all safelists");
        return new ArrayList<String>(m_sndrsByRcpnt.keySet());
    }

    //See doc on SafelistAdminView.java
    public void deleteSafelist(String rcpnt)
        throws SafelistActionFailedException
    {
        m_logger.debug("recipient: " + rcpnt + ", deleted safelist");
        m_sndrsByRcpnt.remove(rcpnt.toLowerCase());

        setSndrs(null, null);
        renew(mlSettings.getSafelistSettings());
        return;
    }

    public void deleteSafelists(String[] rcpnts)
        throws SafelistActionFailedException
    {
        if (rcpnts != null && rcpnts.length > 0) {
            for (int i = 0; i < rcpnts.length; i++) {
                deleteSafelist(rcpnts[i]);
            }
        }
    }

    //See doc on SafelistAdminView.java
    public void createSafelist(String rcpnt)
        throws SafelistActionFailedException
    {
        createSL(rcpnt);
        return;
    }

    //See doc on SafelistAdminView.java
    public boolean safelistExists(String rcpnt)
        throws SafelistActionFailedException
    {
        boolean bReturn = m_sndrsByRcpnt.containsKey(rcpnt.toLowerCase());
        m_logger.debug("recipient: " + rcpnt + ", has safelist: " + bReturn);
        return bReturn;
    }

    //See doc on SafelistAdminView.java
    public List<SafelistCount> getUserSafelistCounts()
        throws NoSuchSafelistException, SafelistActionFailedException {
        List<String> safelists = listSafelists();
        List<SafelistCount> safelistCounts = new ArrayList<SafelistCount>(safelists.size());
        for (String account : safelists){

            if (account.equalsIgnoreCase("GLOBAL")) {
                // ignore GLOBAL safelist for admin
                continue;
            }
            SafelistCount safelistCnt = new SafelistCount(account, getSafelistCnt(account));
            safelistCounts.add(safelistCnt);
        }
    return safelistCounts;
}
    //--------------------- SafelistEndUserView -----------------------

    private boolean checkAddr(Set<String> urs, String addrStr)
    {
        if (null != addrStr) {
            synchronized (allSndrsLock) {
                for (String ur : urs) {
                    Map<String, Pattern> m = m_allSndrs.get(ur);
                    if (null == m) { continue; }
                    if (true == m.containsKey(addrStr)) {
                        m_logger.debug("literal match, sender: " + addrStr);
                        return true;
                    } else { // is not a literal match so try limited regex match
                        Pattern sndrPattern;
                        Matcher sndrMatcher;

                        for (Iterator<Pattern> iter = m.values().iterator();
                             true == iter.hasNext(); ) {
                            sndrPattern = iter.next();
                            sndrMatcher = sndrPattern.matcher(addrStr);
                            if (true == sndrMatcher.matches()) {
                                m_logger.debug("pattern match: " + sndrPattern
                                               + ", sender: " + addrStr);
                                return true;
                            }
                        }
                    }
                }
            }
        }

        return false;
    }

    // refresh (add/delete/update) safelists for this recipient and
    // make these changes visible as persistent hibernate mapping objects
    private synchronized void setSndrs(String rcpnt, List<String> sndrs)
    {
        if (null == rcpnt) {
            m_logger.debug("refreshing all safelists");
        } else {
            m_logger.debug("recipient: " + rcpnt + ", updating safelist: " + sndrs);
            rcpnt = rcpnt.toLowerCase();
        }

        HashMap<String, ArrayList<SafelistSettings>> allHMSafelistsByRcpnt = new HashMap<String, ArrayList<SafelistSettings>>();
        HashMap<String, SafelistSender> allHMSndrs = new HashMap<String, SafelistSender>();
        // cast list because xdoclet does not support java 1.5
        List<SafelistSettings> safelists = mlSettings.getSafelistSettings();

        ArrayList<SafelistSettings> rcpntHMSafelists;
        SafelistRecipient slRcpnt;
        SafelistSender slSndr;
        String tmpRcpnt;
        String tmpSndr;

        // create temporary caches of safelists
        for (SafelistSettings safelist : safelists) {
            // recipient may exist in multiple safelists
            // but we only use one copy of recipient
            slRcpnt = safelist.getRecipient();
            tmpRcpnt = slRcpnt.getAddr();

            // sender may exist in multiple safelists
            // but we only use one copy of sender
            slSndr = safelist.getSender();
            tmpSndr = slSndr.getAddr();

            allHMSndrs.put(tmpSndr, slSndr); // cache sender

            rcpntHMSafelists = allHMSafelistsByRcpnt.get(tmpRcpnt);
            if (null == rcpntHMSafelists) {
                // create safelist cache for recipient
                rcpntHMSafelists = new ArrayList<SafelistSettings>();
                allHMSafelistsByRcpnt.put(tmpRcpnt, rcpntHMSafelists);
            }
            rcpntHMSafelists.add(safelist); // cache safelist for recipient
        }

        List<SafelistSettings> newSafelists = new ArrayList<SafelistSettings>(safelists.size());
        List<String> curRcpnts = new ArrayList<String>(m_sndrsByRcpnt.keySet());

        SafelistSettings newSafelist;
        List<String> curSndrs;

        // update/add/remove safelists for current recipients
        for (String curRcpnt : curRcpnts) {
            if (null != rcpnt && true == rcpnt.equals(curRcpnt)) {
                if (null == sndrs) {
                    // we drop obsolete safelists for this recipient
                    // (by not reusing this recipient's safelists)
                    continue;
                } else {
                    // we have new safelists for this recipient
                    curSndrs = sndrs;
                }
            } else {
                // we reuse old safelists for this recipient
                // - list always exists in this context (but it may be empty)
                curSndrs = m_sndrsByRcpnt.get(curRcpnt);
            }

            rcpntHMSafelists = allHMSafelistsByRcpnt.get(curRcpnt);
            if (null == rcpntHMSafelists) {
                // create new recipient
                slRcpnt = new SafelistRecipient(curRcpnt);
                m_logger.debug("adding recipient: " + curRcpnt);

                // create safelist cache for new recipient
                rcpntHMSafelists = new ArrayList<SafelistSettings>();
                allHMSafelistsByRcpnt.put(curRcpnt, rcpntHMSafelists);
            } else {
                // use recipient from any safelist; recipients are all same
                slRcpnt = rcpntHMSafelists.get(0).getRecipient();
                m_logger.debug("reusing recipient: " + curRcpnt);
            }

            // if recipient has no safelist, skip recipient (do not save)
            for (String curSndr : curSndrs) {
                newSafelist = getSndrSafelist(curSndr, rcpntHMSafelists);
                if (null != newSafelist) {
                    // safelist already exists for this recipient
                    // so reuse safelist
                    newSafelists.add(newSafelist);
                    m_logger.debug("reusing sender: " + curSndr);
                    m_logger.debug("reusing safelist: " + newSafelist);
                    continue;
                }
                // else safelist doesn't exist for this recipient
                // so create new safelist

                newSafelist = new SafelistSettings();
                newSafelist.setRecipient(slRcpnt);

                // if another recipient uses this sender,
                // this recipient can use this sender too
                slSndr = allHMSndrs.get(curSndr);
                if (null == slSndr) {
                    // create new sender
                    slSndr = new SafelistSender(curSndr);
                    // cache new sender
                    allHMSndrs.put(curSndr, slSndr);
                    m_logger.debug("adding sender: " + curSndr);
                } else {
                    // else reuse sender
                    m_logger.debug("reusing sender: " + curSndr);
                }

                newSafelist.setSender(slSndr);

                newSafelists.add(newSafelist);
                m_logger.debug("adding safelist: " + newSafelist);

                if (null != rcpntHMSafelists) {
                    // cache new safelist for new recipient
                    rcpntHMSafelists.add(newSafelist);
                }
            }
        }

        // refresh all safelists
        // - delete unused safelists, add new safelists, reuse old safelists
        safelists.clear(); // clear old cache
        safelists.addAll(newSafelists); // add new cache

        // clear all caches of safelist references
        ArrayList<String> clrRcpnts = new ArrayList<String>(m_sndrsByRcpnt.keySet());
        for (String clrRcpnt : clrRcpnts) {
            allHMSafelistsByRcpnt.get(clrRcpnt).clear();
        }
        allHMSafelistsByRcpnt.clear();
        allHMSndrs.clear();
        newSafelists.clear();

        setHMSafelists(safelists); // set new cache
        return;
    }

    private SafelistSettings getSndrSafelist(String sndr, List<SafelistSettings> safelists)
    {
        if (null == safelists) {
            return null;
        }

        for (SafelistSettings safelist : safelists) {
            if (true == sndr.equals(safelist.getSender().getAddr())) {
                return safelist;
            }
        }

        return null;
    }

    private void setHMSafelists(List<SafelistSettings> safelists)
    {
        m_logger.debug("setting safelists, size: " + safelists.size());

        mlSettings.setSafelistSettings(safelists);

        mlImpl.setMailNodeSettings(mlSettings);

        return;
    }

    // get (refresh) safelists for this recipient
    private List<String> getSndrs(String rcpnt)
    {
        return getSndrs(getHMSafelists(), rcpnt);
    }

    private List<String> getSndrs(List<SafelistSettings> safelists, String rcpnt)
    {
        return renewGetSndrs(safelists, rcpnt);
    }

    private List<SafelistSettings> getHMSafelists()
    {
        TransactionWork<Object> tw = new TransactionWork<Object>()
            {
                public boolean doWork(Session s)
                {
                    Query q = s.createQuery("from MailNodeSettings");
                    mlSettings = (MailNodeSettings)q.uniqueResult();

                    return true;
                }

                public Object getResult() { return null; }
            };
        mlImpl.getNodeContext().runTransaction(tw);

        // cast list because xdoclet does not support java 1.5
        return mlSettings.getSafelistSettings();
    }

    private void renew(List<SafelistSettings> safelists)
    {
        m_logger.debug("reading all safelists");

        Map<String, List<String>> sndrsByRcpnt = new HashMap<String, List<String>>();
        Map<String, Map<String, Pattern>> allSndrs = new HashMap<String, Map<String, Pattern>>();

        SafelistRecipient slRcpnt;
        SafelistSender slSndr;
        List<String> sndrs;
        String rcpnt;
        String sndr;

        for (SafelistSettings safelist : safelists) {
            // we implicitly ignore duplicates w/ HashMap
            slRcpnt = safelist.getRecipient();
            rcpnt = slRcpnt.getAddr().toLowerCase();

            slSndr = safelist.getSender();
            sndr = slSndr.getAddr().toLowerCase();

            //m_logger.debug("using safelist: " + safelist + ", recipient: " + rcpnt + ", sender: " + sndr);

            addSndr(allSndrs, rcpnt, sndr);

            sndrs = sndrsByRcpnt.get(rcpnt);
            if (null == sndrs) {
                // create new safelists for this recipient
                sndrs = new ArrayList<String>();
                sndrs.add(sndr);
                sndrsByRcpnt.put(rcpnt, sndrs);
                continue;
            }
            // else if necessary, add sender to this recipient's safelists

            if (true == sndrs.contains(sndr)) {
                // we explicitly ignore duplicates for ArrayList
                continue;
            }
            // else add sender to this recipient's safelist

            sndrs.add(sndr);
        }

        m_sndrsByRcpnt = sndrsByRcpnt;
        synchronized (allSndrsLock) {
            m_allSndrs = allSndrs;
        }

        return;
    }

    // add/replace sndr - assumes sndr is already in lower-case format
    private void addSndr(Map<String, Map<String, Pattern>> sndrs, String rcpnt, String sndr)
    {
        String sndrTmp = GlobEmailAddressMapper.fixupWildcardAddress(sndr);
        Pattern sndrPattern = Pattern.compile(sndrTmp);

        Map<String, Pattern> m = sndrs.get(rcpnt);
        if (null == m) {
            m = new HashMap<String, Pattern>();
            sndrs.put(rcpnt, m);
        }

        // we implicitly ignore duplicates w/ HashSet
        m.put(sndr, sndrPattern);
    }

    // remove sndr - assumes sndr is already in lower-case format
    private Pattern removeSndr(Map<String, Map<String, Pattern>> sndrs, String rcpnt, String sndr)
    {
        Map<String, Pattern> m = sndrs.get(rcpnt);
        if (null == m) {
            return null;
        } else {
            return m.remove(sndr);
        }
    }

    private List<String> renewGetSndrs(List<SafelistSettings> safelists, String rcpnt)
    {
        renew(safelists);
        // if null, recipient has no safelist
        return (m_sndrsByRcpnt.get(rcpnt.toLowerCase()));
    }

    private List<String> createSL(String rcpnt)
        throws SafelistActionFailedException
    {
        m_logger.debug("recipient: " + rcpnt + ", created safelist");
        List<String> sndrs = getSndrs(rcpnt);
        if (null == sndrs) {
            sndrs = new ArrayList<String>();
            m_sndrsByRcpnt.put(rcpnt, sndrs);
        } else {
            sndrs.clear();
        }

        return sndrs;
    }

    // return references to list contents for private use
    private String[] toStringArray(List<String> strs)
    {
        return strs.toArray(new String[strs.size()]);
    }
}
