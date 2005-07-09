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
package com.metavize.tran.util;
import java.io.File;
import java.io.IOException;


/**
 * Implementation of FileFactory which creates temp files.
 */
public class TempFileFactory
  implements FileFactory {

  private File m_dir;
  
  public TempFileFactory() {
    this(null);
  }
  public TempFileFactory(File rootDir) {
    m_dir = rootDir;
  }
  
  public File createFile(String name) 
    throws IOException {
    if(name == null) {
      name = "meta";
    }
    //Javasoft requires 3 characters in prefix !?!
    while(name.length() < 3) {
      name = name+"X";
    }
    return File.createTempFile(name, null, m_dir);
  }
  
  /**
   * Create an anonymous file.
   */
  public File createFile() 
    throws IOException {
    return createFile(null);
  }

}