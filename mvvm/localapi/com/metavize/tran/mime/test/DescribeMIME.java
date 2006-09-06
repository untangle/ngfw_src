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
package com.metavize.tran.mime.test;
import com.metavize.tran.util.FileFactory;
import java.io.*;
import com.metavize.tran.mime.*;

/**
 * Little test which parses MIME then describes
 * its contents
 */
public class DescribeMIME {

  public static void main(final String[] args) throws Exception {

    File mimeFile = new File(args[0]);

    FileMIMESource source = new FileMIMESource(mimeFile, false);
    
    MIMEMessage mp = new MIMEMessage(source.getInputStream(),
      source,
      new MIMEPolicy(),
      null);

    System.out.println(mp.describe());
  }

}