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

import com.metavize.mvvm.MvvmContextFactory;
import com.metavize.tran.mail.papi.quarantine.NoSuchInboxException;
import com.metavize.tran.mail.papi.quarantine.InboxIndex;
import com.metavize.tran.mail.impl.quarantine.store.InboxIndexImpl;
import com.metavize.tran.mail.papi.quarantine.QuarantineUserActionFailedException;
import com.metavize.tran.mail.papi.quarantine.BadTokenException;
import com.metavize.tran.mail.papi.quarantine.QuarantineManipulation;
import com.metavize.tran.mail.papi.quarantine.QuarantineMaintenenceView;
import com.metavize.tran.mail.papi.quarantine.QuarantineTransformView;
import com.metavize.tran.mail.papi.quarantine.QuarantineUserView;
import com.metavize.tran.mail.papi.quarantine.QuarantineSettings;
import com.metavize.tran.mail.papi.quarantine.InboxRecord;
import com.metavize.tran.mail.papi.quarantine.QuarantineEjectionHandler;
import com.metavize.tran.mail.papi.quarantine.MailSummary;
import com.metavize.tran.mail.impl.quarantine.store.QuarantineStore;
import com.metavize.tran.mime.EmailAddress;
import com.metavize.tran.mime.MIMEMessage;
import com.metavize.tran.util.Pair;
import com.metavize.tran.util.IOUtil;
import com.metavize.tran.util.ByteBufferInputStream;
import java.util.List;
import java.util.ArrayList;
import java.io.File;
import java.io.FileInputStream;
import java.io.BufferedInputStream;
import java.io.InputStream;
import java.nio.ByteBuffer;
import org.apache.log4j.Logger;

//May be removed later
import sun.misc.BASE64Decoder;
import sun.misc.BASE64Encoder;

/**
 *
 */
public class Quarantine
  implements QuarantineTransformView,
    QuarantineMaintenenceView, QuarantineUserView {

  //TODO This has to be made read
  private final String HACK_HOST = "10.0.0.141";
    
  private final Logger m_logger =
    Logger.getLogger(Quarantine.class);    
  private QuarantineStore m_store;
  private RescueEjectionHandler m_rescueHandler =
    new RescueEjectionHandler();
  private DigestGenerator m_digestGenerator;


  public Quarantine() {
    m_store = new QuarantineStore(
      new File(new File(System.getProperty("bunnicula.home")), "quarantine")
      );
    m_digestGenerator = new DigestGenerator();
  }
  

  /**
   * Properties are not maintained explicitly
   * by the Quarantine (i.e. the UI does not
   * talk to the Quarantine).
   */
  public void setSettings(QuarantineSettings settings) {
    //TODO Implement me
  }

  /**
   * Tell the quarantine that it is closing.  Stray calls
   * may still be made (thread timing), but will likely be
   * slower.
   */
  public void close() {
    m_store.close();
  }
    

  //--QuarantineTransformView--

  public boolean quarantineMail(File file,
    MailSummary summary,
    EmailAddress...recipients) {

    //TODO Check size of store vs. max size

    if(recipients.length == 1) {
      return m_store.quarantineMail(file,
            recipients[0].getAddress(),
            summary,
            true).a != QuarantineStore.AdditionStatus.FAILURE;
    }
    else {
      ArrayList<Pair<String, String>> list = new ArrayList<Pair<String, String>>();
      boolean allSuccess = true;
      for(EmailAddress eAddr : recipients) {
        String addr = eAddr.getAddress();
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
  
  public List<String> listInboxes()
    throws QuarantineUserActionFailedException {
    //TODO bscott implement me
    return null;
  }

  public void deleteInbox(String account)
    throws NoSuchInboxException, QuarantineUserActionFailedException {
    //TODO bscott implement me
  }


  //--QuarantineUserView--

  public String getAccountFromToken(String token)
    throws /*NoSuchInboxException, */BadTokenException {

    String ret = decryptAuthToken(token);
    if(ret == null) {
      throw new BadTokenException(token);
    }
//    if(!m_store.inboxExists(ret)) {
//      throw new NoSuchInboxException(ret);
//    }
    
    return ret;
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


  private boolean sendDigestEmail(String account,
    InboxIndex index) {

    MIMEMessage msg = m_digestGenerator.generateMsg(index,
      HACK_HOST,
      account,
      "quarantine@" + HACK_HOST,
      this);

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

  /**
   * Until I resolve how this should be done, this method
   * is a placeholder for Token generation
   *
   * TODO bscott a real way to create tokens.
   */
  public String createAuthToken(String username) {
    return base64Encode(username);
  }
  public String decryptAuthToken(String token) {
    return new String(base64Decode(token));
  }

  private String base64Encode(String s) {
    if(s == null) {
      return null;
    }
    try {
      return new BASE64Encoder().encode(s.getBytes());
    }
    catch(Exception ex) {
      m_logger.warn("Exception base 64 encoding \"" + s + "\"", ex);
      return null;
    }
  }   
   
  private byte[] base64Decode(String s) {
    if(s == null) {
      return null;
    }
    try {
      return new BASE64Decoder().decodeBuffer(s);
    }
    catch(Exception ex) {
      m_logger.warn("Exception base 64 decoding \"" + s + "\"", ex);
      return null;
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