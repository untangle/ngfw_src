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
import java.io.File;


/**
 * Wrapper around the OpenSSL application.  Note that the features
 * around using OpenSSL as a CA are somewhat stateful, so these have been
 * broken into {@link com.metavize.mvvm.util.OpenSSLCAWrapper their own class}.
 */
public class OpenSSLWrapper {

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

  /**
   * Create a Cert Sign Request for submission to a CA.
   *
   * @param cn the common name <b>Must be the FQDN if this is for a web cert</b>
   * @param org "organization" (i.e. "metavize").
   * @param country
   * @param state
   * @param city
   * @param privateKey the private key ({@link #genKey see genKey()}).
   *
   * @return the CRS bytes
   */
  public static byte[] createCSR(String cn,
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

  /**
   * Create a Cert Sign Request for submission to a CA.
   *
   * @param cn the common name <b>Must be the FQDN if this is for a web cert</b>
   * @param org "organization" (i.e. "metavize").
   * @param country
   * @param state
   * @param city
   * @param keyFile the file containing the private key
   *
   * @return the CRS bytes
   */
  public static byte[] createCSR(String cn,
    String org,
    String country,
    String state,
    String city,
    File keyFile)
    throws IOException, java.util.concurrent.TimeoutException {

    StringBuffer subject = new StringBuffer();
    if(country != null) {
      subject.append("/C=");
      subject.append(country);
    }
    if(state != null) {
      subject.append("/ST=");
      subject.append(state);
    }
    if(city != null) {
      subject.append("/L=");
      subject.append(city);
    }
    if(org != null) {
      subject.append("/O=");
      subject.append(org);
    }
    if(cn != null) {
      subject.append("/CN=");
      subject.append(cn);
    }              
    
    
    SimpleExec.SimpleExecResult result = SimpleExec.exec(
      "openssl",
      new String[] {
        "req",
        "-new",
        "-key",
        keyFile.getAbsolutePath(),
        "-subj",
        subject.toString()
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
  public static void main(String[] args) throws Exception {

    java.io.FileOutputStream fOut =
      new java.io.FileOutputStream(new File("my.pri"));

    byte[] pkBytes = genKey();

    fOut.write(pkBytes);
    fOut.flush();
    fOut.close();

    fOut = new java.io.FileOutputStream(new File("req.pem"));


    byte[] csrBytes = createCSR("gobbles.metavize.com",
      "metavize", "US", "California", "San Mateo", pkBytes);

    fOut.write(csrBytes);
    fOut.flush();
    fOut.close();  
  }
*/  

}

