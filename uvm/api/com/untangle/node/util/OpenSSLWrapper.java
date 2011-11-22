/*
 * $HeadURL$
 * Copyright (c) 2003-2007 Untangle, Inc.
 *
 * This library is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License, version 2,
 * as published by the Free Software Foundation.
 *
 * This library is distributed in the hope that it will be useful, but
 * AS-IS and WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE, TITLE, or
 * NONINFRINGEMENT.  See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Linking this library statically or dynamically with other modules is
 * making a combined work based on this library.  Thus, the terms and
 * conditions of the GNU General Public License cover the whole combination.
 *
 * As a special exception, the copyright holders of this library give you
 * permission to link this library with independent modules to produce an
 * executable, regardless of the license terms of these independent modules,
 * and to copy and distribute the resulting executable under terms of your
 * choice, provided that you also meet, for each linked independent module,
 * the terms and conditions of the license of that module.  An independent
 * module is a module which is not derived from or based on this library.
 * If you modify this library, you may extend this exception to your version
 * of the library, but you are not obligated to do so.  If you do not wish
 * to do so, delete this exception statement from your version.
 */

package com.untangle.node.util;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.naming.InvalidNameException;

import com.untangle.uvm.security.CertInfo;
import com.untangle.uvm.security.RFC2253Name;

/**
 * Wrapper around the OpenSSL application.  Note that the features
 * around using OpenSSL as a CA are somewhat stateful, so these have been
 * broken into {@link com.untangle.uvm.util.OpenSSLCAWrapper their own class}.
 * <br>
 * Note also that some of this could be obtained from Java's APIs, but those
 * are (a) way too complicated and (b) I don't trust Java as much as OpenSSL.
 */
public class OpenSSLWrapper {

    private static final String CERT_DATE_FORMAT =
        "MMM d HH:mm:ss yyy z";

    private static final String KEY_PAIR_VALID_DAYS = "3650"; // In the neighborhood of 10 years


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
        throws IOException, InvalidNameException {

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
            RFC2253Name subjectDN = null;
            RFC2253Name issuerDN = null;
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
                    subjectDN = RFC2253Name.parse(theLines[i].substring("subject=".length()));
                }
                if(theLines[i].startsWith("issuer=")) {
                    issuerDN = RFC2253Name.parse(theLines[i].substring("issuer=".length()));
                }
            }

            String cmd = System.getProperty("uvm.bin.dir") + "/ut-inspect_ca";

            //Now figure out if this is a CA
            result = SimpleExec.exec(
                                     cmd,
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

           CertInfo ci = new CertInfo(notBefore,
                                notAfter,
                                subjectDN,
                                issuerDN,
                                result.exitCode == 0,
                                prettyPrint(certFile));

	   ci.setAppearsSelfSignedFlag(appearsSelfSigned(certFile));

	   return ci;
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
        throws IOException, InvalidNameException {

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
        catch(InvalidNameException ex) {
            IOUtil.delete(temp);
            throw ex;
        }
    }

    public static String getCertFromPEM(String filename) {
        return extractMatchFromFile(filename, "-----BEGIN CERTIFICATE-----.+-----END CERTIFICATE-----");
    }

    public static String getCertKeyFromPEM(String filename) {
        return extractMatchFromFile(filename, "-----BEGIN .*? KEY-----.+-----END .*? KEY-----");
    }
    public static void generateSelfSignedCert(String alias, String filename)
        throws IOException {
        SimpleExec.SimpleExecResult resultKeyCopy = SimpleExec.exec( "openssl",
                                                                     new String[] {
                                                                      "rsa",
                                                                      "-in", 
                                                                      filename,
                                                                      "-out",
                                                                      filename+".new"
                                                                     },
                                                                     null,
                                                                     null,
                                                                     true,
                                                                     false,
                                                                     1000*60);

        if(resultKeyCopy.exitCode==0) {
            SimpleExec.SimpleExecResult result = SimpleExec.exec(
                                                             "openssl",
                                                             new String[] {
                                                                 "req",
                                                                 "-batch",
                                                                 "-subj",
                                                                 alias,
                                                                 "-new",
                                                                 "-x509",
                                                                 "-nodes",
								 "-days",
								 KEY_PAIR_VALID_DAYS,
                                                                 "-key",
                                                                 filename+".new",
                                                                 "-out",
                                                                 filename+".new",
                                                                 "-keyout",
                                                                 filename+".new"
                                                             },
                                                             null,
                                                             null,
                                                             true,
                                                             false,
                                                             1000*60);
              File oldCertFile = new File(filename);
              oldCertFile.renameTo(new File(filename+".old"));
              File newCertFile = new File(filename+".new");
              newCertFile.renameTo(new File(filename));
              @SuppressWarnings("unused")
              SimpleExec.SimpleExecResult resultRestartApache = SimpleExec.exec("apache2ctl", 
                                                                                new String[] {
                                                                                  "graceful"
                                                                                },
                                                                                null,
                                                                                null,
                                                                                true,
                                                                                false,
                                                                                1000*60);


            if(result.exitCode==0) {
                return;
            }
            throw new IOException("openssl exited with value " + result.exitCode);
        }
        throw new IOException("openssl exited with value " + resultKeyCopy.exitCode);
    }

    /**
     * Generates an RSA key of length 2048.
     *
     * @return the bytes of the private key (encoded)
     */
    public static byte[] genKey()
        throws IOException {
        //openssl genrsa 2048
        SimpleExec.SimpleExecResult result = SimpleExec.exec(
                                                             "openssl",
                                                             new String[] {
                                                                 "genrsa",
                                                                 "2048"
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
     * @param org "organization" (i.e. "untangle").
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
     * @param org "organization" (i.e. "untangle").
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

	return createCSR(subject.toString(), keyFile);
    }
    public static byte[] createCSR(String subject,
                                   File keyFile)
        throws IOException {

        SimpleExec.SimpleExecResult result = SimpleExec.exec(
                                                             "openssl",
                                                             new String[] {
                                                                 "req",
                                                                 "-new",
                                                                 "-key",
                                                                 keyFile.getAbsolutePath(),
                                                                 "-subj",
                                                                 subject
                                                             },
                                                             null,
                                                             null,
                                                             true,
                                                             false,
                                                             1000*60);

        if(result.exitCode==0) {
            return result.stdOut;
        }
        throw new IOException("openssl exited with value " + result.exitCode + " key file " + keyFile.getAbsolutePath() + " Subject " + subject.toString());
    }


    public static void importCert(byte[] certBytes, byte[] caBytes, File keyFile)
        throws IOException {
	File temp = File.createTempFile("apache", ".tmp");
	byte[] fullCertBytes;
        try {
	    SimpleExec.SimpleExecResult result = SimpleExec.exec(
								 "openssl",
								 new String[] {
								     "rsa",
								     "-in",
								     keyFile.getAbsolutePath()
								 },
								 null,
								 null,
								 true,
								 false,
								 1000*60);
	    
	    if (caBytes != null) {
		fullCertBytes = new byte[certBytes.length+result.stdOut.length+caBytes.length+2];
	    } else {
		fullCertBytes = new byte[certBytes.length+result.stdOut.length+2];
	    }

	    int i;
	    for (i = 0; i < certBytes.length; i++) {
		fullCertBytes[i] = certBytes[i];
	    }

	    fullCertBytes[certBytes.length] = Byte.parseByte("10");

	    for (i = 0; i < result.stdOut.length; i++) {
		fullCertBytes[certBytes.length+i+1] = result.stdOut[i];
	    }
	    
	    if (caBytes != null) {
		for (i = 0; i < caBytes.length; i++) {
		    fullCertBytes[certBytes.length+result.stdOut.length+i+1] = caBytes[i];
		}
	    }

	    IOUtil.bytesToFile(fullCertBytes, temp);
	    
	    if (appearsSelfSigned(temp) == false) {
		temp.renameTo(keyFile);
	    } else {
		throw new IOException("Error verifying cert against private key and CA");
	    }
            @SuppressWarnings("unused")
			SimpleExec.SimpleExecResult resultRestartApache = SimpleExec.exec("apache2ctl", 
                                                                              new String[] {
                                                                                "graceful"
                                                                              },
                                                                              null,
                                                                              null,
                                                                              true,
                                                                              false,
                                                                              1000*60);

        }
        catch(IOException ex) {
            IOUtil.delete(temp);
            throw ex;
        }
    }


    public static boolean appearsSelfSigned(File certFile) {
	try {
	    SimpleExec.SimpleExecResult verifyResult = SimpleExec.exec(
								       "openssl",
								       new String[] {
									   "verify",
									   "-CApath",
									   "/usr/share/ca-certificates",
									   "-CAfile",
									   certFile.getAbsolutePath(),
									   "-purpose",
									   "any",
									   certFile.getAbsolutePath()
								       },
								       null,
								       null,
								       true,
								       false,
								       1000*60);
	    if (verifyResult.exitCode == 0) {
		return false; //If openssl verify succeeds then it checks against a CA
	    }
	} catch(IOException ex) {
	    //hmm maybe do something here TODO FIXME !!!
        }
	
	return true;
    }
    

    //============
    // Helpers
    //============

    private static String extractMatchFromFile(String filename, String stringPattern) {
        try {
            BufferedReader in = new BufferedReader(new FileReader(filename));
            String input;
            String str = "";
            //read file into a string
            while ((input = in.readLine()) != null) {
                str += input + "\n";
            }
            in.close();
            Pattern pattern = Pattern.compile(stringPattern, Pattern.DOTALL);
            Matcher matcher = pattern.matcher(str);
            if(matcher.find()) {
                return matcher.group(0);
            } else {
                return null;
            }
        } catch (Exception e) { // FIXME
            return null;
        }
    }

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
    /*
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
    */
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

