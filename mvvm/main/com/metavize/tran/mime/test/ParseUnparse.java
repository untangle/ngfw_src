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
package com.metavize.tran.mime.test;
import com.metavize.tran.util.FileFactory;
import java.io.*;
import com.metavize.tran.mime.*;

/**
 * Little test which parses MIME then writes it out.  Files
 * should be the same.
 */
public class ParseUnparse {

  public static void main(final String[] args) throws Exception {

    File mimeFile = new File(args[0]);

    FileMIMESource source = new FileMIMESource(mimeFile, false);
    
    MIMEMessage mp = new MIMEMessage(source.getInputStream(),
      source,
      new MIMEPolicy(),
      null);

    final String outFileName = args[0] + ".out";
      
    System.out.println("================================");
    System.out.println(mp.describe());
    System.out.println("================================");
    mp.changed();
    File newFile = mp.toFile(new FileFactory() {
      public File createFile(String name)
        throws IOException {
        return createFile();
      }
      public File createFile()
        throws IOException {
        return new File(outFileName);
      }
    });
    System.out.println("Wrote back out to " + outFileName);

  }

}