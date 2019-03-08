/**
 * $Id$
 */
package com.untangle.app.smtp.safelist;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.mail.internet.InternetAddress;

import org.apache.log4j.Logger;

import com.untangle.app.smtp.GlobEmailAddressMapper;
import com.untangle.app.smtp.SmtpImpl;
import com.untangle.app.smtp.SmtpSettings;
import com.untangle.app.smtp.quarantine.Quarantine;

/**
 * Implementation of the safelist stuff
 */
public class SafelistManager implements SafelistAdminView, SafelistAppView
{
    private final Logger m_logger = Logger.getLogger(SafelistManager.class);
    private final Quarantine quarantine;

    private SmtpImpl mlImpl;
    private SmtpSettings mlSettings;

    // caches of values
    private Map<String, List<String>> m_sndrsByRcpnt = new HashMap<String, List<String>>();
    private Map<String, Map<String, Pattern>> m_allSndrs = new HashMap<String, Map<String, Pattern>>();
    private Object allSndrsLock = new Object();

    /**
     * Initialize safelist with quarantine.
     * 
     * @param  quarantine Quarantine to use in this safelist.
     */
    public SafelistManager(Quarantine quarantine) {
        this.quarantine = quarantine;
    }

    /**
     * The Safelist manager "cheats" and lets the MailTranformImpl maintain the persistence for settings
     *
     * @param mlImpl SMTP implementaton object.
     * @param mlSettings Smtp settings. 
     */
    public void setSettings(SmtpImpl mlImpl, SmtpSettings mlSettings)
    {
        this.mlImpl = mlImpl;
        this.mlSettings = mlSettings;
        // cast list because xdoclet does not support java 1.5
        renew(mlSettings.getSafelistSettings());
    }

    // -------------------- SafelistAppView ------------------------

    /**
     * Determine of this email address is safelisted.
     * See doc on SafelistAppView.java
     * 
     * @param  envelopeSender Email address from envelope.
     * @param  mimeFrom       Email address from mime.
     * @param  recipients     List of recipients.
     * @return                true if addresses are in safelist, false otherwise.
     */
    @Override
    public boolean isSafelisted(InternetAddress envelopeSender, InternetAddress mimeFrom, List<InternetAddress> recipients)
    {
        // Fix for bug 1174 - check not only for a null
        // EmailAddress object, but a null internal address
        // String. This is because of the special "null"
        // address used by MTAs
        //
        // wrs - 12/05

        String envAddrStr = null;
        if (envelopeSender != null && envelopeSender.getAddress() != null) {
            envAddrStr = envelopeSender.getAddress().toLowerCase();
        }

        String hdrAddrStr = null;
        if (mimeFrom != null && mimeFrom.getAddress() != null) {
            hdrAddrStr = mimeFrom.getAddress().toLowerCase();
        }

        boolean bReturn = false;

        Set<String> urs = new HashSet<String>();
        urs.add("GLOBAL".toLowerCase());
        if (null != recipients) {
            for (InternetAddress r : recipients) {
                try {
                    urs.add(quarantine.getUltimateRecipient(r.getAddress().toLowerCase()).toLowerCase());
                } catch (Exception exn) {
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

    // --------------------- SafelistManipulation -----------------------

    /**
     * Add sender/recipient addresses to safelist.
     * 
     * See doc on SafelistManipulation.java
     * @param  rcpnt                         Recipient.
     * @param  newSndr                       Sender address.
     * @return                               Array of senders.
     * @throws NoSuchSafelistException       If no safelist exists.
     * @throws SafelistActionFailedException If safelist action failed.
     */
    @Override
    public String[] addToSafelist(String rcpnt, String newSndr) throws NoSuchSafelistException,
            SafelistActionFailedException
    {
        m_logger.debug("recipient: " + rcpnt + ", added: " + newSndr + " to safelist");
        List<String> sndrs = getSndrs(rcpnt);
        newSndr = newSndr.toLowerCase();
        if (null == sndrs) {
            sndrs = createSL(rcpnt);
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

    /**
     * Remove sender/recipient addresses from safelist.
     * See doc on SafelistManipulation.java
     *
     * @param  rcpnt                         Recipient.
     * @param  obsSndr                       Sender address.
     * @return                               Array of senders.
     * @throws NoSuchSafelistException       If no safelist exists.
     * @throws SafelistActionFailedException If safelist action failed.
     */
    @Override
    public String[] removeFromSafelist(String rcpnt, String obsSndr) throws NoSuchSafelistException,
            SafelistActionFailedException
    {
        m_logger.debug("recipient: " + rcpnt + ", removed: " + obsSndr + " from safelist");
        List<String> sndrs = getSndrs(rcpnt);
        obsSndr = obsSndr.toLowerCase();
        if (null == sndrs) {
            return null;
        } else if (false == sndrs.contains(obsSndr)) {
            // if multiple views manipulate same user and
            // one view has recently removed sender,
            // other view does not refresh itself to pick up change
            // - we explicitly dropped duplicates from ArrayList
            // so we don't have to remove sender again
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

    /**
     * Remove sender/recipient addresses from safelist.
     * See doc on SafelistManipulation.java
     *
     * @param  rcpnt                         Recipient.
     * @param  obsSndrs                      Array of sender addresses.
     * @return                               Array of senders.
     * @throws NoSuchSafelistException       If no safelist exists.
     * @throws SafelistActionFailedException If safelist action failed.
     */
    @Override
    public String[] removeFromSafelists(String rcpnt, String[] obsSndrs) throws NoSuchSafelistException,
            SafelistActionFailedException
    {
        List<String> sndrs = getSndrs(rcpnt);
        if (null == sndrs) {
            return null;
        }
        if (obsSndrs != null && obsSndrs.length > 0) {
            m_logger.debug("recipient: " + rcpnt + ", removed: " + obsSndrs + " from safelist");
            for (int i = 0; i < obsSndrs.length; i++) {
                String obsSndr = obsSndrs[i].toLowerCase();
                if (false == sndrs.contains(obsSndr)) {
                    // if multiple views manipulate same user and
                    // one view has recently removed sender,
                    // other view does not refresh itself to pick up change
                    // - we explicitly dropped duplicates from ArrayList
                    // so we don't have to remove sender again
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

    /**
     * Replace  sender/recipient addresses from safelist.
     * See doc on SafelistManipulation.java
     *
     * @param  rcpnt                         Recipient.
     * @param  newSndrs                      Array of sender addresses.
     * @return                               Array of senders.
     * @throws NoSuchSafelistException       If no safelist exists.
     * @throws SafelistActionFailedException If safelist action failed.
     */
    @Override
    public String[] replaceSafelist(String rcpnt, String... newSndrs) throws NoSuchSafelistException,
            SafelistActionFailedException
    {
        m_logger.debug("recipient: " + rcpnt + ", replacing safelist");
        List<String> sndrs = getSndrs(rcpnt);
        if (null == sndrs) {
            sndrs = createSL(rcpnt);
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

    /**
     * Get string of safelist addresses.
     * See doc on SafelistManipulation.java
     * 
     * @param  rcpnt                         Safelist owner address.
     * @return                               Array of safelisted email addresses.
     * @throws NoSuchSafelistException       If no safelist exists.
     * @throws SafelistActionFailedException If safelist action failed.
     */
    @Override
    public String[] getSafelistContents(String rcpnt) throws NoSuchSafelistException, SafelistActionFailedException
    {
        m_logger.debug("recipient: " + rcpnt + ", getting safelist");
        List<String> sndrs = getSndrs(rcpnt);
        if (null == sndrs) {
            sndrs = createSL(rcpnt);
        }

        return toStringArray(sndrs);
    }

    /**
     * Get size of safelist for this address.
     * See doc on SafelistManipulation.java
     * 
     * @param  rcpnt                         Safelist owner address.
     * @return                               Number of safelist addresses.
     * @throws NoSuchSafelistException       If no safelist exists.
     * @throws SafelistActionFailedException If safelist action failed.
     */
    @Override
    public int getSafelistCnt(String rcpnt) throws NoSuchSafelistException, SafelistActionFailedException
    {
        m_logger.debug("recipient: " + rcpnt + ", getting safelist cnt");
        List<String> sndrs = getSndrs(rcpnt);
        if (null == sndrs) {
            return 0;
        }

        return sndrs.size();
    }

    /**
     * Determine of this recipient address can have a safelist.
     * 
     * @param  rcpnt Email address of recipient.
     * @return       Always true.
     */
    @Override
    public boolean hasOrCanHaveSafelist(String rcpnt)
    {
        m_logger.debug("recipient: " + rcpnt + ", has or can have safelist");
        return true;
    }

    /**
     * Test method.
     */
    @Override
    public void test()
    {
        return;
    }

    // --------------------- SafelistAdminView -----------------------

    /**
     * List of addresses with safelists.
     * 
     * @return String list of email addresses.
     * @throws SafelistActionFailedException If safelist action failed.
     */
    @Override
    public List<String> listSafelists() throws SafelistActionFailedException
    {
        m_logger.debug("returning all safelists");
        return new ArrayList<String>(m_sndrsByRcpnt.keySet());
    }

    /**
     * Delete safelist for the recipient.
     *
     * @param rcpnt Owner of safelist.
     * @throws SafelistActionFailedException If safelist action failed.
     */
    @Override
    public void deleteSafelist(String rcpnt) throws SafelistActionFailedException
    {
        m_logger.debug("recipient: " + rcpnt + ", deleted safelist");
        m_sndrsByRcpnt.remove(rcpnt.toLowerCase());

        setSndrs(null, null);
        renew(mlSettings.getSafelistSettings());
        return;
    }

    /**
     * Delete safelists for the list of recipients.
     *
     * @param rcpnts Array of strings of the safelist owners.
     * @throws SafelistActionFailedException If safelist action failed.
     */
    @Override
    public void deleteSafelists(String[] rcpnts) throws SafelistActionFailedException
    {
        if (rcpnts != null && rcpnts.length > 0) {
            for (int i = 0; i < rcpnts.length; i++) {
                deleteSafelist(rcpnts[i]);
            }
        }
    }

    /**
     * Create safelist for recipient.
     *
     * @param rcpnt Safelist owner.
     * @throws SafelistActionFailedException If safelist action failed.
     */
    @Override
    public void createSafelist(String rcpnt) throws SafelistActionFailedException
    {
        createSL(rcpnt);
        return;
    }

    /**
     * Determine of safelist exists for this recipient.
     *
     * @param rcpnt Safelist owner.
     * @return true if safelist exist, false otherwise.
     * @throws SafelistActionFailedException If safelist action failed.
     */
    @Override
    public boolean safelistExists(String rcpnt) throws SafelistActionFailedException
    {
        boolean bReturn = m_sndrsByRcpnt.containsKey(rcpnt.toLowerCase());
        m_logger.debug("recipient: " + rcpnt + ", has safelist: " + bReturn);
        return bReturn;
    }

    /**
     * Get safelist counts for all avalable safelists.
     *
     * @return List of SafeListAccount for all availabel safelists.
     * @throws NoSuchSafelistException if no such safelist exists.
     * @throws SafelistActionFailedException If safelist action failed.
     */
    @Override
    public List<SafelistCount> getUserSafelistCounts() throws NoSuchSafelistException, SafelistActionFailedException
    {
        List<String> safelists = listSafelists();
        List<SafelistCount> safelistCounts = new ArrayList<SafelistCount>(safelists.size());
        for (String account : safelists) {

            if (account.equalsIgnoreCase("GLOBAL")) {
                // ignore GLOBAL safelist for admin
                continue;
            }
            SafelistCount safelistCnt = new SafelistCount(account, getSafelistCnt(account));
            safelistCounts.add(safelistCnt);
        }
        return safelistCounts;
    }

    /**
     * See if this address exists within list of strings.
     * 
     * @param  urs     Set of addresses.
     * @param  addrStr Address to check
     * @return         true of address exists, false otherwise.
     */
    private boolean checkAddr(Set<String> urs, String addrStr)
    {
        if (null != addrStr) {
            synchronized (allSndrsLock) {
                for (String ur : urs) {
                    Map<String, Pattern> m = m_allSndrs.get(ur);
                    if (null == m) {
                        continue;
                    }
                    if (true == m.containsKey(addrStr)) {
                        m_logger.debug("literal match, sender: " + addrStr);
                        return true;
                    } else { // is not a literal match so try limited regex
                             // match
                        Pattern sndrPattern;
                        Matcher sndrMatcher;

                        for (Iterator<Pattern> iter = m.values().iterator(); true == iter.hasNext();) {
                            sndrPattern = iter.next();
                            sndrMatcher = sndrPattern.matcher(addrStr);
                            if (true == sndrMatcher.matches()) {
                                m_logger.debug("pattern match: " + sndrPattern + ", sender: " + addrStr);
                                return true;
                            }
                        }
                    }
                }
            }
        }

        return false;
    }

    /**
     * Refresh (add/delete/update) safelists for this recipient
     *
     * @param rcpnt Recipient address.
     * @param sndrs List of sender email address strings to replace.
     */
    private synchronized void setSndrs(String rcpnt, List<String> sndrs)
    {
        if (null == rcpnt) {
            m_logger.debug("refreshing all safelists");
        } else {
            m_logger.debug("recipient: " + rcpnt + ", updating safelist: " + sndrs);
            rcpnt = rcpnt.toLowerCase();
        }

        HashMap<String, ArrayList<SafelistSettings>> allHMSafelistsByRcpnt = new HashMap<String, ArrayList<SafelistSettings>>();
        HashMap<String, String> allHMSndrs = new HashMap<String, String>();
        // cast list because xdoclet does not support java 1.5
        List<SafelistSettings> safelists = mlSettings.getSafelistSettings();

        ArrayList<SafelistSettings> rcpntHMSafelists;
        String slRcpnt;
        String slSndr;
        String tmpRcpnt;
        String tmpSndr;

        // create temporary caches of safelists
        for (SafelistSettings safelist : safelists) {
            // recipient may exist in multiple safelists
            // but we only use one copy of recipient
            slRcpnt = safelist.getRecipient();
            tmpRcpnt = slRcpnt;

            // sender may exist in multiple safelists
            // but we only use one copy of sender
            slSndr = safelist.getSender();
            tmpSndr = slSndr;

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
                slRcpnt = new String(curRcpnt);
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
                    slSndr = new String(curSndr);
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

    /**
     * Get first safelist this uer belongs to.
     *
     * @param  sndr      Sender to check.
     * @param  safelists Safelists to check.
     * @return           First SafeListSettings the sender address belongs to.
     */
    private SafelistSettings getSndrSafelist(String sndr, List<SafelistSettings> safelists)
    {
        if (null == safelists) {
            return null;
        }

        for (SafelistSettings safelist : safelists) {
            if (true == sndr.equals(safelist.getSender())) {
                return safelist;
            }
        }

        return null;
    }

    /**
     * Set safelists.
     *
     * @param safelists List of safelists to set in settings.
     */
    private void setHMSafelists(List<SafelistSettings> safelists)
    {
        m_logger.debug("setting safelists, size: " + safelists.size());

        mlSettings.setSafelistSettings(safelists);

        mlImpl.setSmtpSettings(mlSettings);

        return;
    }

    /**
     * Get (refresh) safelists for this recipient
     *
     * @param  rcpnt Email address to lookup.
     * @return       List of strings of safelists.
     */
    private List<String> getSndrs(String rcpnt)
    {
        return getSndrs(getHMSafelists(), rcpnt);
    }

    /**
     * Return senders in specified safelsits for the named recipient.
     *
     * @param  safelists Safelists to search.
     * @param  rcpnt     Recipient address to mathch.
     * @return           List of senders.
     */
    private List<String> getSndrs(List<SafelistSettings> safelists, String rcpnt)
    {
        return renewGetSndrs(safelists, rcpnt);
    }

    /**
     * Return safelists.
     * 
     * @return Safelists from smtp settings.
     */
    private List<SafelistSettings> getHMSafelists()
    {
        // cast list because xdoclet does not support java 1.5
        return mlSettings.getSafelistSettings();
    }

    /**
     * Renew safelists.
     * 
     * @param safelists safelists to add.
     */
    private void renew(List<SafelistSettings> safelists)
    {
        m_logger.debug("reading all safelists");

        Map<String, List<String>> sndrsByRcpnt = new HashMap<String, List<String>>();
        Map<String, Map<String, Pattern>> allSndrs = new HashMap<String, Map<String, Pattern>>();

        String slRcpnt;
        String slSndr;
        List<String> sndrs;
        String rcpnt;
        String sndr;

        for (SafelistSettings safelist : safelists) {
            // we implicitly ignore duplicates w/ HashMap
            slRcpnt = safelist.getRecipient();
            rcpnt = slRcpnt.toLowerCase();

            slSndr = safelist.getSender();
            sndr = slSndr.toLowerCase();

            // m_logger.debug("using safelist: " + safelist + ", recipient: " +
            // rcpnt + ", sender: " + sndr);

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

    /**
     * add/replace sndr - assumes sndr is already in lower-case format
     *
     * @param sndrs Map of safelists.
     * @param rcpnt Recipient.
     * @param sndr  Sender.
     */
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

    /**
     * Remove sender from safelists.  assumes sndr is already in lower-case format
     *
     * @param sndrs Map of safelists.
     * @param rcpnt Recipient.
     * @param sndr  Sender.
     * @return sender address if found, null otherwise.
     */
    private Pattern removeSndr(Map<String, Map<String, Pattern>> sndrs, String rcpnt, String sndr)
    {
        Map<String, Pattern> m = sndrs.get(rcpnt);
        if (null == m) {
            return null;
        } else {
            return m.remove(sndr);
        }
    }

    /**
     * Renew get senders.
     *
     * @param  safelists Safelists.
     * @param  rcpnt     Safelist owner.
     * @return           List of string of senders.
     */
    private List<String> renewGetSndrs(List<SafelistSettings> safelists, String rcpnt)
    {
        renew(safelists);
        // if null, recipient has no safelist
        return (m_sndrsByRcpnt.get(rcpnt.toLowerCase()));
    }

    /**
     * Create safelist.
     * @param  rcpnt                         Safelist owner.
     * @return                               Senders in safelist.
     * @throws SafelistActionFailedException If safelist cannot be created.
     */
    private List<String> createSL(String rcpnt) throws SafelistActionFailedException
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

    /**
     * Convert a list of strings as an array.
     *
     * @param  strs List of strings.
     * @return      Array of strings.
     */
    private String[] toStringArray(List<String> strs)
    {
        return strs.toArray(new String[strs.size()]);
    }
}
