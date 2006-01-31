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

package com.metavize.tran.mail.impl.quarantine;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.net.InetAddress;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

import com.metavize.mvvm.CronJob;
import com.metavize.mvvm.MvvmContextFactory;
import com.metavize.mvvm.Period;
import com.metavize.tran.mail.impl.quarantine.store.InboxIndexImpl;
import com.metavize.tran.mail.impl.quarantine.store.QuarantinePruningObserver;
import com.metavize.tran.mail.impl.quarantine.store.QuarantineStore;
import com.metavize.tran.mail.impl.GlobEmailAddressList;
import com.metavize.tran.mail.impl.GlobEmailAddressMapper;
import com.metavize.tran.mail.papi.quarantine.BadTokenException;
import com.metavize.tran.mail.papi.quarantine.Inbox;
import com.metavize.tran.mail.papi.quarantine.InboxIndex;
import com.metavize.tran.mail.papi.quarantine.InboxRecord;
import com.metavize.tran.mail.papi.quarantine.MailSummary;
import com.metavize.tran.mail.papi.quarantine.NoSuchInboxException;
import com.metavize.tran.mail.papi.quarantine.QuarantineEjectionHandler;
import com.metavize.tran.mail.papi.quarantine.QuarantineMaintenenceView;
import com.metavize.tran.mail.papi.quarantine.QuarantineManipulation;
import com.metavize.tran.mail.papi.quarantine.QuarantineSettings;
import com.metavize.tran.mail.papi.quarantine.QuarantineTransformView;
import com.metavize.tran.mail.papi.quarantine.QuarantineUserActionFailedException;
import com.metavize.tran.mail.papi.quarantine.QuarantineUserView;
import com.metavize.tran.mail.papi.EmailAddressPair;
import com.metavize.tran.mail.papi.EmailAddressWrapper;
import com.metavize.tran.mime.EmailAddress;
import com.metavize.tran.mime.MIMEMessage;
import com.metavize.tran.util.ByteBufferInputStream;
import com.metavize.tran.util.IOUtil;
import com.metavize.tran.util.Pair;
import org.apache.log4j.Logger;



/**
 *
 */
public class Quarantine
  implements QuarantineTransformView,
    QuarantineMaintenenceView, QuarantineUserView {

  private static final long ONE_DAY =
    1000*60*60*24;

  private final Logger m_logger =
    Logger.getLogger(Quarantine.class);
  private QuarantineStore m_store;
  private RescueEjectionHandler m_rescueHandler =
    new RescueEjectionHandler();
  private DigestGenerator m_digestGenerator;
  private AuthTokenManager m_atm;
  private QuarantineSettings m_settings = new QuarantineSettings();
  private CronJob m_cronJob;
  private GlobEmailAddressList m_quarantineForList;
  private GlobEmailAddressMapper m_addressAliases;


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
  public void setSettings(QuarantineSettings settings) {
    m_settings = settings;

    m_atm.setKey(m_settings.getSecretKey());

    //Handle nulls (defaults)
    if(settings.getAllowedAddressPatterns() == null ||
      settings.getAllowedAddressPatterns().size() == 0) {
      settings.setAllowedAddressPatterns(
        java.util.Arrays.asList(
          new EmailAddressWrapper[] {new EmailAddressWrapper("*")}));
    }
    if(settings.getAddressRemaps() == null) {
      settings.setAddressRemaps(new ArrayList());
    }

    //Update address mapping
    ArrayList<Pair<String, String>> addressAliasPairList =
      new ArrayList<Pair<String, String>>();

    for(Object o : settings.getAddressRemaps()) {
      EmailAddressPair pair = (EmailAddressPair) o;
      addressAliasPairList.add(new
        Pair<String, String>(pair.getAddress1(), pair.getAddress2()));
    }
    m_addressAliases = new GlobEmailAddressMapper(addressAliasPairList);

    //Update the quarantine-for stuff
    ArrayList<String> quarantineForList =
      new ArrayList<String>();

    for(Object o : settings.getAllowedAddressPatterns()) {
      EmailAddressWrapper wrapper = (EmailAddressWrapper) o;
      quarantineForList.add(wrapper.getAddress()); 
    }
    m_quarantineForList = new GlobEmailAddressList(quarantineForList);

    
          

    if (null != m_cronJob) {
        int h = m_settings.getDigestHourOfDay();
        int m = m_settings.getDigestMinuteOfDay();
        Period p = new Period(h, m, true);
        m_cronJob.reschedule(p);
    }
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
          m_cronJob = MvvmContextFactory.context().makeCronJob(p, r);
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
          }
          else {
            if(sendDigestEmail(inbox.getAddress(), result.b)) {
              m_logger.debug("Sent digest to \"" + inbox.getAddress() + "\"");
            }
            else {
              m_logger.warn("Unable to send digest to \"" + inbox.getAddress() + "\"");
            }
          }
        }
        else {
          m_logger.debug("No need to send digest to \"" + inbox.getAddress() + "\", no mails");
        }
      }
    }
  }

  private String getInternalIPAsString() {
    InetAddress addr = MvvmContextFactory.context().argonManager().getInsideAddress();
    if(addr == null) {
      return null;
    }
    return addr.getHostAddress();
  }


  //--QuarantineTransformView--

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
      m_logger.warn("No inside interface, so no way for folks to release inbox.  Abort " +
        "quarantining");
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
    }
    else {
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

    MIMEMessage msg = m_digestGenerator.generateMsg(index,
      internalHost,
      account,
      m_settings.getDigestFrom(),
      m_atm);

    if(msg == null) {
      m_logger.debug("Unable to generate digest message " +
        "for \"" + account + "\"");
      return false;
    }

    //Convert message to a Stream
    InputStream in = null;
    try {
      ByteBuffer buf = msg.toByteBuffer();
      in = new ByteBufferInputStream(buf);
    }
    catch(Exception ex) {
      m_logger.error("Exception converting MIMEMessage to a byte[]", ex);
      IOUtil.close(in);
      return false;
    }

    //Attempt the send
    boolean ret = MvvmContextFactory.context().mailSender().sendMessage(in, account);

    IOUtil.close(in);

    return ret;

  }


  private void checkAndThrowCommonErrors(QuarantineStore.GenericStatus status,
    String account)
      throws NoSuchInboxException, QuarantineUserActionFailedException {

    if(status == QuarantineStore.GenericStatus.NO_SUCH_INBOX) {
      throw new NoSuchInboxException(account);
    }
    else if(status == QuarantineStore.GenericStatus.ERROR) {
      throw new QuarantineUserActionFailedException();
    }
  }




  //------------- Inner Class --------------------

  private class RescueEjectionHandler
    implements QuarantineEjectionHandler {

    public void ejectMail(InboxRecord record,
      String recipient,
      File data) {

      FileInputStream fIn = null;
      try {
        fIn = new FileInputStream(data);
        BufferedInputStream bufIn = new BufferedInputStream(fIn);
        boolean success = MvvmContextFactory.context().mailSender().sendMessage(bufIn, recipient);
        if(success) {
          m_logger.debug("Released mail \"" + record.getMailID() + "\" for \"" +
            recipient + "\"");
        }
        else {
          m_logger.warn("Unable to release mail \"" + record.getMailID() + "\" for \"" +
            recipient + "\"");
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
              com.metavize.tran.mail.papi.EmailAddressWrapper wrapper1 =
                new com.metavize.tran.mail.papi.EmailAddressWrapper("*1@shoop.com");
              com.metavize.tran.mail.papi.EmailAddressWrapper wrapper2 =
                new com.metavize.tran.mail.papi.EmailAddressWrapper("billtest3@shoop.com");
              MailTransformSettings settings = getMailTransformSettings();
              com.metavize.tran.mail.papi.quarantine.QuarantineSettings qs =
                settings.getQuarantineSettings();
              java.util.ArrayList list = new java.util.ArrayList();
              list.add(wrapper1);
              list.add(wrapper2);

              qs.setAllowedAddressPatterns(list);
              setMailTransformSettings(settings);                  
                

//              MailTransformSettings settings = getMailTransformSettings();
//              com.metavize.tran.mail.papi.quarantine.QuarantineSettings qs =
//                settings.getQuarantineSettings();
//              com.metavize.tran.mail.papi.EmailAddressPair p1 =
//                new com.metavize.tran.mail.papi.EmailAddressPair("billtest1@shoop.com",
//                  "billtest2@shoop.com");
//              com.metavize.tran.mail.papi.EmailAddressPair p2 =
//                new com.metavize.tran.mail.papi.EmailAddressPair("billtest3@shoop.com",
//                  "foo@billsco.com");
//              java.util.ArrayList list = new java.util.ArrayList();
//              list.add(p1);
//              list.add(p2);

//              qs.setAddressRemaps(list);
//              setMailTransformSettings(settings);
              System.out.println("********* Done.");
                
              
            }
            catch(Exception ex) {
              ex.printStackTrace();
            }
          }
        }).start();        
        
*/