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
import com.metavize.tran.util.*;

/**
 * TODO: bscott work in progress  This needs to be improved
 *       so it tracks open files, and (possibly) reuses
 *       the last closed stream.
 */
public class FileMIMESource 
  implements MIMESource {

  File m_file;
  
  
  /**
   *
   */
  public FileMIMESource(File file) {
    m_file = file;
  }


  //-------------------------
  // See doc on MIMESource
  //-------------------------
  public MIMEParsingInputStream getInputStream() 
    throws IOException {
    return new MIMEParsingInputStream(
      new BufferedInputStream(
        new FileInputStream(m_file)));
  }
  
  public MIMEParsingInputStream getInputStream(long offset)
    throws IOException {
    MIMEParsingInputStream ret = getInputStream();
    while(offset > 0) {
      offset-=ret.skip(offset);
    }
    return ret;
  }
  
  public void close() {
    //TODO bscott Implemement me, closing the file
    //and any open streams.
  } 
  
  public File getFile() {
    return m_file;
  } 
  
  public File toFile(FileFactory factory) throws IOException {
    return getFile();
  }
  
  public File toFile(FileFactory factory, String name) throws IOException {
    return getFile();  
  }  

}