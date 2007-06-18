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

package com.untangle.node.mail.impl.quarantine;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.untangle.uvm.CronJob;
import com.untangle.uvm.LocalUvmContextFactory;
import com.untangle.uvm.Period;
import com.untangle.node.mail.MailNodeImpl;
import com.untangle.node.mail.impl.GlobEmailAddressList;
import com.untangle.node.mail.impl.GlobEmailAddressMapper;
import com.untangle.node.mail.impl.quarantine.store.InboxIndexImpl;
import com.untangle.node.mail.impl.quarantine.store.QuarantinePruningObserver;
import com.untangle.node.mail.impl.quarantine.store.QuarantineStore;
import com.untangle.node.mail.papi.EmailAddressPairRule;
import com.untangle.node.mail.papi.EmailAddressRule;
import com.untangle.node.mail.papi.MailNodeSettings;
import com.untangle.node.mail.papi.quarantine.BadTokenException;
import com.untangle.node.mail.papi.quarantine.Inbox;
import com.untangle.node.mail.papi.quarantine.InboxAlreadyRemappedException;
import com.untangle.node.mail.papi.quarantine.InboxIndex;
import com.untangle.node.mail.papi.quarantine.InboxRecord;
import com.untangle.node.mail.papi.quarantine.MailSummary;
import com.untangle.node.mail.papi.quarantine.NoSuchInboxException;
import com.untangle.node.mail.papi.quarantine.QuarantineEjectionHandler;
import com.untangle.node.mail.papi.quarantine.QuarantineMaintenenceView;
import com.untangle.node.mail.papi.quarantine.QuarantineManipulation;
import com.untangle.node.mail.papi.quarantine.QuarantineSettings;
import com.untangle.node.mail.papi.quarantine.QuarantineNodeView;
import com.untangle.node.mail.papi.quarantine.QuarantineUserActionFailedException;
import com.untangle.node.mail.papi.quarantine.QuarantineUserView;
import com.untangle.node.mime.EmailAddress;
import com.untangle.node.mime.MIMEMessage;
import com.untangle.node.util.ByteBufferInputStream;
import com.untangle.node.util.IOUtil;
import com.untangle.node.util.Pair;
import org.apache.log4j.Logger;

/**
 *
 */
public class Quarantine
    implements QuarantineNodeView,
               QuarantineMaintenenceView, QuarantineUserView {

    private static final long ONE_DAY = (1000L * 60L * 60L * 24L);

    private final Logger m_logger = Logger.getLogger(Quarantine.class);
    private QuarantineStore m_store;
    private RescueEjectionHandler m_rescueHandler = new RescueEjectionHandler();
    private DigestGenerator m_digestGenerator;
    private AuthTokenManager m_atm;
    private QuarantineSettings m_settings = new QuarantineSettings();
    private CronJob m_cronJob;
    private GlobEmailAddressList m_quarantineForList;
    private GlobEmailAddressMapper m_addressAliases;
    private MailNodeImpl m_impl;

    public Quarantine() {
        m_store = new QuarantineStore(
                                      new File(new File(System.getProperty("bunnicula.home")), "quarantine")
                                      );
        m_digestGenerator = new DigestGenerator();
        m_atm = new AuthTokenManager();

        m_quarantineForList = new GlobEmailAddressList(
                                                       java.util.Arrays.asList(new String[] {"*"}));

        m_addressAliases = new GlobEmailAddressMapper(new
                                                      ArrayList<Pair<String, String>>());
    }

    /**
     * Properties are not maintained explicitly
     * by the Quarantine (i.e. the UI does not
     * talk to the Quarantine).
     */
    public void setSettings(MailNodeImpl impl,
                            QuarantineSettings settings) {
        m_impl = impl;
        m_settings = settings;

        m_atm.setKey(m_settings.getSecretKey());

        //Handle nulls (defaults)
        if(settings.getAllowedAddressPatterns() == null ||
           settings.getAllowedAddressPatterns().size() == 0) {
            settings.setAllowedAddressPatterns(
                                               java.util.Arrays.asList(
                                                                       new EmailAddressRule[] {new EmailAddressRule("*")}));
        }
        if(settings.getAddressRemaps() == null) {
            settings.setAddressRemaps(new ArrayList());
        }

        //Update address mapping
        m_addressAliases = new GlobEmailAddressMapper(
                                                      fromEmailAddressRuleListPair(settings.getAddressRemaps()));

        //Update the quarantine-for stuff
        m_quarantineForList = new GlobEmailAddressList(
                                                       fromEmailAddressRule(settings.getAllowedAddressPatterns()));

        if (null != m_cronJob) {
            int h = m_settings.getDigestHourOfDay();
            int m = m_settings.getDigestMinuteOfDay();
            Period p = new Period(h, m, true);
            m_cronJob.reschedule(p);
        }

        m_digestGenerator.setMaxMailInternInDays(settings.getMaxMailIntern() / QuarantineSettings.DAY);
        m_digestGenerator.setMaxIdleInboxInDays(settings.getMaxIdleInbox() / QuarantineSettings.DAY);
    }

    private boolean m_opened = false;
    /**
     * Call that the Quarantine should "open"
     */
    public void open() {
        if(!m_opened) {
            synchronized(this) {
                if(!m_opened) {
                    m_opened = true;
                    Period p;
                    if (null == m_settings) {
                        p = new Period(6, 0, true);
                    } else {
                        int h = m_settings.getDigestHourOfDay();
                        int m = m_settings.getDigestMinuteOfDay();
                        p = new Period(h, m, true);
                    }

                    Runnable r = new Runnable()
                        {
                            public void run()
                            {
                                cronCallback();
                            }
                        };
                    m_cronJob = LocalUvmContextFactory.context().makeCronJob(p, r);
                }
            }
        }
    }

    /**
     * Tell the quarantine that it is closing.  Stray calls
     * may still be made (thread timing), but will likely be
     * slower.
     */
    public void close() {
        m_store.close();
        if (null != m_cronJob) {
            m_cronJob.cancel();
        }
    }

    /**
     * Callback from the Chron thread that we should send
     * digests and purge the store.
     */
    void cronCallback() {
        //TODO bscott  There should be a way to combine
        //pruning with collection of inboxes
        m_logger.debug("Cron callback for sending digests/pruning things");
        pruneStoreNow();
        sendDigestsNow();
    }

    public void pruneStoreNow() {
        m_store.prune(m_settings.getMaxMailIntern(),
                      m_settings.getMaxIdleInbox(),
                      QuarantinePruningObserver.NOOP);
    }

    /**
     * Warning - this method executes synchronously
     */
    public void sendDigestsNow() {
        List<Inbox> allInboxes = m_store.listInboxes();
        long cutoff = System.currentTimeMillis() - ONE_DAY;

        for(Inbox inbox : allInboxes) {
            Pair<QuarantineStore.GenericStatus, InboxIndexImpl> result =
                m_store.getIndex(inbox.getAddress());

            if(result.a == QuarantineStore.GenericStatus.SUCCESS) {
                if(result.b.size() > 0) {
                    if(result.b.getNewestMailTimestamp() < cutoff) {
                        m_logger.debug("No need to send digest to \"" +
                                       inbox.getAddress() + "\", no new mails in last 24 hours (" +
                                       result.b.getNewestMailTimestamp() + " millis)");
                    } else {
                        if(sendDigestEmail(inbox.getAddress(), result.b)) {
                            m_logger.debug("Sent digest to \"" + inbox.getAddress() + "\"");
                        } else {
                            m_logger.warn("Unable to send digest to \"" + inbox.getAddress() + "\"");
                        }
                    }
                } else {
                    m_logger.debug("No need to send digest to \"" + inbox.getAddress() + "\", no mails");
                }
            }
        }
    }

    private String getInternalIPAsString() {
        return LocalUvmContextFactory.context().networkManager().getPublicAddress();
    }

    //--QuarantineNodeView--

    public boolean quarantineMail(File file,
                                  MailSummary summary,
                                  EmailAddress...recipients) {

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
        if(getInternalIPAsString() == null) {
            //TODO bscott It would be nice to not repeat
            //     this warning msg
            m_logger.warn("No inside interface, so no way for folks to release inbox.  Abort quarantining");
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
            String[] recipientsForThisInbox = (String[])
                entry.getValue().toArray(new String[entry.getValue().size()]);

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

        Pair<QuarantineStore.GenericStatus, InboxIndexImpl> result =
            m_store.purge(account, doomedMails);

        checkAndThrowCommonErrors(result.a, account);

        return result.b;
    }

    public InboxIndex rescue(String account,
                             String...rescuedMails)
        throws NoSuchInboxException, QuarantineUserActionFailedException {

        Pair<QuarantineStore.GenericStatus, InboxIndexImpl> result =
            m_store.rescue(account, m_rescueHandler, rescuedMails);

        checkAndThrowCommonErrors(result.a, account);

        return result.b;
    }

    public void rescueInbox(String account)
        throws NoSuchInboxException, QuarantineUserActionFailedException {

        InboxIndex index = getInboxIndex(account);

        String[] ids = new String[index.size()];
        int ptr = 0;
        for(InboxRecord record : index) {
            ids[ptr++] = record.getMailID();
        }
        rescue(account, ids);
    }

    public InboxIndex getInboxIndex(String account)
        throws NoSuchInboxException, QuarantineUserActionFailedException {

        Pair<QuarantineStore.GenericStatus, InboxIndexImpl> result = m_store.getIndex(account);

        checkAndThrowCommonErrors(result.a, account);

        return result.b;
    }

    public void test() {
        //Do nothing.
    }


    //--QuarantineMaintenenceView --

    public long getInboxesTotalSize()
        throws QuarantineUserActionFailedException {
        return m_store.getTotalSize();
    }

    public String getFormattedInboxesTotalSize(boolean inMB) {
        return m_store.getFormattedTotalSize(inMB);
    }

    public List<Inbox> listInboxes()
        throws QuarantineUserActionFailedException {
        return m_store.listInboxes();
    }

    public void deleteInbox(String account)
        throws NoSuchInboxException, QuarantineUserActionFailedException {
        switch(m_store.deleteInbox(account)) {
        case NO_SUCH_INBOX:
            //Just supress this one for now
        case SUCCESS:
            break;//
        case ERROR:
            throw new QuarantineUserActionFailedException("Unable to delete inbox");
        }
    }


    //--QuarantineUserView--

    public String getAccountFromToken(String token)
        throws /*NoSuchInboxException, */BadTokenException {

        Pair<AuthTokenManager.DecryptOutcome, String> p =
            m_atm.decryptAuthToken(token);

        if(p.a != AuthTokenManager.DecryptOutcome.OK) {
            throw new BadTokenException(token);
        }

        return p.b;
    }

    public boolean requestDigestEmail(String account)
        throws NoSuchInboxException, QuarantineUserActionFailedException {

        boolean ret = sendDigestEmail(account, getInboxIndex(account));

        if(!ret) {
            m_logger.warn("Unable to send digest email to account \"" +
                          account + "\"");
        }

        return true;
    }

    public void remapSelfService(String from, String to)
        throws QuarantineUserActionFailedException, InboxAlreadyRemappedException {

        String existingMapping = m_addressAliases.getAddressMapping(from);
        if(existingMapping != null) {
            throw new InboxAlreadyRemappedException(from, existingMapping);
        }

        //Create a new List
        List<Pair<String, String>> mappings = m_addressAliases.getRawMappings();
        mappings.add(0, new Pair<String, String>(from, to));

        //Convert list to form which makes settings happy
        List newMappingsList = toEmailAddressPairRuleList(mappings);

        MailNodeSettings settings = m_impl.getMailNodeSettings();
        settings.getQuarantineSettings().setAddressRemaps(newMappingsList);

        m_impl.setMailNodeSettings(settings);
    }

    public boolean unmapSelfService(String inboxName, String aliasToRemove)
        throws QuarantineUserActionFailedException {

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
        List newMappingsList = toEmailAddressPairRuleList(mappings);

        MailNodeSettings settings = m_impl.getMailNodeSettings();
        settings.getQuarantineSettings().setAddressRemaps(newMappingsList);

        m_impl.setMailNodeSettings(settings);
        //    m_addressAliases = newMapper; // use updated mapping

        //    System.out.println("***DEBUG*** Returning True");
        return true;
    }

    public String getMappedTo(String account)
        throws QuarantineUserActionFailedException {

        return m_addressAliases.getAddressMapping(account);

    }

    public String[] getMappedFrom(String account)
        throws QuarantineUserActionFailedException {

        return m_addressAliases.getReverseMapping(account);
    }

    /**
     * Helper method which sends a digest email.  Returns
     * false if there was an error in sending of template
     * merging
     */
    private boolean sendDigestEmail(String account,
                                    InboxIndex index) {

        String internalHost = getInternalIPAsString();
        if(internalHost == null) {
            m_logger.warn("Unable to determine internal interface");
            return false;
        }

        String fromAddr = LocalUvmContextFactory.context().mailSender().getMailSettings().getFromAddress();
        MIMEMessage msg = m_digestGenerator.generateMsg(index,
                                                        internalHost,
                                                        account,
                                                        fromAddr,
                                                        m_atm);

        if(msg == null) {
            m_logger.debug("Unable to generate digest message for \"" + account + "\"");
            return false;
        }

        //Convert message to a Stream
        InputStream in = null;
        try {
            ByteBuffer buf = msg.toByteBuffer();
            in = new ByteBufferInputStream(buf);
        } catch(Exception ex) {
            m_logger.error("Exception converting MIMEMessage to a byte[]", ex);
            IOUtil.close(in);
            return false;
        }

        //Attempt the send
        boolean ret = LocalUvmContextFactory.context().mailSender().sendMessage(in, account);

        IOUtil.close(in);

        return ret;
    }

    private void checkAndThrowCommonErrors(QuarantineStore.GenericStatus status,
                                           String account)
        throws NoSuchInboxException, QuarantineUserActionFailedException {

        if(status == QuarantineStore.GenericStatus.NO_SUCH_INBOX) {
            throw new NoSuchInboxException(account);
        } else if(status == QuarantineStore.GenericStatus.ERROR) {
            throw new QuarantineUserActionFailedException();
        }
    }

    private List toEmailAddressRule(List<String> typedList) {
        ArrayList ret = new ArrayList();

        for(String s : typedList) {
            ret.add(new EmailAddressRule(s));
        }

        return ret;
    }

    private List<String> fromEmailAddressRule(List list) {
        ArrayList<String> ret =
            new ArrayList<String>();

        for(Object o : list) {
            EmailAddressRule wrapper = (EmailAddressRule) o;
            ret.add(wrapper.getAddress());
        }
        return ret;
    }

    private List toEmailAddressPairRuleList(List<Pair<String, String>> typedList) {
        ArrayList ret = new ArrayList();

        for(Pair<String, String> pair : typedList) {
            ret.add(new EmailAddressPairRule(pair.a, pair.b));
        }
        return ret;
    }

    private List<Pair<String, String>> fromEmailAddressRuleListPair(List list) {
        ArrayList<Pair<String, String>> ret =
            new ArrayList<Pair<String, String>>();

        for(Object o : list) {
            EmailAddressPairRule eaPair = (EmailAddressPairRule) o;
            ret.add(new Pair<String, String>(eaPair.getAddress1(), eaPair.getAddress2()));
        }
        return ret;
    }

    //------------- Inner Class --------------------

    private class RescueEjectionHandler
        implements QuarantineEjectionHandler {

        public void ejectMail(InboxRecord record,
                              String inboxAddress,
                              String[] recipients,
                              File data) {

            FileInputStream fIn = null;
            try {
                fIn = new FileInputStream(data);
                BufferedInputStream bufIn = new BufferedInputStream(fIn);
                boolean success = LocalUvmContextFactory.context().mailSender().sendMessage(bufIn, recipients);
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
}

/*
  new Thread(new Runnable() {
  public void run() {
  try {
  System.out.println("********* Sleep for 2 minutes");
  Thread.currentThread().sleep(1000*60*2);
  System.out.println("********* Woke up");
  com.untangle.node.mail.papi.EmailAddressRule wrapper1 =
  new com.untangle.node.mail.papi.EmailAddressRule("*1@shoop.com");
  com.untangle.node.mail.papi.EmailAddressRule wrapper2 =
  new com.untangle.node.mail.papi.EmailAddressRule("billtest3@shoop.com");
  MailNodeSettings settings = getMailNodeSettings();
  com.untangle.node.mail.papi.quarantine.QuarantineSettings qs =
  settings.getQuarantineSettings();
  java.util.ArrayList list = new java.util.ArrayList();
  list.add(wrapper1);
  list.add(wrapper2);

  qs.setAllowedAddressPatterns(list);
  setMailNodeSettings(settings);


  //              MailNodeSettings settings = getMailNodeSettings();
  //              com.untangle.node.mail.papi.quarantine.QuarantineSettings qs =
  //                settings.getQuarantineSettings();
  //              com.untangle.node.mail.papi.EmailAddressPair p1 =
  //                new com.untangle.node.mail.papi.EmailAddressPair("billtest1@shoop.com",
  //                  "billtest2@shoop.com");
  //              com.untangle.node.mail.papi.EmailAddressPair p2 =
  //                new com.untangle.node.mail.papi.EmailAddressPair("billtest3@shoop.com",
  //                  "foo@billsco.com");
  //              java.util.ArrayList list = new java.util.ArrayList();
  //              list.add(p1);
  //              list.add(p2);

  //              qs.setAddressRemaps(list);
  //              setMailNodeSettings(settings);
  System.out.println("********* Done.");

  }
  catch(Exception ex) {
  ex.printStackTrace();
  }
  }
  }).start();

*/
