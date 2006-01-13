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
import java.io.IOException;
import java.io.FileNotFoundException;
import java.io.File;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.List;
import java.text.SimpleDateFormat;
import javax.naming.ldap.LdapName;
import javax.naming.ldap.Rdn;


/**
 * Wrapper around the OpenSSL application.  Note that the features
 * around using OpenSSL as a CA are somewhat stateful, so these have been
 * broken into {@link com.metavize.mvvm.util.OpenSSLCAWrapper their own class}.
 * <br>
 * Note also that some of this could be obtained from Java's APIs, but those
 * are (a) way too complicated and (b) I don't trust Java as much as OpenSSL.
 */
public class OpenSSLWrapper {

  private static final String CERT_DATE_FORMAT =
    "MMM d HH:mm:ss yyy z";

  /** 
   * Little class to hold interesting parsed information
   * from a cert.  Java has native classes for doing this,
   * but if you want to start using the "X500" and "ldap"
   * classes you're a champ.  This is much easier.
   *
   * The subjectDN and issuerDN members are hashmaps,
   * where the key is the RDN type and the value the
   * value.  For example, the subject String
   * <code>CN=gobbles.metavize.com,L=San Mateo,ST=CA,C=US</code>
   * when parsed can have its common name accessed via
   * <code>myCertInfo.subjectDN.get("CN")</code>
   */
  public static class CertInfo {
    /**
     * Date when cert becomes valid
     */
    public final Date notBefore;
    /**
     * Date when cert becomes invalid
     */
    public final Date notAfter;
    /**
     * The Distinguished name broken into a Map.  Note that
     * the most insteresting key is "CN" which will be
     * the DNS entry for a web server
     */
    public final Map<String, String> subjectDN;
    /**
     * The Distinguished name broken into a Map.
     */
    public final Map<String, String> issuerDN;
    /**
     * True if the cert is issued as a CA cert.
     */ 
    public final boolean isCA;

    private CertInfo(Date notBefore,
      Date notAfter,
      Map<String, String> subjectDN,
      Map<String, String> issuerDN,
      boolean isCA) {
      this.notBefore = notBefore;
      this.notAfter = notAfter;
      this.subjectDN = subjectDN;
      this.issuerDN = issuerDN;
      this.isCA = isCA;
    }

    /**
     * Convienence method to obtain the <b>C</b>ommon<b>N</b>ame (i.e. the hostname
     * for a web server).
     *
     * @return the CN of the cert's subject (or null if not found).
     */
    public String getSubjectCN() {
      //TODO is this stuff case sensitive?
      if(subjectDN == null) {
        return null;
      }
      String ret = subjectDN.get("CN");
      if(ret==null) {
        ret = subjectDN.get("cn");
      }
      return ret;
    }
     
    /**
     * Convience method which prints out debug info.
     */
    public String toString() {
      StringBuilder sb = new StringBuilder();
      String newLine = System.getProperty("line.separator", "\n");
      sb.append("notBefore: ").append(notBefore).append(newLine);
      sb.append("notAfter: ").append(notAfter).append(newLine);
      sb.append("isCA: ").append(isCA?"true":"false").append(newLine);
      sb.append("subjectDN").append(newLine);
      for(String key : subjectDN.keySet()) {
        sb.append("  ").append(key).append(":").append(subjectDN.get(key)).append(newLine);
      }
      sb.append("issuerDN").append(newLine);
      for(String key : issuerDN.keySet()) {
        sb.append("  ").append(key).append(":").append(issuerDN.get(key)).append(newLine);
      }
      return sb.toString();
    }
    
  }

  /**
   * "Pretty" is a relative term, but prints the human-readable
   * summary outputted by OpenSSL
   *
   * @param certBytes the bytes of the cert
   *
   * @return a String (with embedded new lines) suitable for display
   */
  public static String prettyPrint(byte[] certBytes)
    throws IOException {

    File temp = File.createTempFile("ppc", ".tmp");
    try {
      IOUtil.bytesToFile(certBytes, temp);
      String ret = prettyPrint(temp);
      IOUtil.delete(temp);
      return ret;
    }
    catch(IOException ex) {
      IOUtil.delete(temp);
      throw ex;
    }
  }
  

  /**
   * "Pretty" is a relative term, but prints the human-readable
   * summary outputted by OpenSSL
   *
   * @param certFile the cert file
   *
   * @return a String (with embedded new lines) suitable for display
   */
  public static String prettyPrint(File certFile)
    throws IOException {
    
    if(!certFile.exists()) {
      throw new FileNotFoundException(certFile.getAbsolutePath());
    }

    SimpleExec.SimpleExecResult result = SimpleExec.exec(
      "openssl",//cmd
      new String[] {//args
        "x509",
        "-in",
        certFile.getAbsolutePath(),
        "-text"
      },
      null,//env
      null,//rootDir
      true,//stdout
      true,//stderr
      1000*20);

    if(result.exitCode==0) {
      return new String(result.stdOut);
    }
    throw new IOException("Error printing cert file.  Return code: " +
      result.exitCode + ", stdout \"" +
      new String(result.stdOut) + "\", stderr \"" +
      new String(result.stdErr) + "\"");
    
  }

  /**
   * Get some info about a cert.
   * <br>
   * <b>Warning - some members of the returned object may be null</b>
   * <br>
   *
   * @param certFile the cert file
   *
   * @return the info
   */
  public static CertInfo getCertInfo(File certFile)
    throws IOException {

    //=================================================
    // Tested command as follows:
    // prompt> openssl x509 -in gobbles.pem -dates -noout -subject -issuer -nameopt RFC2253
    // prompt> notBefore=Jan  4 22:53:52 2006 GMT
    // prompt> notAfter=Jan  4 22:53:52 2007 GMT
    // prompt> subject= CN=gobbles.metavize.com,L=San Mateo,ST=CA,C=US
    // prompt> issuer= CN=gobbles.metavize.com,L=San Mateo,ST=CA,C=US
    //
    // Note that it exits "0" if there are no problems.
    //
    //
    // Then to determine if it is a CA, call "inspect_ca" which should
    // be in "/usr/bin".  It exits with "1" if it is not a CA (0 if it is).
    //=================================================
    
    SimpleExec.SimpleExecResult result = SimpleExec.exec(
      "openssl",//cmd
      new String[] {//args
        "x509",
        "-in",
        certFile.getAbsolutePath(),
        "-dates",
        "-noout",
        "-subject",
        "-issuer",
        "-nameopt",
        "RFC2253"
      },
      null,//env
      null,//rootDir
      true,//stdout
      false,//stderr
      1000*20);

    if(result.exitCode==0) {
      Date notBefore = null;
      Date notAfter = null;
      HashMap<String, String> subjectMap = null;
      HashMap<String, String> issuerMap = null;
      //Time to parse the output (I hate this in Java)
      String[] theLines = new String(result.stdOut).split("(?m)$");//The "(?m") crap is so Java's
                                                                  //Regex treats embedded new
                                                                  //lines correctly.
      for(int i = 0; i<theLines.length; i++) {
        theLines[i] = theLines[i].trim();
        if(theLines[i].startsWith("notBefore")) {
          notBefore = parseCertDate(theLines[i].substring("notBefore=".length()));
        }
        if(theLines[i].startsWith("notAfter")) {
          notAfter = parseCertDate(theLines[i].substring("notAfter=".length()));
        }
        if(theLines[i].startsWith("subject=")) {
          subjectMap = parseDN(theLines[i].substring("subject=".length()));
        }
        if(theLines[i].startsWith("issuer=")) {
          issuerMap = parseDN(theLines[i].substring("issuer=".length()));
        }        
      }

      //Now figure out if this is a CA
      result = SimpleExec.exec(
        "inspect_ca",//cmd
        new String[] {//args
          certFile.getAbsolutePath(),
        },
        null,//env
        null,//rootDir
        false,//stdout
        false,//stderr
        1000*20,
        null,//logas
        false);//TODO More thread junk.  This is getting to be a real pain...
        
      return new CertInfo(notBefore, notAfter, subjectMap, issuerMap, result.exitCode == 0);
    }
    
    throw new IOException("openssl exited with value " + result.exitCode);
  }


  




  /**
   * Get some info about a cert.
   * <br>
   * <b>Warning - some members of the returned object may be null</b>
   * <br>
   *
   * @param certBytes the cert bytes
   *
   * @return the info
   */
  public static CertInfo getCertInfo(byte[] certBytes)
    throws IOException {    

    File temp = File.createTempFile("gci", ".tmp");
    try {
      IOUtil.bytesToFile(certBytes, temp);
      CertInfo ret = getCertInfo(temp);
      IOUtil.delete(temp);
      return ret;
    }
    catch(IOException ex) {
      IOUtil.delete(temp);
      throw ex;
    }
  }



  /**
   * Generates an RSA key of length 1024.
   *
   * @return the bytes of the private key (encoded)
   */
  public static byte[] genKey()
    throws IOException {
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
      1000*60);

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
    throws IOException {
    
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
    throws IOException {

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
      1000*60);

    if(result.exitCode==0) {
      return result.stdOut;
    }
    throw new IOException("openssl exited with value " + result.exitCode);    
  }

//============
// Helpers
//============

  /**
   * Parses the output of a date from openssl.  I'm sure this format
   * has some RFC, but I don't know its name.  Just based on examining
   * the output.
   */
  private static Date parseCertDate(String str)
    throws IOException {
    SimpleDateFormat sdf = new SimpleDateFormat(CERT_DATE_FORMAT);
    try {
      return sdf.parse(str);
    }
    catch(java.text.ParseException ex) {
      throw new IOException("Unable to parse \"" +
        str + "\" into a date based on format \"" +
        CERT_DATE_FORMAT + "\"");
    }
  }
  

  /**
   * Parse an RFC2253 DN.  I hope the JavaSoft folks got the parsing
   * right, because I sure as hell didn't want to write the parser.
   */
  private static HashMap<String, String> parseDN(String dnStr)
    throws IOException {

    //TODO Replace with "RFC2253Name" class
    dnStr = dnStr.trim();
    try {
      HashMap<String, String> ret = new HashMap<String, String>();
      
      LdapName ldapName = new LdapName(dnStr);
  
      List<Rdn> rdns = ldapName.getRdns();
  
      for(Rdn rdn : rdns) {
        ret.put(rdn.getType().toString(), rdn.getValue().toString());
      }
      return ret;
    }
    catch(Exception ex) {
      throw new IOException("Unable to parse \"" +
        dnStr + "\" into a distinguished name.  Error: " +
        ex.toString());
    }
  }   

//============
// Testing
//============  

  public static void main(String[] args) throws Exception {
    /*
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
    */


    File f = new File(args[0]);
    CertInfo ci = getCertInfo(f);

    System.out.println(ci);


  

  }


//============================================================
// Trying to determine the "purpose" of a cert.  Example
// on a non-CA cert
//
// openssl x509 -in tomcat.cer -purpose -noout  
/*
Certificate purposes:
SSL client : Yes
SSL client CA : Yes (WARNING code=3)
SSL server : Yes
SSL server CA : Yes (WARNING code=3)
Netscape SSL server : Yes
Netscape SSL server CA : Yes (WARNING code=3)
S/MIME signing : Yes
S/MIME signing CA : Yes (WARNING code=3)
S/MIME encryption : Yes
S/MIME encryption CA : Yes (WARNING code=3)
CRL signing : Yes
CRL signing CA : Yes (WARNING code=3)
Any Purpose : Yes
Any Purpose CA : Yes
OCSP helper : Yes
OCSP helper CA : Yes (WARNING code=3)
*/

//Example on a CA cert
//openssl x509 -in ca-cert.pem -purpose -noout
/*
Certificate purposes:
SSL client : Yes
SSL client CA : Yes
SSL server : Yes
SSL server CA : Yes
Netscape SSL server : Yes
Netscape SSL server CA : Yes
S/MIME signing : Yes
S/MIME signing CA : Yes
S/MIME encryption : Yes
S/MIME encryption CA : Yes
CRL signing : Yes
CRL signing CA : Yes
Any Purpose : Yes
Any Purpose CA : Yes
OCSP helper : Yes
OCSP helper CA : Yes
*/
//============================================================


//======================================
// Here's a neat trick.  Using OpenSSL
// to get a site's cert
// openssl s_client -connect login.yahoo.com:443
//======================================
  

}

