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
import static com.metavize.tran.mime.HeaderNames.*;


/**
 * Wrapper class which lets us attach a MIMEMessage
 * as an RFC822 Message.  Takes care of the correct
 * Content-XXXX headers to be an attached email.
 */
public class AttachedMIMEMessage
  extends MIMEPart {

  private final Logger m_logger = Logger.getLogger(AttachedMIMEMessage.class);  

  private MIMEMessage m_attach;

  public AttachedMIMEMessage(MIMEMessage attach) {
    super();
    m_attach = attach;

    try {
      getMPHeaders().addHeaderField(CONTENT_TYPE, ContentTypeHeaderField.MESSAGE_RFC822);
      getMPHeaders().addHeaderField(CONTENT_TRANSFER_ENCODING, ContentXFerEncodingHeaderField.SEVEN_BIT_STR);
      getMPHeaders().addHeaderField(CONTENT_DISPOSITION, ContentDispositionHeaderField.INLINE_VAL);
    }
    catch(Exception ex) {
      //SHouldn't happen!
      m_logger.error(ex);
    }
  }

  @Override
  public void dispose() {
    m_attach.dispose();
    m_attach = null;
    super.dispose();
  }

  @Override
  public File getContentAsFile(FileFactory factory,
    boolean decoded) 
    throws IOException {

    checkDisposed();
    
    File f = factory.createFile();
    FileOutputStream fOut = null;
    try {
      //TODO bscott Again, this is a waste.  Cache files better!
      fOut = new FileOutputStream(f);
      BufferedOutputStream bufOut = new BufferedOutputStream(fOut);
      MIMEOutputStream mimeOut = new MIMEOutputStream(bufOut);
      m_attach.writeTo(mimeOut);
      mimeOut.flush();
      bufOut.flush();
      fOut.flush();
      fOut.close();
      return f;
    }
    catch(IOException ex) {
      try {f.delete();}catch(Exception ignore){}
      try {fOut.close();}catch(Exception ignore){}
      IOException ex2 = new IOException();
      ex2.initCause(ex);
      throw ex2;
    }
  }

  @Override
  public final void writeTo(MIMEOutputStream out)
    throws IOException {  
    
    checkDisposed();

    getMPHeaders().writeTo(out);

    m_attach.writeTo(out);
  }

}