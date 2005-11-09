/*
 * Copyright (c) 2005 Metavize Inc.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Metavize Inc. ("Confidential Information").  You shall
 * not disclose such Confidential Information.
 *
 * $Id$
 */
package com.metavize.tran.mail.impl.safelist;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import org.apache.log4j.Logger;
import org.hibernate.Query;
import org.hibernate.Session;

import com.metavize.mvvm.util.TransactionWork;
import com.metavize.tran.mail.MailTransformImpl;
import com.metavize.tran.mail.papi.MailTransformSettings;
import com.metavize.tran.mail.papi.safelist.NoSuchSafelistException;
import com.metavize.tran.mail.papi.safelist.SafelistActionFailedException;
import com.metavize.tran.mail.papi.safelist.SafelistAdminView;
import com.metavize.tran.mail.papi.safelist.SafelistEndUserView;
import com.metavize.tran.mail.papi.safelist.SafelistManipulation;
import com.metavize.tran.mail.papi.safelist.SafelistRecipient;
import com.metavize.tran.mail.papi.safelist.SafelistSender;
import com.metavize.tran.mail.papi.safelist.SafelistSettings;
import com.metavize.tran.mail.papi.safelist.SafelistTransformView;
import com.metavize.tran.mime.EmailAddress;

//TODO bscott This is *obviously* a silly fake implemementation,
//existing only for basic testing.  Someone needs to "make this
//real"

/**
 * Implementation of the safelist stuff
 */
public class SafelistManager
  implements SafelistAdminView, SafelistEndUserView, SafelistTransformView
{
    private final Logger m_logger = Logger.getLogger(SafelistManager.class);

    // caches of values held by persistent hibernate mapping objects
    private HashMap<String, ArrayList<String>> m_listsByRcpnt = new HashMap<String, ArrayList<String>>();
    private HashSet<String> m_allSndrs = new HashSet<String>();

    // caches of persistent hibernate mapping objects
    private HashMap<String, SafelistRecipient> m_allHMSLRcpnts = new HashMap<String, SafelistRecipient>();
    private HashMap<String, SafelistSender> m_allHMSLSndrs = new HashMap<String, SafelistSender>();

    private MailTransformImpl mlImpl;
    private MailTransformSettings mlSettings;

    public SafelistManager() {}

    /**
     * The Safelist manager "cheats" and lets the MailTranformImpl
     * maintain the persistence for settings
     */
    public void setSettings(MailTransformImpl mlImpl, MailTransformSettings mlSettings)
    {
        this.mlImpl = mlImpl;
        this.mlSettings = mlSettings;
        // must cast list because xdoclet does not support java 1.5
        renew((List<SafelistSettings>) mlSettings.getSafelistSettings());
        return;
    }

    //-------------------- SafelistTransformView ------------------------

    //See doc on SafelistTransformView.java
    public boolean isSafelisted(EmailAddress envelopeSender,
      EmailAddress mimeFrom, List<EmailAddress> recipients)
    {
        EmailAddress eAddr = (null != envelopeSender ? envelopeSender : (null != mimeFrom ? mimeFrom : null));

        boolean bReturn;
        if (null == eAddr) {
            bReturn = false;
        } else {
            bReturn = m_allSndrs.contains(eAddr.getAddress().toLowerCase());
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
        ArrayList<String> sndrs = getSndrs(rcpnt);
        if (null == sndrs) {
            sndrs = createSL(rcpnt);
            //throw new NoSuchSafelistException(rcpnt + " has no safelist; cannot add " + newSndr + " to safelist");
        }
        sndrs.add(newSndr.toLowerCase());

        setSndrs(rcpnt, sndrs);
        return toStringArray(sndrs);
    }

    //See doc on SafelistManipulation.java
    public String[] removeFromSafelist(String rcpnt, String obsSndr)
      throws NoSuchSafelistException, SafelistActionFailedException
    {
        m_logger.debug("recipient: " + rcpnt + ", removed: " + obsSndr + " from safelist");
        ArrayList<String> sndrs = getSndrs(rcpnt);
        if (null == sndrs) {
            return null;
            //throw new NoSuchSafelistException(rcpnt + " has no safelist; cannot remove " + obsSndr + " from safelist");
        }
        sndrs.remove(obsSndr.toLowerCase());

        setSndrs(rcpnt, sndrs);
        return toStringArray(sndrs);
    }

    //See doc on SafelistManipulation.java
    public String[] replaceSafelist(String rcpnt, String...newSndrs)
      throws NoSuchSafelistException, SafelistActionFailedException
    {
        m_logger.debug("recipient: " + rcpnt + ", replacing safelist");
        ArrayList<String> sndrs = getSndrs(rcpnt);
        if (null == sndrs) {
            sndrs = createSL(rcpnt);
            //throw new NoSuchSafelistException(rcpnt + " has no safelist to replace");
        } else {
            sndrs.clear();
        }

        for (String sndr : newSndrs) {
            sndrs.add(sndr.toLowerCase());
        }

        setSndrs(rcpnt, sndrs);
        return toStringArray(sndrs);
    }

    //See doc on SafelistManipulation.java
    public String[] getSafelistContents(String rcpnt)
      throws NoSuchSafelistException, SafelistActionFailedException
    {
        m_logger.debug("recipient: " + rcpnt + ", getting safelist");
        ArrayList<String> sndrs = getSndrs(rcpnt);
        if (null == sndrs) {
            sndrs = createSL(rcpnt);
            //throw new NoSuchSafelistException(rcpnt + " has no safelist; cannot get safelist");
        }

        return toStringArray(sndrs);
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
        m_logger.debug("returning list of safelists");
        return new ArrayList<String>(m_listsByRcpnt.keySet());
    }

    //See doc on SafelistAdminView.java
    public void deleteSafelist(String rcpnt)
      throws SafelistActionFailedException
    {
        m_logger.debug("recipient: " + rcpnt + ", deleted safelist");
        m_listsByRcpnt.remove(rcpnt.toLowerCase());

        setSndrs(null, null);
        return;
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
        boolean bReturn = m_listsByRcpnt.containsKey(rcpnt.toLowerCase());
        m_logger.debug("recipient: " + rcpnt + ", has safelist: " + bReturn);
        return bReturn;
    }

    //--------------------- SafelistEndUserView -----------------------

    private synchronized void setSndrs(String rcpnt, ArrayList<String> sndrs)
    {
        if (null == rcpnt) {
            m_logger.debug("updating safelist");
        } else {
            m_logger.debug("recipient: " + rcpnt + ", updating safelist: " + sndrs);
            rcpnt = rcpnt.toLowerCase();
        }

        // must cast list because xdoclet does not support java 1.5
        List<SafelistSettings> safelists = (List<SafelistSettings>) mlSettings.getSafelistSettings();
        ArrayList<String> tmpRcpnts = new ArrayList<String>(m_listsByRcpnt.keySet());

        SafelistRecipient slRcpnt;
        SafelistSender slSndr;
        SafelistSettings safelist;
        ArrayList<String> tmpSndrs;

        safelists.clear();

        for (String tmpRcpnt : tmpRcpnts) {
            m_logger.debug("inserting recipient: " + tmpRcpnt);

            slRcpnt = m_allHMSLRcpnts.get(tmpRcpnt);
            tmpSndrs = m_listsByRcpnt.get(tmpRcpnt);
            if (null == slRcpnt &&
                false == tmpSndrs.isEmpty()) {
                slRcpnt = new SafelistRecipient(tmpRcpnt);
            }

            // if recipient's safelist is empty, we do not save it
            for (String tmpSndr : tmpSndrs) {
                m_logger.debug("inserting sender: " + tmpSndr);

                safelist = new SafelistSettings();

                safelist.setRecipient(slRcpnt);

                slSndr = m_allHMSLSndrs.get(tmpSndr);
                if (null == slSndr) {
                    slSndr = new SafelistSender(tmpSndr);
                }
                safelist.setSender(slSndr);

                safelists.add(safelist);
            }
        }

        if (null != rcpnt && false == m_listsByRcpnt.containsKey(rcpnt)) {
            m_logger.debug("inserting new recipient: " + rcpnt);

            slRcpnt = new SafelistRecipient(rcpnt);

            for (String tmpSndr : sndrs) {
                m_logger.debug("inserting sender: " + tmpSndr);

                safelist = new SafelistSettings();

                safelist.setRecipient(slRcpnt);

                slSndr = m_allHMSLSndrs.get(tmpSndr);
                if (null == slSndr) {
                    slSndr = new SafelistSender(tmpSndr);
                }
                safelist.setSender(slSndr);

                safelists.add(safelist);
            }
        }

        setSafelists(safelists);
        return;
    }

    private void setSafelists(List<SafelistSettings> safelists)
    {
        m_logger.debug("setting safelists: " + safelists);

        // must cast list because xdoclet does not support java 1.5
        mlSettings.setSafelistSettings((List) safelists);

        TransactionWork tw = new TransactionWork()
        {
            public boolean doWork(Session s)
            {
                s.saveOrUpdate(mlSettings);

                return true;
            }

            public Object getResult() { return null; }
        };
        mlImpl.getTransformContext().runTransaction(tw);

        return;
    }

    private ArrayList<String> getSndrs(String rcpnt)
    {
        return getSndrs(getSafelists(), rcpnt);
    }

    private List getSafelists()
    {
        TransactionWork tw = new TransactionWork()
        {
            public boolean doWork(Session s)
            {
                Query q = s.createQuery
                    ("from MailTransformSettings");
                mlSettings = (MailTransformSettings)q.uniqueResult();

                return true;
            }

            public Object getResult() { return null; }
        };
        mlImpl.getTransformContext().runTransaction(tw);

        // must cast list because xdoclet does not support java 1.5
        return (List<SafelistSettings>) mlSettings.getSafelistSettings();
    }

    private ArrayList<String> getSndrs(List<SafelistSettings> safelists, String rcpnt)
    {
        return renewGetSndrs(safelists, rcpnt);
    }

    private void renew(List<SafelistSettings> safelists)
    {
        HashMap<String, ArrayList<String>> listsByRcpnt = new HashMap<String, ArrayList<String>>();
        HashSet<String> allSndrs = new HashSet<String>();

        HashMap<String, SafelistRecipient> allHMSLRcpnts = new HashMap<String, SafelistRecipient>();
        HashMap<String, SafelistSender> allHMSLSndrs = new HashMap<String, SafelistSender>();

        SafelistRecipient slRcpnt;
        SafelistSender slSndr;
        ArrayList<String> sndrs;
        String rcpnt;
        String sndr;

        for (SafelistSettings safelist : safelists) {
            // we implicitly ignore duplicates with HashMap
            slRcpnt = safelist.getRecipient();
            rcpnt = slRcpnt.getAddr().toLowerCase();
            allHMSLRcpnts.put(rcpnt, slRcpnt);

            slSndr = safelist.getSender();
            sndr = slSndr.getAddr().toLowerCase();
            allHMSLSndrs.put(sndr, slSndr);

            // we implicitly ignore duplicates with HashSet
            allSndrs.add(sndr);

            if (false == listsByRcpnt.containsKey(rcpnt)) {
                // create safelist for this recipient
                sndrs = new ArrayList<String>();
                sndrs.add(sndr);
                listsByRcpnt.put(rcpnt, sndrs);
                continue;
            }

            sndrs = listsByRcpnt.get(rcpnt);
            if (true == sndrs.contains(sndr)) {
                // we explicitly ignore duplicates for ArrayList
                continue; 
            }
            // else add sender to this recipient's safelist

            sndrs.add(sndr);
        }

        m_listsByRcpnt = listsByRcpnt;
        m_allSndrs = allSndrs;

        m_allHMSLRcpnts = allHMSLRcpnts;
        m_allHMSLSndrs = allHMSLSndrs;
        return;
    }

    private ArrayList<String> renewGetSndrs(List<SafelistSettings> safelists, String rcpnt)
    {
        renew(safelists);
        // if null, recipient has no safelist
        return (ArrayList<String>) m_listsByRcpnt.get(rcpnt.toLowerCase());
    }

    private ArrayList<String> createSL(String rcpnt)
      throws SafelistActionFailedException
    {
        m_logger.debug("recipient: " + rcpnt + ", created safelist");
        ArrayList<String> sndrs = getSndrs(rcpnt);
        if (null == sndrs) {
            sndrs = new ArrayList<String>();
            m_listsByRcpnt.put(rcpnt, sndrs);
        } else {
            sndrs.clear();
        }

        return sndrs;
    }

    // return references to list contents for private use
    private String[] toStringArray(ArrayList<String> strs)
    {
        return (String[]) strs.toArray(new String[strs.size()]);
    }
}
