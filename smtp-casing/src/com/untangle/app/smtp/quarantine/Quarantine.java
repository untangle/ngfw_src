/**
 * $Id$
 */
package com.untangle.app.smtp.quarantine;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.mail.internet.InternetAddress;

import org.apache.log4j.Logger;

import com.untangle.app.smtp.EmailAddressPairRule;
import com.untangle.app.smtp.EmailAddressRule;
import com.untangle.app.smtp.GlobEmailAddressList;
import com.untangle.app.smtp.GlobEmailAddressMapper;
import com.untangle.app.smtp.SmtpImpl;
import com.untangle.app.smtp.SmtpSettings;
import com.untangle.app.smtp.mime.MIMEUtil;
import com.untangle.app.smtp.quarantine.store.InboxSummary;
import com.untangle.app.smtp.quarantine.store.QuarantineStore;
import com.untangle.uvm.util.Pair;
import com.untangle.uvm.UvmContextFactory;
import com.untangle.uvm.app.DayOfWeekMatcher;
import com.untangle.uvm.util.I18nUtil;

/**
 *
 */
public class Quarantine implements QuarantineAppView, QuarantineMaintenenceView, QuarantineUserView
{
    private final Logger logger = Logger.getLogger(Quarantine.class);

    private static final String CRON_STRING = "* * * root /usr/share/untangle/bin/smtp-send-quarantine-digests.py >/dev/null 2>&1";
    private static final File CRON_FILE = new File("/etc/cron.d/untangle-smtp-quarantine-digests");
    
    private static final long ONE_DAY = (1000L * 60L * 60L * 24L);
    private static final int DIGEST_SEND_DELAY_MILLISEC = 500;

    private QuarantineStore store;
    private DigestGenerator digestGenerator;
    private AuthTokenManager atm;
    private QuarantineSettings settings = new QuarantineSettings();
    private GlobEmailAddressList quarantineForList;
    private GlobEmailAddressMapper addressAliases;
    private SmtpImpl impl;
    private boolean opened = false;

    public Quarantine()
    {
        this.store = new QuarantineStore(new File(new File(System.getProperty("uvm.conf.dir")), "quarantine"));
        this.digestGenerator = new DigestGenerator();
        this.atm = new AuthTokenManager();
        this.quarantineForList = new GlobEmailAddressList(java.util.Arrays.asList(new String[] { "*" }));
        this.addressAliases = new GlobEmailAddressMapper(new ArrayList<Pair<String, String>>());
    }

    /**
     * Properties are not maintained explicitly by the Quarantine (i.e. the UI does not talk to the Quarantine).
     */
    public void setSettings(SmtpImpl impl, QuarantineSettings settings)
    {
        this.impl = impl;
        this.settings = settings;

        this.atm.setKey(this.settings.grabBinaryKey());

        // Handle nulls (defaults)
        if (settings.getAllowedAddressPatterns() == null || settings.getAllowedAddressPatterns().size() == 0) {
            settings.setAllowedAddressPatterns( java.util.Arrays.asList( new EmailAddressRule[] {new EmailAddressRule("*")}));
        }
        if (settings.getAddressRemaps() == null) {
            settings.setAddressRemaps(new ArrayList<EmailAddressPairRule>());
        }

        // Update address mapping
        this.addressAliases = new GlobEmailAddressMapper(fromEmailAddressRuleListPair(settings.getAddressRemaps()));

        // Update the quarantine-for stuff
        this.quarantineForList = new GlobEmailAddressList( fromEmailAddressRule(settings.getAllowedAddressPatterns()));

        writeCronFile();
    }

    private void writeCronFile()
    {
        // write the cron file for nightly runs
        String conf = this.settings.getDigestMinuteOfDay() + " " + this.settings.getDigestHourOfDay() + " " + CRON_STRING;
        BufferedWriter out = null;
        try {
            out = new BufferedWriter(new FileWriter(CRON_FILE));
            out.write(conf, 0, conf.length());
            out.write("\n");
        } catch (IOException ex) {
            logger.error("Unable to write file", ex);
            return;
        }
        try {
            out.close();
        } catch (IOException ex) {
            logger.error("Unable to close file", ex);
            return;
        }
    }

    /**
     * Call that the Quarantine should "open"
     */
    public void open()
    {
    }

    /**
     * Tell the quarantine that it is closing. Stray calls may still be made (thread timing), but will likely be slower.
     */
    public void close()
    {
        this.store.close();
    }

    /**
     * Callback from the Chron thread that we should send digests and purge the store.
     */
    public void sendQuarantineDigests()
    {
        logger.info("Quarantine Digest Cron running...");
        pruneStoreNow();

        if (this.settings.getSendDailyDigests()) {
            logger.info("Sending Quarantine Digests...");
            sendDigestsNow();
        }
    }

    public void pruneStoreNow()
    {
        this.store.prune(this.settings.getMaxMailIntern(), this.settings.getMaxIdleInbox());
    }

    /**
     * Warning - this method executes synchronously
     */
    public void sendDigestsNow()
    {
        List<InboxSummary> allInboxes = this.store.listInboxes();
        long cutoff = System.currentTimeMillis() - ONE_DAY;

        for (InboxSummary inbox : allInboxes) {

            Pair<QuarantineStore.GenericStatus, InboxIndex> result = this.store.getIndex(inbox.getAddress());

            if (result.a == QuarantineStore.GenericStatus.SUCCESS) {
                if (result.b.size() > 0) {
                    if (result.b.getNewestMailTimestamp() < cutoff) {
                        logger.debug("No need to send digest to \"" + inbox.getAddress()
                                + "\", no new mails in last 24 hours (" + result.b.getNewestMailTimestamp()
                                + " millis)");
                    } else {
                        if (sendDigestEmail(inbox.getAddress(), result.b)) {
                            logger.info("Sent digest to \"" + inbox.getAddress() + "\"");
                        } else {
                            logger.warn("Unable to send digest to \"" + inbox.getAddress() + "\"");
                        }

                        try {
                            Thread.sleep(DIGEST_SEND_DELAY_MILLISEC);
                        } catch (java.lang.InterruptedException e) {
                        }
                    }
                } else {
                    logger.debug("No need to send digest to \"" + inbox.getAddress() + "\", no mails");
                }
            }
        }
    }

    @Override
    public boolean quarantineMail(File file, MailSummary summary, InternetAddress... recipients)
    {
        // Test against our list of stuff we are permitted to quarantine for
        for (InternetAddress eAddr : recipients) {
            if (eAddr == null || MIMEUtil.isNullAddress(eAddr)) {
                continue;
            }
            if (!this.quarantineForList.contains(eAddr.getAddress())) {
                logger.debug("Not permitting mail to be quarantined as address \"" + eAddr.getAddress()
                        + "\" does not conform to patterns of addresses " + "we will quarantine-for");
                return false;
            }
        }

        // Here is the tricky part. First off, we assume that a given
        // recipient has not been addressed more than once (if they have,
        // then they would have gotten duplicates and we will not change that
        // situation).
        //
        // Now, for each recipient we may need to place the mail
        // into a different quarantine. However, we must preserve the original
        // recipient(s). In fact, if the mail was to "foo@moo.com" but
        // quarantined into "fred@yahoo"'s inbox, when released it should go
        // to "foo" and not "fred".
        Map<String, List<String>> recipientGroupings = new HashMap<String, List<String>>();

        for (InternetAddress eAddr : recipients) {
            // Skip null address
            if (eAddr == null || MIMEUtil.isNullAddress(eAddr)) {
                continue;
            }

            String recipientAddress = eAddr.getAddress().toLowerCase();
            String inboxAddress = this.addressAliases.getAddressMapping(recipientAddress);

            if (inboxAddress != null) {
                logger.debug("Recipient \"" + recipientAddress + "\" remaps to \"" + inboxAddress + "\"");
            } else {
                inboxAddress = recipientAddress;
            }

            List<String> listForInbox = recipientGroupings.get(inboxAddress);
            if (listForInbox == null) {
                listForInbox = new ArrayList<String>();
                recipientGroupings.put(inboxAddress, listForInbox);
            }
            listForInbox.add(recipientAddress);
        }

        // Now go ahead and perform inserts. Note that we could save a few cycles
        // by breaking-out the (common) case of a mail with a single recipient
        ArrayList<Pair<String, String>> outcomeList = new ArrayList<Pair<String, String>>();
        boolean allSuccess = true;

        for (Map.Entry<String, List<String>> entry : recipientGroupings.entrySet()) {
            String inboxAddress = entry.getKey();

            // Get recipients as an array
            String[] recipientsForThisInbox = entry.getValue().toArray(new String[entry.getValue().size()]);

            // Perform the insert
            Pair<QuarantineStore.AdditionStatus, String> result = this.store.quarantineMail(file, inboxAddress,
                    recipientsForThisInbox, summary, false);
            if (result.a == QuarantineStore.AdditionStatus.FAILURE) {
                allSuccess = false;
                break;
            } else {
                outcomeList.add(new Pair<String, String>(inboxAddress, result.b));
            }
        }

        // Rollback
        if (!allSuccess) {
            logger.debug("Quarantine for multiple recipients had failure.  Rollback any success");
            for (Pair<String, String> addition : outcomeList) {
                this.store.purge(addition.a, addition.b);
            }
            return false;
        }
        return true;

    }

    // --QuarantineManipulation--
    @Override
    public InboxIndex purge(String account, String... doomedMails) throws NoSuchInboxException,
            QuarantineUserActionFailedException
    {
        Pair<QuarantineStore.GenericStatus, InboxIndex> result = this.store.purge(account, doomedMails);
        checkAndThrowCommonErrors(result.a, account);
        return result.b;
    }

    @Override
    public InboxIndex rescue(String account, String... rescuedMails) throws NoSuchInboxException,
            QuarantineUserActionFailedException
    {
        Pair<QuarantineStore.GenericStatus, InboxIndex> result = this.store.rescue(account, rescuedMails);
        checkAndThrowCommonErrors(result.a, account);
        return result.b;
    }

    // --QuarantineMaintenenceView --
    @Override
    public void rescueInbox(String account) throws NoSuchInboxException, QuarantineUserActionFailedException
    {
        InboxIndex index = getInboxIndex(account);

        String[] ids = new String[index.size()];
        int ptr = 0;
        for (InboxRecord record : index) {
            ids[ptr++] = record.getMailID();
        }
        rescue(account, ids);
    }

    @Override
    public void rescueInboxes(String[] accounts) throws NoSuchInboxException, QuarantineUserActionFailedException
    {
        if (accounts != null && accounts.length > 0) {
            for (int i = 0; i < accounts.length; i++) {
                rescueInbox(accounts[i]);
            }
        }
    }

    private InboxIndex getInboxIndex(String account) throws NoSuchInboxException, QuarantineUserActionFailedException
    {
        Pair<QuarantineStore.GenericStatus, InboxIndex> result = this.store.getIndex(account);
        checkAndThrowCommonErrors(result.a, account);
        return result.b;
    }

    @Override
    public List<InboxRecord> getInboxRecords(String account) throws NoSuchInboxException,
            QuarantineUserActionFailedException
    {
        InboxIndex index = getInboxIndex(account);
        return Arrays.asList(index.allRecords());
    }

    @Override
    public long getInboxesTotalSize() throws QuarantineUserActionFailedException
    {
        return this.store.getTotalSize();
    }

    @Override
    public String getFormattedInboxesTotalSize(boolean inMB)
    {
        return this.store.getFormattedTotalSize(inMB);
    }

    @Override
    public List<InboxSummary> listInboxes() throws QuarantineUserActionFailedException
    {
        return this.store.listInboxes();
    }

    @Override
    public void deleteInbox(String account) throws NoSuchInboxException, QuarantineUserActionFailedException
    {
        switch (this.store.deleteInbox(account)) {
            case NO_SUCH_INBOX:
                // Just supress this one for now
            case SUCCESS:
                break;//
            case ERROR:
                throw new QuarantineUserActionFailedException("Unable to delete inbox");
        }
    }

    @Override
    public void deleteInboxes(String[] accounts) throws NoSuchInboxException, QuarantineUserActionFailedException
    {
        if (accounts != null && accounts.length > 0) {
            for (int i = 0; i < accounts.length; i++) {
                deleteInbox(accounts[i]);
            }
        }
    }

    @Override
    public String getAccountFromToken(String token) throws BadTokenException
    {
        Pair<AuthTokenManager.DecryptOutcome, String> p = this.atm.decryptAuthToken(token);

        if (p.a != AuthTokenManager.DecryptOutcome.OK) {
            throw new BadTokenException(token);
        }

        return p.b;
    }

    @Override
    public String createAuthToken(String account)
    {
        return this.atm.createAuthToken(account.trim());
    }

    @Override
    public boolean requestDigestEmail(String account) throws NoSuchInboxException, QuarantineUserActionFailedException
    {
        boolean ret = sendDigestEmail(account, getInboxIndex(account));

        if (!ret) {
            logger.warn("Unable to send digest email to account \"" + account + "\"");
        }

        return true;
    }

    @Override
    public void remapSelfService(String from, String to) throws QuarantineUserActionFailedException,
            InboxAlreadyRemappedException
    {
        if ((from == null) || (to == null)) {
            logger.warn("empty from or to string.");
            return;
        }

        if ((from.length() == 0) || (to.length() == 0)) {
            logger.warn("empty from or to string.");
            return;
        }

        GlobEmailAddressMapper currentMapper = null;

        /* Remove the current map if one exists. */
        String existingMapping = this.addressAliases.getAddressMapping(from);
        if (existingMapping != null) {
            currentMapper = this.addressAliases.removeMapping(new Pair<String, String>(from, existingMapping));
        }

        if (currentMapper == null)
            currentMapper = this.addressAliases;

        // Create a new List
        List<Pair<String, String>> mappings = currentMapper.getRawMappings();
        mappings.add(0, new Pair<String, String>(from, to));

        // Convert list to form which makes settings happy
        List<EmailAddressPairRule> newMappingsList = toEmailAddressPairRuleList(mappings);

        SmtpSettings settings = this.impl.getSmtpSettings();
        settings.getQuarantineSettings().setAddressRemaps(newMappingsList);

        this.impl.setSmtpSettings(settings);
    }

    @Override
    public boolean unmapSelfService(String inboxName, String aliasToRemove) throws QuarantineUserActionFailedException
    {
        if ((inboxName == null) || (aliasToRemove == null)) {
            logger.warn("empty from or to string.");
            return false;
        }

        if ((inboxName.length() == 0) || (aliasToRemove.length() == 0)) {
            logger.warn("empty from or to string.");
            return false;
        }

        GlobEmailAddressMapper newMapper = this.addressAliases.removeMapping(new Pair<String, String>(aliasToRemove,
                inboxName));

        if (newMapper == null) {
            return false;
        }
        // Create a new List
        List<Pair<String, String>> mappings = newMapper.getRawMappings();

        // Convert list to form which makes settings happy
        List<EmailAddressPairRule> newMappingsList = toEmailAddressPairRuleList(mappings);

        SmtpSettings settings = this.impl.getSmtpSettings();
        settings.getQuarantineSettings().setAddressRemaps(newMappingsList);

        this.impl.setSmtpSettings(settings);

        return true;
    }

    @Override
    public String getMappedTo(String account) throws QuarantineUserActionFailedException
    {
        return this.addressAliases.getAddressMapping(account);

    }

    public String getUltimateRecipient(String address) throws QuarantineUserActionFailedException
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

    @Override
    public String[] getMappedFrom(String account) throws QuarantineUserActionFailedException
    {

        return this.addressAliases.getReverseMapping(account);
    }

    /**
     * Helper method which sends a digest email. Returns false if there was an error in sending of template merging
     */
    private boolean sendDigestEmail(String account, InboxIndex index)
    {
        Map<String, String> i18nMap = UvmContextFactory.context().languageManager().getTranslations("untangle");
        I18nUtil i18nUtil = new I18nUtil(i18nMap);
        String internalHost = UvmContextFactory.context().networkManager().getPublicUrl();

        if (internalHost == null) {
            logger.warn("Unable to determine internal interface");
            return false;
        }
        String[] recipients = { account };
        String subject = i18nUtil.tr("Quarantine Digest");

        String bodyHtml = this.digestGenerator.generateMsgBody(internalHost, account, this.atm, i18nUtil);

        // Attempt the send
        boolean ret = UvmContextFactory.context().mailSender().sendHtmlMessage(recipients, subject, bodyHtml);

        return ret;
    }

    private void checkAndThrowCommonErrors(QuarantineStore.GenericStatus status, String account)
            throws NoSuchInboxException, QuarantineUserActionFailedException
    {

        if (status == QuarantineStore.GenericStatus.NO_SUCH_INBOX) {
            throw new NoSuchInboxException(account);
        } else if (status == QuarantineStore.GenericStatus.ERROR) {
            throw new QuarantineUserActionFailedException();
        }
    }

    @Override
    public void test()
    {

    }

    private List<EmailAddressPairRule> toEmailAddressPairRuleList(List<Pair<String, String>> typedList)
    {
        ArrayList<EmailAddressPairRule> ret = new ArrayList<EmailAddressPairRule>();

        for (Pair<String, String> pair : typedList) {
            ret.add(new EmailAddressPairRule(pair.a, pair.b));
        }
        return ret;
    }

    private List<Pair<String, String>> fromEmailAddressRuleListPair(List<EmailAddressPairRule> list)
    {
        ArrayList<Pair<String, String>> ret = new ArrayList<Pair<String, String>>();

        for (EmailAddressPairRule eaPair : list) {
            ret.add(new Pair<String, String>(eaPair.getAddress1(), eaPair.getAddress2()));
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
}
