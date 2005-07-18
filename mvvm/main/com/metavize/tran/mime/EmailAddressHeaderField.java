 /*
  * Copyright (c) 2005 Metavize Inc.
  * All rights reserved.
  *
  * This software is the confidential and proprietary information of
  * Metavize Inc. ("Confidential Information").  You shall
  * not disclose such Confidential Information.
  *
  * $Id:$
  */
package com.metavize.tran.mime;

import javax.mail.internet.*;
import javax.mail.*;
import java.util.*;
import java.io.*;
import static com.metavize.tran.util.Ascii.*;

//===========================================
// Implementation Note.  We're currently
// leaning on the JavaMail API for the 
// heavy lifting (parsing).  We've created
// this wrapper in case we need to move alway
// from JavaMail in the future.
// -wrs 6/05
//===========================================


/**
 * HeaderField containing EmailAddresses.  This is useful for
 * things like "cc" and "To".
 */
public class EmailAddressHeaderField 
  extends HeaderField {

  private List<EmailAddress> m_addresses;
  
  public EmailAddressHeaderField(String name,
    LCString lCName) {
    super(name, lCName);
  }
  public EmailAddressHeaderField(String name) {
    super(name);
  }  
/*  
  public EmailAddressHeaderField(String name,
    LCString lCName,
    Line[] sourceLines,
    int valueStartOffset) {
    
    super(name, lCName, sourceLines, valueStartOffset);
  } 
*/
  /**
   * Get the Address at the given index.
   * 
   */
  public EmailAddress getAddress(int index)
    throws IndexOutOfBoundsException {
    if(m_addresses == null ||
      m_addresses.size() <= index) {
      throw new IndexOutOfBoundsException("" + index);
    }
    return m_addresses.get(index);
  }

  /**
   * Returns the number of addresses repesented
   * by this header
   */
  public int size() {
    return m_addresses == null?
      0:
      m_addresses.size();
  }
  
  /**
   * Iterate over the addressed contained within
   *
   * @return a typed Iterator
   */
  public Iterator<EmailAddress> iterator() {
    ensureList();
    return m_addresses.iterator();
  } 
  
  /**
   * Remove all occurances of EmailAddresses which
   * test equals (true) for the argument.  The argument
   * address itself (instance) need not be contained
   * in this Header
   * 
   * @param address the address
   * @return true if one or more were present, and removed.
   */
  public boolean remove(EmailAddress address) {
    ensureList();
    boolean ret = false;
    for(int i = 0; i<m_addresses.size(); i++) {
      if(m_addresses.get(i).equals(address)) {
        m_addresses.remove(i);
        ret = true;
      }
    }
    if(ret) {
      changed();
    }
    return ret;
  }
  
  /**
   * Removes all EmailAddresses from this header field.
   */
  public void removeAll() {
    if(m_addresses != null && m_addresses.size() > 0) {
      m_addresses.clear();
      changed();
    }
  }
  
  /**
   * Test if this Header contains any EmailAddresses which
   * Match the argument.
   */
  public boolean contains(EmailAddress address) {
    ensureList();
    for(int i = 0; i<m_addresses.size(); i++) {
      if(m_addresses.get(i).equals(address)) {
        return true;
      }
    }
    return false;  
  }
  
  /**
   * Duplicates are not prevented.
   */
  public void add(EmailAddress address) {
    ensureList();
    m_addresses.add(address);
    changed();
  }
  
  
  /**
   * Makes sure List<EmailAddresses> is never null.
   */
  private void ensureList() {
    if(m_addresses == null) {
      m_addresses = new ArrayList<EmailAddress>();
    }
  }
  
  @Override  
  protected void parseStringValue() 
    throws HeaderParseException {
    
    m_addresses = EmailAddressHeaderField.parseHeaderLine(getValueAsString());
    
  }  
  
  @Override
  public void parseLines() 
    throws HeaderParseException {
    
    parseStringValue();
  }
  
  /**
   * Really only for debugging, not to produce output suitable
   * for output.
   */  
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append(getName());
    sb.append(":");
    Iterator<EmailAddress> it = iterator();
    boolean first = true;
    while(it.hasNext()) {
      if(first) {
        first = false;
      }
      else {
        sb.append(",");
      }
      sb.append(it.next().toMIMEString());
    }
    return sb.toString();
  }   
  
  @Override
  public void writeToAssemble(MIMEOutputStream out)
    throws IOException {
    
    out.write(getName());
    out.write((byte) COLON);
    
    int written = getName().length()+1;    
    
    boolean first = true;
    for(EmailAddress address : m_addresses) {
      if(address.isNullAddress()) {
        continue;
      }
      String addrStr = address.toMIMEString();
      if(first) {
        out.write(addrStr);
        written+=addrStr.length();
        first = false;
      }
      else {
        out.write((byte) COMMA);
        out.write((byte) SP);
        if(written + addrStr.length() > 76) {
          out.writeLine();
          out.write((byte) HT);
          written = 0;
        }
        out.write(addrStr);
        written+=addrStr.length();
      }
    }
    out.writeLine();
  }

  
  /**
   * Parse a raw header line into a collection of EmailAddresses,
   * as per RFC821 and its successors.
   *
   * @param line a line containing addresses
   * @return a Listof EmailAddresses.
   */
  public static List<EmailAddress> parseHeaderLine(String line) 
    throws HeaderParseException {
    
    //For now, we'll be lazy and use JavaMail.  If they ever break, 
    //we'll have to do this on our own - wrs.
    try {
      InternetAddress[] addresses = InternetAddress.parseHeader(line, false);
//      System.out.println("[EmailAddressHeaderField] Parsed \"" + 
//        line + "\" into: " + addresses.length + " addresses");
      List<EmailAddress> ret = new ArrayList<EmailAddress>();
      if(addresses != null) {
        for(int i = 0; i<addresses.length; i++) {
          ret.add(EmailAddress.fromJavaMail(addresses[i]));
        }
      }
      return ret;
    }
    catch(AddressException ex) {
      throw new HeaderParseException("Unable to parse \"" + 
        line + "\" into addresses", ex);
    }
    
  }
  

  
}  