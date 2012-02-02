/**
 * $Id$
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
import com.untangle.uvm.ExecManagerResult;
import com.untangle.uvm.UvmContextFactory;

/**
 * Wrapper around the OpenSSL application.  Note that the features
 * around using OpenSSL as a CA are somewhat stateful, so these have been
 * broken into {@link com.untangle.uvm.util.OpenSSLCAWrapper their own class}.
 * <br>
 * Note also that some of this could be obtained from Java's APIs, but those
 * are (a) way too complicated and (b) I don't trust Java as much as OpenSSL.
 */
public class OpenSSLWrapper
{
    private static final String CERT_DATE_FORMAT = "MMM d HH:mm:ss yyy z";
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
        throws IOException
    {

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
        throws IOException
    {

        if(!certFile.exists()) {
            throw new FileNotFoundException(certFile.getAbsolutePath());
        }

        ExecManagerResult result = UvmContextFactory.context().execManager().exec("openssl x509 -in " + certFile.getAbsolutePath() + " -text");

        if(result.getResult()==0) {
            return result.getOutput();
        }

        throw new IOException("Error printing cert file.  Return: " + result.getResult() + " output: \"" + result.getOutput() + "\"");

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
        throws IOException, InvalidNameException
    {

        //=================================================
        // Tested command as follows:
        // prompt> openssl x509 -in gobbles.pem -dates -noout -subject -issuer -nameopt RFC2253
        // prompt> notBefore=Jan  4 22:53:52 2006 GMT
        // prompt> notAfter=Jan  4 22:53:52 2007 GMT
        // prompt> subject= CN=gobbles.untangle.com,L=San Mateo,ST=CA,C=US
        // prompt> issuer= CN=gobbles.untangle.com,L=San Mateo,ST=CA,C=US
        //
        // Note that it exits "0" if there are no problems.
        //
        //
        // Then to determine if it is a CA, call "inspect_ca" which should
        // be in "/usr/bin".  It exits with "1" if it is not a CA (0 if it is).
        //=================================================

        ExecManagerResult result = UvmContextFactory.context().execManager().exec("openssl x509 -in " + certFile.getAbsolutePath() + " -dates -noout -subject -issuer -nameopt RFC2253");

        if(result.getResult()==0) {
            Date notBefore = null;
            Date notAfter = null;
            RFC2253Name subjectDN = null;
            RFC2253Name issuerDN = null;
            //Time to parse the output (I hate this in Java)
            String[] theLines = result.getOutput().split("(?m)$");//The "(?m") crap is so Java's
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
            result = UvmContextFactory.context().execManager().exec( cmd + " " + certFile.getAbsolutePath() );

            CertInfo ci = new CertInfo(notBefore,
                                       notAfter,
                                       subjectDN,
                                       issuerDN,
                                       result.getResult() == 0,
                                       prettyPrint(certFile));

            ci.setAppearsSelfSignedFlag(appearsSelfSigned(certFile));

            return ci;
        }

        throw new IOException("openssl exited with value " + result.getResult());
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
        throws IOException, InvalidNameException
    {

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

    public static String getCertFromPEM(String filename)
    {
        return extractMatchFromFile(filename, "-----BEGIN CERTIFICATE-----.+-----END CERTIFICATE-----");
    }

    public static String getCertKeyFromPEM(String filename)
    {
        return extractMatchFromFile(filename, "-----BEGIN .*? KEY-----.+-----END .*? KEY-----");
    }

    public static void generateSelfSignedCert(String alias, String filename)
        throws IOException
    {
        ExecManagerResult resultKeyCopy = UvmContextFactory.context().execManager().exec( "openssl rsa -in " + filename + " -out " + (filename+".new"));

        if(resultKeyCopy.getResult()==0) {
            ExecManagerResult result = UvmContextFactory.context().execManager().exec("openssl req -batch -subj alias -new -x509 -nodes -days " + KEY_PAIR_VALID_DAYS +
                                                                                      " -key " + (filename+".new") +
                                                                                      " -out " + (filename+".new") +
                                                                                      " -keyout " + (filename+".new"));
            File oldCertFile = new File(filename);
            oldCertFile.renameTo(new File(filename+".old"));
            File newCertFile = new File(filename+".new");
            newCertFile.renameTo(new File(filename));
            UvmContextFactory.context().execManager().exec("apache2ctl graceful");

            if(result.getResult()==0) {
                return;
            }
            throw new IOException("openssl exited with value " + result.getResult());
        }
        throw new IOException("openssl exited with value " + resultKeyCopy.getResult());
    }

    /**
     * Generates an RSA key of length 2048.
     *
     * @return the bytes of the private key (encoded)
     */
    public static byte[] genKey()
        throws IOException
    {
        //openssl genrsa 2048
        ExecManagerResult result = UvmContextFactory.context().execManager().exec( "openssl genrsa 2048");

        if(result.getResult()==0) {
            return result.getOutput().getBytes();
        }
        throw new IOException("openssl exited with value " + result.getResult());
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
    public static byte[] createCSR(String cn, String org, String country, String state, String city, byte[] privateKey)
        throws IOException
    {
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
    public static byte[] createCSR(String cn, String org, String country, String state, String city, File keyFile)
        throws IOException
    {

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

    public static byte[] createCSR(String subject, File keyFile)
        throws IOException
    {
        ExecManagerResult result = UvmContextFactory.context().execManager().exec("openssl req -new -key " + keyFile.getAbsolutePath()  +" -subj " + subject);

        if(result.getResult()==0) {
            return result.getOutput().getBytes();
        }
        throw new IOException("openssl exited with value " + result.getResult() + " key file " + keyFile.getAbsolutePath() + " Subject " + subject.toString());
    }


    public static void importCert(byte[] certBytes, byte[] caBytes, File keyFile)
        throws IOException
    {
        File temp = File.createTempFile("apache", ".tmp");
        byte[] fullCertBytes;
        try {
            ExecManagerResult result = UvmContextFactory.context().execManager().exec( "openssl rsa -i " + keyFile.getAbsolutePath());

            byte[] outputBytes = result.getOutput().getBytes();
            
            if (caBytes != null) {
                fullCertBytes = new byte[certBytes.length+outputBytes.length+caBytes.length+2];
            } else {
                fullCertBytes = new byte[certBytes.length+outputBytes.length+2];
            }

            int i;
            for (i = 0; i < certBytes.length; i++) {
                fullCertBytes[i] = certBytes[i];
            }

            fullCertBytes[certBytes.length] = Byte.parseByte("10");

            for (i = 0; i < outputBytes.length; i++) {
                fullCertBytes[certBytes.length+i+1] = outputBytes[i];
            }
	    
            if (caBytes != null) {
                for (i = 0; i < caBytes.length; i++) {
                    fullCertBytes[certBytes.length+outputBytes.length+i+1] = caBytes[i];
                }
            }

            IOUtil.bytesToFile(fullCertBytes, temp);
	    
            if (appearsSelfSigned(temp) == false) {
                temp.renameTo(keyFile);
            } else {
                throw new IOException("Error verifying cert against private key and CA");
            }
			UvmContextFactory.context().execManager().exec("apache2ctl graceful");
        }
        catch(IOException ex) {
            IOUtil.delete(temp);
            throw ex;
        }
    }


    public static boolean appearsSelfSigned(File certFile)
    {
        ExecManagerResult verifyResult = UvmContextFactory.context().execManager().exec("openssl verify -CApath /usr/share/ca-certificates -CAfile " + certFile.getAbsolutePath() + " -purpose any " + certFile.getAbsolutePath());

        if (verifyResult.getResult() == 0) {
            return false; //If openssl verify succeeds then it checks against a CA
        }
	
        return true;
    }
    

    //============
    // Helpers
    //============

    private static String extractMatchFromFile(String filename, String stringPattern)
    {
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
        throws IOException
    {
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

