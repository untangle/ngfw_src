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
import java.io.*;
import java.util.*;
import org.apache.log4j.Logger;
import static com.metavize.tran.util.Ascii.*;
import com.metavize.tran.util.*;


/**
 * Class representing a MIMEMessage.  Adds the strongly-typed
 * {@link #getMMHeaders MIMEMessageHeaders} with convienence 
 * members for a top-level message (such as recipient
 * and subject manipulation).
 * <br>
 * <b>Work in progress</b>
 */
public class MIMEMessage 
  extends MIMEPart {
  
  private final Logger m_logger = Logger.getLogger(MIMEPart.class); 
  
  protected MIMEMessage() {
    super();
  }  
  
  /**
   * Construct a MIME part, reading until the outerBoundary.
   */
  public MIMEMessage(MIMEParsingInputStream stream,
    MIMESource source,
    MIMEPolicy policy,
    String outerBoundary) throws IOException,
      InvalidHeaderDataException, 
      HeaderParseException,
      MIMEPartParseException {  
    
    super();  
      
    parse(new MailMessageHeaderFieldFactory(),
      stream,
      source,
      policy,
      outerBoundary);
  }

  /**
   * Construct a MIMEMessage using the already-parsed headers.
   */
  public MIMEMessage(MIMEParsingInputStream stream,
    MIMESource source,
    MIMEPolicy policy,
    String outerBoundary,
    MIMEMessageHeaders headers) throws IOException,
      InvalidHeaderDataException, 
      HeaderParseException,
      MIMEPartParseException {    
    super(stream, source, policy, outerBoundary, headers);
  }
  
  /**
   * Get the MIMEMessageHeaders for this MIMEMessage.  Changes
   * to the headers will be known by this message.  
   *
   * @return the headers
   */
  public MIMEMessageHeaders getMMHeaders() {
    return (MIMEMessageHeaders) getMPHeaders();
  }  
  
//------------- Debug/Test ---------------  

  public static void main(String[] args) throws Exception {

    File f = new File(args[0]);
    
    File tempDir = new File(new File(System.getProperty("user.dir")),
      "mimeFiles");
    if(!tempDir.exists()) {
      tempDir.mkdirs();
    }    
    
    //Dump file to another file, with byte offsets.  This
    //makes troubleshooting really easy
    FileInputStream fIn = new FileInputStream(f);
    FileOutputStream fOut = 
      new FileOutputStream(new File("byteMap.txt"));
    int rawRead = fIn.read();
    int counter = 0;
    while(rawRead != -1) {
      fOut.write((counter + ": ").getBytes());
      if(rawRead < 33 || rawRead > 126) {
        fOut.write(("(unprintable)" + rawRead).getBytes());
      }
      else {
        fOut.write((byte) rawRead);
      }
      fOut.write(System.getProperty("line.separator").getBytes());
      rawRead = fIn.read();
      counter++;
    }
    fIn.close();
    fOut.flush();
    fOut.close();
    
    FileMIMESource source = new FileMIMESource(f);
    
    MIMEMessage mp = new MIMEMessage(source.getInputStream(),
      source,
      new MIMEPolicy(),
      null);
    
    System.out.println("");
    System.out.println("Message has subject: \"" + 
      mp.getMMHeaders().getSubject() + "\"");
    System.out.println("BEGIN Recipients");
    List<EmailAddressWithRcptType> allRcpts = mp.getMMHeaders().getAllRecipients();
    for(EmailAddressWithRcptType rwt : allRcpts) {
      System.out.println(rwt);
    }
    System.out.println("ENDOF Recipients");
    mp.dump("");


    
    TempFileFactory factory = new TempFileFactory(tempDir);
    
    File file = null;    
    if(mp.isMultipart()) {  

      MIMEPart[] children = mp.getLeafParts(true);

      System.out.println("Now, decode the " + children.length + " leaf children");
      for(MIMEPart part : children) {
        if(!part.isMultipart()) {
          file = part.getContentAsFile(factory, false);
          System.out.println("Raw part to: " + file.getName());      
          file = part.getContentAsFile(factory, true);
          System.out.println("Decoded part to: " + file.getName());
        }
      }
      
      for(MIMEPart part : children) {
        part.changed();
        part.getObserver().mIMEPartChanged(part);
        part.getMPHeaders().addHeaderField("FooBar", "Goo");
        part.getMPHeaders().removeHeaderFields(new LCString("FooBar"));
      }
      System.out.println("Try writing it out (after declaring changed)");
      fOut = new FileOutputStream(new File(tempDir, "redone.txt"));
      mp.writeTo(new MIMEOutputStream(fOut));
      fOut.flush();
      fOut.close();
    }
    else {
      file = mp.getContentAsFile(factory, false);
      System.out.println("Raw part to: " + file.getName());      
      file = mp.getContentAsFile(factory, true);
      System.out.println("Decoded part to: " + file.getName());    
      System.out.println("Try writing it out (after declaring changed)");
        mp.changed();
//        mp.getObserver().mIMEPartChanged(part);
        mp.getMPHeaders().addHeaderField("FooBar", "Goo");
        mp.getMPHeaders().removeHeaderFields(new LCString("FooBar"));      
      fOut = new FileOutputStream(new File(tempDir, "redone.txt"));
      mp.writeTo(new MIMEOutputStream(fOut));
      fOut.flush();
      fOut.close();    
    }
      
  }

//------------- Debug/Test ---------------  
  
}