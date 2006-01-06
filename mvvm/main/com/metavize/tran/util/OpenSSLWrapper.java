/*
 * Copyright (c) 2005 Metavize Inc.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Metavize Inc. ("Confidential Information").  You shall
 * not disclose such Confidential Information.
 *
 *  $Id$
 */
 
package com.metavize.tran.util;
import java.util.concurrent.TimeoutException;
import java.io.IOException;


/**
 * Wrapper around the OpenSSL application.  Note that the features
 * around using OpenSSL as a CA are somewhat stateful, so these have been
 * broken into {@link com.metavize.mvvm.util.OpenSSLCAWrapper their own class}.
 */
public class OpenSSLWrapper {

//To generate a request from an existing key
//openssl req -new -key out.pri -out myreq.pem -subj '/CN=www.mydom.com/O=My Dom, Inc./C=US/ST=Oregon/L=Portland'



  /**
   * Generates an RSA key of length 1024.
   *
   * @return the bytes of the private key (encoded)
   */
  public static byte[] genKey()
    throws IOException, TimeoutException {
    //openssl genrsa 1024
    SimpleExec.SimpleExecResult result = SimpleExec.exec(
      "openssl",
      new String[] {
        "genrsa",
        "1024"
      },
      null,
      null,
      true,
      false,
      1000*60,
      null,
      false);//TODO More thread junk.  This is getting to be a real pain...

    if(result.exitCode==0) {
      return result.stdOut;
    }
    throw new IOException("openssl exited with value " + result.exitCode);    
  }

/*  
  public byte[] createCSR(String cn,
    String org,
    String country,
    String state,
    String city,
    byte[] privateKey)
    throws IOException, java.util.concurrent.TimeoutException {
    
    File temp = File.createTempFile("csr", ".tmp");
    try {
      IOUtil.bytesToFile(privateKey, temp);
      byte[] ret = createCSR(cn, org, country, state, city, temp);
      IOUtil.delete(temp);
      return ret;
    }
    catch(IOException ex) {
      IOUtil.delete(temp);
      throw ex;
    }
    catch(java.util.concurrent.TimeoutException ex) {
      IOUtil.delete(temp);
      throw ex;
    }    
  }  
*/
  

  public static void main(String[] args) throws Exception {
    System.out.write(genKey());
    System.out.flush();
  }

}

