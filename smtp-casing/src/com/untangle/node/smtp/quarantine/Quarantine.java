/**
 * $Id$
 */
package com.untangle.node.smtp.quarantine;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.log4j.Logger;

import com.untangle.node.smtp.EmailAddressPairRule;
import com.untangle.node.smtp.EmailAddressRule;
import com.untangle.node.smtp.GlobEmailAddressList;
import com.untangle.node.smtp.GlobEmailAddressMapper;
import com.untangle.node.smtp.SmtpNodeImpl;
import com.untangle.node.smtp.SmtpNodeSettings;
import com.untangle.node.smtp.mime.EmailAddress;
import com.untangle.node.smtp.quarantine.store.QuarantinePruningObserver;
import com.untangle.node.smtp.quarantine.store.QuarantineStore;
import com.untangle.node.util.IOUtil;
import com.untangle.node.util.Pair;
import com.untangle.uvm.CronJob;
import com.untangle.uvm.UvmContextFactory;
import com.untangle.uvm.node.DayOfWeekMatcher;
import com.untangle.uvm.util.I18nUtil;

/**
 *
 */
public class Quarantine implements QuarantineNodeView, QuarantineMaintenenceView, QuarantineUserView
{
    private final Logger logger = Logger.getLogger(Quarantine.class);
    
    private static final long ONE_DAY = (1000L * 60L * 60L * 24L);
    private static final int DIGEST_SEND_DELAY_MILLISEC = 500; 
    
    private final Logger m_logger = Logger.getLogger(Quarantine.class);
    private QuarantineStore m_store;
    private RescueEjectionHandler m_rescueHandler = new RescueEjectionHandler();
    private DigestGenerator m_digestGenerator;
    private AuthTokenManager m_atm;
    private QuarantineSettings m_settings = new QuarantineSettings();
    private CronJob m_cronJob;
    private GlobEmailAddressList m_quarantineForList;
    private GlobEmailAddressMapper m_addressAliases;
    private SmtpNodeImpl m_impl;
    private boolean m_opened = false;

    private static final Map<String, InboxComparator.SortBy> NAME_TO_I_SORT_BY;
    private static final InboxComparator.SortBy DEFAULT_I_SORT_COLUMN = InboxComparator.SortBy.ADDRESS;

    private static final Map<String, InboxRecordComparator.SortBy> NAME_TO_IR_SORT_BY;
    private static final InboxRecordComparator.SortBy DEFAULT_IR_SORT_COLUMN = InboxRecordComparator.SortBy.INTERN_DATE;

    public Quarantine()
    {
        m_store = new QuarantineStore( new File(new File(System.getProperty("uvm.conf.dir")), "quarantine") );
        m_digestGenerator = new DigestGenerator();
        m_atm = new AuthTokenManager();

        m_quarantineForList = new GlobEmailAddressList( java.util.Arrays.asList(new String[] {"*"}));

        m_addressAliases = new GlobEmailAddressMapper(new ArrayList<Pair<String, String>>());
    }

    /**
     * Properties are not maintained explicitly
     * by the Quarantine (i.e. the UI does not
     * talk to the Quarantine).
     */
    public void setSettings( SmtpNodeImpl impl, QuarantineSettings settings )
    {
        m_impl = impl;
        m_settings = settings;

        m_atm.setKey(m_settings.grabBinaryKey());

        //Handle nulls (defaults)
        if(settings.getAllowedAddressPatterns() == null ||
           settings.getAllowedAddressPatterns().size() == 0) {
            settings.setAllowedAddressPatterns( java.util.Arrays.asList( new EmailAddressRule[] {new EmailAddressRule("*")}));
        }
        if(settings.getAddressRemaps() == null) {
            settings.setAddressRemaps(new ArrayList<EmailAddressPairRule>());
        }

        //Update address mapping
        m_addressAliases = new GlobEmailAddressMapper( fromEmailAddressRuleListPair(settings.getAddressRemaps()));

        //Update the quarantine-for stuff
        m_quarantineForList = new GlobEmailAddressList( fromEmailAddressRule(settings.getAllowedAddressPatterns()));

        if (null != m_cronJob) {
            int h = m_settings.getDigestHourOfDay();
            int m = m_settings.getDigestMinuteOfDay();
            try {
                m_cronJob.reschedule(DayOfWeekMatcher.getAnyMatcher(), h, m);
            } catch (Exception e) {
                logger.warn( "Exception rescheduling cronjob.", e );
            }
        }
    }


    /**
     * Call that the Quarantine should "open"
     */
    public void open()
    {
        if(!m_opened) {
            synchronized(this) {
                if(!m_opened) {
                    m_opened = true;
                    int hour = 6;
                    int minute = 0;
                    if (m_settings != null) {
                        hour = m_settings.getDigestHourOfDay();
                        minute = m_settings.getDigestMinuteOfDay();
                    }

                    Runnable r = new Runnable()
                        {
                            public void run()
                            {
                                cronCallback();
                            }
                        };
                    m_cronJob = UvmContextFactory.context().makeCronJob(DayOfWeekMatcher.getAnyMatcher(), hour, minute, r);
                }
            }
        }
    }

    /**
     * Tell the quarantine that it is closing.  Stray calls
     * may still be made (thread timing), but will likely be
     * slower.
     */
    public void close()
    {
        m_store.close();
        if (null != m_cronJob) {
            m_cronJob.cancel();
        }
    }

    /**
     * Callback from the Chron thread that we should send
     * digests and purge the store.
     */
    void cronCallback()
    {
        m_logger.info("Quarantine management cron running...");
        pruneStoreNow();

        if (m_settings.getSendDailyDigests())
            sendDigestsNow();
    }

    public void pruneStoreNow()
    {
        m_store.prune(m_settings.getMaxMailIntern(), m_settings.getMaxIdleInbox(), QuarantinePruningObserver.NOOP);
    }

    /**
     * Warning - this method executes synchronously
     */
    public void sendDigestsNow()
    {
        List<Inbox> allInboxes = m_store.listInboxes();
        long cutoff = System.currentTimeMillis() - ONE_DAY;

        for(Inbox inbox : allInboxes) {

            Pair<QuarantineStore.GenericStatus, InboxIndex> result =
                m_store.getIndex(inbox.getAddress());

            if(result.a == QuarantineStore.GenericStatus.SUCCESS) {
                if(result.b.size() > 0) {
                    if(result.b.getNewestMailTimestamp() < cutoff) {
                        m_logger.debug("No need to send digest to \"" +
                                       inbox.getAddress() + "\", no new mails in last 24 hours (" +
                                       result.b.getNewestMailTimestamp() + " millis)");
                    } else {
                        if(sendDigestEmail(inbox.getAddress(), result.b)) {
                            m_logger.info("Sent digest to \"" + inbox.getAddress() + "\"");
                        } else {
                            m_logger.warn("Unable to send digest to \"" + inbox.getAddress() + "\"");
                        }

                        try {Thread.sleep(DIGEST_SEND_DELAY_MILLISEC);} catch (java.lang.InterruptedException e) {}
                    }
                } else {
                    m_logger.debug("No need to send digest to \"" + inbox.getAddress() + "\", no mails");
                }
            }
        }
    }

    //--QuarantineNodeView--

    public boolean quarantineMail(File file, MailSummary summary, EmailAddress...recipients)
    {
        //Check for out-of-space condition
        if(m_store.getTotalSize() > m_settings.getMaxQuarantineTotalSz()) {
            //TODO This will be very anoying, as we'll have *way* too many
            //error messages in the logs
            //
            //TODO bscott Shouldn't we at least once take a SWAG at
            //pruning the store?  It should reduce the size by ~1/14th
            //in a default configuration.
            m_logger.warn("Quarantine size of " + m_store.getTotalSize() +
                          " exceeds max of " + m_settings.getMaxQuarantineTotalSz());
            return false;
        }

        //If we do not have an internal IP, then
        //don't even bother quarantining
        if(UvmContextFactory.context().systemManager().getPublicUrl() == null) {
            m_logger.warn("No valid IP, so no way for folks to connect to quarantine.  Abort quarantining");
            return false;
        }

        //Test against our list of stuff we
        //are permitted to quarantine for
        for(EmailAddress eAddr : recipients) {
            if(eAddr == null || eAddr.isNullAddress()) {
                continue;
            }
            if(!m_quarantineForList.contains(eAddr.getAddress())) {
                m_logger.debug("Not permitting mail to be quarantined as address \"" +
                               eAddr.getAddress() + "\" does not conform to patterns of addresses " +
                               "we will quarantine-for");
                return false;
            }
        }

        //Here is the tricky part.  First off, we assume that a given
        //recipient has not been addressed more than once (if they have,
        //then they would have gotten duplicates and we will not change that
        //situation).
        //
        //Now, for each recipient we may need to place the mail
        //into a different quarantine.  However, we must preserve the original
        //recipient(s).  In fact, if the mail was to "foo@moo.com" but
        //quarantined into "fred@yahoo"'s inbox, when released it should go
        //to "foo" and not "fred".
        Map<String, List<String>> recipientGroupings =
            new HashMap<String, List<String>>();

        for(EmailAddress eAddr : recipients) {
            //Skip null address
            if(eAddr == null || eAddr.isNullAddress()) {
                continue;
            }

            String recipientAddress = eAddr.getAddress().toLowerCase();
            String inboxAddress = m_addressAliases.getAddressMapping(recipientAddress);

            if(inboxAddress != null) {
                m_logger.debug("Recipient \"" + recipientAddress + "\" remaps to \"" + inboxAddress + "\"");
            } else {
                inboxAddress = recipientAddress;
            }

            List<String> listForInbox = recipientGroupings.get(inboxAddress);
            if(listForInbox == null) {
                listForInbox = new ArrayList<String>();
                recipientGroupings.put(inboxAddress, listForInbox);
            }
            listForInbox.add(recipientAddress);
        }

        //Now go ahead and perform inserts.  Note that we could save a few cycles
        //by breaking-out the (common) case of a mail with a single recipient
        ArrayList<Pair<String, String>> outcomeList = new ArrayList<Pair<String, String>>();
        boolean allSuccess = true;

        for(Map.Entry<String, List<String>> entry : recipientGroupings.entrySet()) {
            String inboxAddress = entry.getKey();

            //Get recipients as an array
            String[] recipientsForThisInbox = entry.getValue().toArray(new String[entry.getValue().size()]);

            //Perform the insert
            Pair<QuarantineStore.AdditionStatus, String> result =
                m_store.quarantineMail(file,
                                       inboxAddress,
                                       recipientsForThisInbox,
                                       summary,
                                       false);
            if(result.a == QuarantineStore.AdditionStatus.FAILURE) {
                allSuccess = false;
                break;
            } else {
                outcomeList.add(new Pair<String, String>(inboxAddress, result.b));
            }
        }

        //Rollback
        if(!allSuccess) {
            m_logger.debug("Quarantine for multiple recipients had failure.  Rollback any success");
            for(Pair<String, String> addition : outcomeList) {
                m_store.purge(addition.a, addition.b);
            }
            return false;
        }
        return true;

        /*
        //Perform any remapping
        ArrayList<String> sRecipients = new ArrayList<String>();
        for(EmailAddress eAddr : recipients) {
        if(eAddr == null || eAddr.isNullAddress()) {
        continue;
        }
        String addr = eAddr.getAddress().toLowerCase();
        String remapped = m_addressAliases.getAddressMapping(addr);
        if(remapped != null) {
        m_logger.debug("Remapping \"" +
        addr + "\" to \"" + remapped + "\"");
        addr = remapped.toLowerCase();
        }
        if(sRecipients.contains(addr)) {
        continue;
        }
        sRecipients.add(addr);
        }


        if(sRecipients.size() == 1) {
        return m_store.quarantineMail(file,
        sRecipients.get(0),
        summary,
        true).a != QuarantineStore.AdditionStatus.FAILURE;
        } else {
        ArrayList<Pair<String, String>> list = new ArrayList<Pair<String, String>>();
        boolean allSuccess = true;
        for(String addr : sRecipients) {
        Pair<QuarantineStore.AdditionStatus, String> result =
        m_store.quarantineMail(file,
        addr,
        summary,
        false);
        if(result.a == QuarantineStore.AdditionStatus.FAILURE) {
        allSuccess = false;
        break;
        }
        else {
        list.add(new Pair<String, String>(addr, result.b));
        }
        }
        //Rollback
        if(!allSuccess) {
        m_logger.debug("Quarantine for multiple recipients had failure.  Rollback " +
        "any success");
        for(Pair<String, String> addition : list) {
        m_store.purge(addition.a, addition.b);
        }
        return false;
        }
        return true;
        }
        */
    }


    //--QuarantineManipulation--

    public InboxIndex purge(String account,
                            String...doomedMails)
        throws NoSuchInboxException, QuarantineUserActionFailedException {

        Pair<QuarantineStore.GenericStatus, InboxIndex> result =
            m_store.purge(account, doomedMails);

        checkAndThrowCommonErrors(result.a, account);

        return result.b;
    }

    public InboxIndex rescue(String account, String...rescuedMails)
        throws NoSuchInboxException, QuarantineUserActionFailedException
    {
        Pair<QuarantineStore.GenericStatus, InboxIndex> result =
            m_store.rescue(account, m_rescueHandler, rescuedMails);

        checkAndThrowCommonErrors(result.a, account);

        return result.b;
    }

    public void rescueInbox(String account)
        throws NoSuchInboxException, QuarantineUserActionFailedException
    {
        InboxIndex index = getInboxIndex(account);

        String[] ids = new String[index.size()];
        int ptr = 0;
        for(InboxRecord record : index) {
            ids[ptr++] = record.getMailID();
        }
        rescue(account, ids);
    }

    public void rescueInboxes(String[] accounts)
        throws NoSuchInboxException, QuarantineUserActionFailedException
    {
        if (accounts != null && accounts.length > 0) {
            for (int i = 0; i < accounts.length; i++) {
                rescueInbox(accounts[i]);
            }
        }
    }

    public InboxIndex getInboxIndex(String account)
        throws NoSuchInboxException, QuarantineUserActionFailedException
    {

        Pair<QuarantineStore.GenericStatus, InboxIndex> result = m_store.getIndex(account);

        checkAndThrowCommonErrors(result.a, account);

        return result.b;
    }

    /**
     * Get the inboxes
     */
    public InboxArray getInboxArray( int start, int limit, String sortColumn, boolean isAscending )
        throws QuarantineUserActionFailedException
    {
        List<Inbox> inboxList = m_store.listInboxes();
        if ( inboxList == null ) return new InboxArray( new Inbox[0], 0 );

        InboxComparator.SortBy sortBy = NAME_TO_I_SORT_BY.get( sortColumn );
        if ( sortBy == null ) sortBy = DEFAULT_I_SORT_COLUMN;

        return InboxArray.getInboxArray( inboxList.toArray( new Inbox[inboxList.size()] ), sortBy,
                                         start, limit, isAscending );
    }

    public InboxRecordArray getInboxRecordArray( String account, int start, int limit, String sortColumn,
                                                 boolean isAscending )
        throws NoSuchInboxException, QuarantineUserActionFailedException
    {
        Pair<QuarantineStore.GenericStatus, InboxIndex> result = m_store.getIndex(account);

        checkAndThrowCommonErrors(result.a, account);

        InboxRecordComparator.SortBy sortBy = NAME_TO_IR_SORT_BY.get( sortColumn );
        if ( sortBy == null ) sortBy = DEFAULT_IR_SORT_COLUMN;

        InboxIndex index = result.b;

        InboxRecordCursor cursor =
            InboxRecordCursor.get( index.allRecords(), sortBy, isAscending, start, limit );

        return new InboxRecordArray( cursor.getRecords(), index.size());
    }

    public InboxRecordArray getInboxRecordArray(String account )
            throws NoSuchInboxException, QuarantineUserActionFailedException
    {
        Pair<QuarantineStore.GenericStatus, InboxIndex> result = m_store
                .getIndex(account);

        checkAndThrowCommonErrors(result.a, account);

        InboxIndex index = result.b;

        return new InboxRecordArray(index.allRecords(), index.size());
    }

    public List<InboxRecord> getInboxRecords( String account, int start, int limit, String... sortColumns)
        throws NoSuchInboxException, QuarantineUserActionFailedException
    {
        String sortColumn = null;
        boolean isAscending = true;
        if (0 < sortColumns.length) {
            sortColumn = sortColumns[0];
            if (sortColumn.startsWith("+")) {
                isAscending = true;
                sortColumn = sortColumn.substring(1);
            } else if (sortColumn.startsWith("-")) {
                isAscending = false;
                sortColumn = sortColumn.substring(1);
            } else {
                isAscending = true;
            }
        }

        InboxRecordArray inboxRecordArray = getInboxRecordArray(account, start, limit, sortColumn, isAscending);
        return Arrays.asList(inboxRecordArray.getInboxRecords());
    }

    public int getInboxTotalRecords( String account)
        throws NoSuchInboxException, QuarantineUserActionFailedException
    {
        return getInboxRecordArray(account).getTotalRecords();
    }

    public void test()
    {
        //Do nothing.
    }


    //--QuarantineMaintenenceView --

    public long getInboxesTotalSize()
        throws QuarantineUserActionFailedException
    {
        return m_store.getTotalSize();
    }

    public String getFormattedInboxesTotalSize(boolean inMB)
    {
        return m_store.getFormattedTotalSize(inMB);
    }

    public List<Inbox> listInboxes()
        throws QuarantineUserActionFailedException
    {
        return m_store.listInboxes();
    }

    public void deleteInbox(String account)
        throws NoSuchInboxException, QuarantineUserActionFailedException
    {
        switch(m_store.deleteInbox(account)) {
        case NO_SUCH_INBOX:
            //Just supress this one for now
        case SUCCESS:
            break;//
        case ERROR:
            throw new QuarantineUserActionFailedException("Unable to delete inbox");
        }
    }

    public void deleteInboxes(String[] accounts)
        throws NoSuchInboxException, QuarantineUserActionFailedException
    {
        if (accounts != null && accounts.length > 0) {
            for (int i = 0; i < accounts.length; i++) {
                deleteInbox(accounts[i]);
            }
        }
    }

    //--QuarantineUserView--

    public String getAccountFromToken(String token)
        throws /*NoSuchInboxException, */BadTokenException
    {

        Pair<AuthTokenManager.DecryptOutcome, String> p =
            m_atm.decryptAuthToken(token);

        if(p.a != AuthTokenManager.DecryptOutcome.OK) {
            throw new BadTokenException(token);
        }

        return p.b;
    }

    public String createAuthToken(String account)
    {
        return m_atm.createAuthToken(account.trim());
    }

    public boolean requestDigestEmail(String account)
        throws NoSuchInboxException, QuarantineUserActionFailedException
    {

        boolean ret = sendDigestEmail(account, getInboxIndex(account));

        if(!ret) {
            m_logger.warn("Unable to send digest email to account \"" +
                          account + "\"");
        }

        return true;
    }

    public void remapSelfService(String from, String to)
        throws QuarantineUserActionFailedException, InboxAlreadyRemappedException
    {

        if (( from == null ) || ( to == null )) {
            m_logger.warn( "empty from or to string." );
            return;
        }

        if (( from.length() == 0 ) || ( to.length() == 0 )) {
            m_logger.warn( "empty from or to string." );
            return;
        }

        GlobEmailAddressMapper currentMapper = null;

        /* Remove the current map if one exists. */
        String existingMapping = m_addressAliases.getAddressMapping(from);
        if(existingMapping != null) {
            currentMapper = m_addressAliases.removeMapping(new Pair<String, String>(from, existingMapping));
        }

        if (currentMapper == null) currentMapper = m_addressAliases;

        //Create a new List
        List<Pair<String, String>> mappings = currentMapper.getRawMappings();
        mappings.add(0, new Pair<String, String>(from, to));

        //Convert list to form which makes settings happy
        List<EmailAddressPairRule> newMappingsList = toEmailAddressPairRuleList(mappings);

        SmtpNodeSettings settings = m_impl.getSmtpNodeSettings();
        settings.getQuarantineSettings().setAddressRemaps(newMappingsList);

        m_impl.setSmtpNodeSettings(settings);
    }

    public boolean unmapSelfService(String inboxName, String aliasToRemove)
        throws QuarantineUserActionFailedException
    {
        if (( inboxName == null ) || ( aliasToRemove == null )) {
            m_logger.warn( "empty from or to string." );
            return false;
        }

        if (( inboxName.length() == 0 ) || ( aliasToRemove.length() == 0 )) {
            m_logger.warn( "empty from or to string." );
            return false;
        }

        //    System.out.println("***DEBUG*** [unmapSelfService()] Called with inbox: " +
        //      inboxName + " and aliasToRemove: " + aliasToRemove);

        GlobEmailAddressMapper newMapper =
            m_addressAliases.removeMapping(new Pair<String, String>(aliasToRemove, inboxName));

        if(newMapper == null) {
            //      System.out.println("***DEBUG*** newMapper is null!?!");
            return false;
        }
        //Create a new List
        List<Pair<String, String>> mappings = newMapper.getRawMappings();

        //Convert list to form which makes settings happy
        List<EmailAddressPairRule> newMappingsList = toEmailAddressPairRuleList(mappings);

        SmtpNodeSettings settings = m_impl.getSmtpNodeSettings();
        settings.getQuarantineSettings().setAddressRemaps(newMappingsList);

        m_impl.setSmtpNodeSettings(settings);
        //    m_addressAliases = newMapper; // use updated mapping

        //    System.out.println("***DEBUG*** Returning True");
        return true;
    }

    public String getMappedTo(String account)
        throws QuarantineUserActionFailedException
    {
        return m_addressAliases.getAddressMapping(account);

    }

    public String getUltimateRecipient(String address)
        throws QuarantineUserActionFailedException
    {
        String r = address.toLowerCase();

        Set<String> seen = new HashSet<String>();
        seen.add(r);

        String s;
        do {
            s = getMappedTo(r);
            if (null != s) {
                r = s.toLowerCase();
                if (seen.contains(r)) {
                    break;
                } else {
                    seen.add(r);
                }
            }
        } while (s != null);

        return r;
    }

    public String[] getMappedFrom(String account)
        throws QuarantineUserActionFailedException
    {
        return m_addressAliases.getReverseMapping(account);
    }

    /**
     * Helper method which sends a digest email.  Returns
     * false if there was an error in sending of template
     * merging
     */
    private boolean sendDigestEmail(String account, InboxIndex index)
    {
        Map<String,String> i18nMap = UvmContextFactory.context().languageManager().getTranslations("untangle-casing-smtp");
        I18nUtil i18nUtil = new I18nUtil(i18nMap);
        String internalHost = UvmContextFactory.context().systemManager().getPublicUrl();

        if(internalHost == null) {
            m_logger.warn("Unable to determine internal interface");
            return false;
        }
        String[] recipients = {account};
        String subject = i18nUtil.tr("Quarantine Digest");

        String bodyHtml = m_digestGenerator.generateMsgBody(internalHost, account, m_atm, i18nUtil);

        // Attempt the send
        boolean ret = UvmContextFactory.context().mailSender().sendHtmlMessage(recipients, subject, bodyHtml);

        return ret;
    }

    private void checkAndThrowCommonErrors(QuarantineStore.GenericStatus status, String account)
        throws NoSuchInboxException, QuarantineUserActionFailedException {

        if(status == QuarantineStore.GenericStatus.NO_SUCH_INBOX) {
            throw new NoSuchInboxException(account);
        } else if(status == QuarantineStore.GenericStatus.ERROR) {
            throw new QuarantineUserActionFailedException();
        }
    }

    @SuppressWarnings("unused")
    private List<EmailAddressRule> toEmailAddressRule(List<String> typedList) 
    {
        ArrayList<EmailAddressRule> ret = new ArrayList<EmailAddressRule>();

        for(String s : typedList) {
            ret.add(new EmailAddressRule(s));
        }

        return ret;
    }

    private List<String> fromEmailAddressRule(List<EmailAddressRule> list) 
    {
        ArrayList<String> ret = new ArrayList<String>();

        for(EmailAddressRule wrapper : list) {
            ret.add(wrapper.getAddress());
        }
        return ret;
    }

    private List<EmailAddressPairRule> toEmailAddressPairRuleList(List<Pair<String, String>> typedList)
    {
        ArrayList<EmailAddressPairRule> ret = new ArrayList<EmailAddressPairRule>();

        for(Pair<String, String> pair : typedList) {
            ret.add(new EmailAddressPairRule(pair.a, pair.b));
        }
        return ret;
    }

    private List<Pair<String, String>> fromEmailAddressRuleListPair(List<EmailAddressPairRule> list)
    {
        ArrayList<Pair<String, String>> ret = new ArrayList<Pair<String, String>>();

        for(EmailAddressPairRule eaPair : list) {
            ret.add(new Pair<String, String>(eaPair.getAddress1(), eaPair.getAddress2()));
        }
        return ret;
    }

    //------------- Inner Class --------------------

    private class RescueEjectionHandler implements QuarantineEjectionHandler
    {
        public void ejectMail(InboxRecord record, String inboxAddress, String[] recipients, File data)
        {
            FileInputStream fIn = null;
            try {
                fIn = new FileInputStream(data);
                BufferedInputStream bufIn = new BufferedInputStream(fIn);
                boolean success = UvmContextFactory.context().mailSender().sendMessage(bufIn, recipients);
                if(success) {
                    m_logger.debug("Released mail \"" + record.getMailID() + "\" for " + recipients.length +
                                   " recipients from inbox \"" + inboxAddress + "\"");
                } else {
                    m_logger.warn("Unable to release mail \"" + record.getMailID() + "\" for " + recipients.length +
                                  " recipients from inbox \"" + inboxAddress + "\"");
                }
            }
            catch(Exception ex) {
                m_logger.warn("Exception reading mail file for rescue", ex);
            }

            IOUtil.close(fIn);
            IOUtil.delete(data);
        }
    }

    static
    {
        Map <String,InboxRecordComparator.SortBy> irNameMap = new HashMap<String,InboxRecordComparator.SortBy>();

        irNameMap.put( "sender", InboxRecordComparator.SortBy.SENDER );
        irNameMap.put( "attachmentCount", InboxRecordComparator.SortBy.ATTACHMENT_COUNT );
        irNameMap.put( "quarantineDetail", InboxRecordComparator.SortBy.DETAIL );
        irNameMap.put( "truncatedSubject", InboxRecordComparator.SortBy.SUBJECT );
        irNameMap.put( "subject", InboxRecordComparator.SortBy.SUBJECT );
        irNameMap.put( "quarantinedDate", InboxRecordComparator.SortBy.INTERN_DATE );
        irNameMap.put( "size", InboxRecordComparator.SortBy.SIZE );

        NAME_TO_IR_SORT_BY = Collections.unmodifiableMap( irNameMap );

        Map <String,InboxComparator.SortBy> iNameMap =
            new HashMap<String,InboxComparator.SortBy>();

        iNameMap.put( "address", InboxComparator.SortBy.ADDRESS );
        iNameMap.put( "size", InboxComparator.SortBy.SIZE );
        iNameMap.put( "numberMesssages", InboxComparator.SortBy.NUMBER_MESSAGES );

        NAME_TO_I_SORT_BY = Collections.unmodifiableMap( iNameMap );

    }
}

