/*
 * $Id$
 */
package com.untangle.uvm;

import javax.servlet.ServletContext;
import org.apache.catalina.Valve;

import com.untangle.uvm.security.CertInfo;
import com.untangle.uvm.security.RFC2253Name;

/**
 * Abstraction to the application server used for external web
 * applications.
 */
public interface CertificateManager
{
    final String UVM_WEB_MESSAGE_ATTR = "com.untangle.uvm.web.message";

    /**
     * Regenerate the self-signed certificate for this instance. The
     * key algorithm and strength are determined by the
     * implementation.
     *
     * Note the following are the common attributes of a Distinguished
     * Name (and should be solicited from the requestor of this
     * method).
     *
     * <ul>
     *   <li><code>O</code>The organization (i.e. "Widgets, Inc")</li>
     *   <li><code>OU</code>The organization unit (i.e. "Button Division")</li>
     *   <li><code>L</code>The city (i.e. "San Mateo")</li>
     *   <li><code>ST</code>The state (i.e. "California")</li>
     *   <li><code>C</code>The country <b>code</b> (i.e. "US")</li>
     * </ul>
     *
     * The most important attribute is <code>CN</code>. This is the
     * external hostname of the machine (i.e. "www.widgets.com"),
     * which should not be solicited as it will be filled-in
     * automagically.
     *
     * Since there should always be a "current" cert for the server,
     * it may be accessed via {@link #getCurrentServerCert
     * getCurrentServerCert} and its information accessed via the
     * helper method {@link #getCertInfo getCertInfo}.
     *
     * @param dn the distinguished name object
     * @param durationInDays how long this self-signed cert should be
     * good-for
     *
     * @return true if created successfully. False if there was a
     * problem. Note that since no user-defined parameters can cause
     * an exception (almost anything should be encodable for the user
     * values) there is no explanation given to the caller. The error
     * will be in the logs and engineering will have to investigate.
     */
    boolean regenCert(RFC2253Name dn, int durationInDays);

    /**
     * Import the signed certificate for this machine. The input to
     * this method should be the bytes returned from a CA (where the
     * output of {@link #generateCSR generateCSR} was submitted).
     *
     * This method optionaly takes a second cert. This is for cases
     * when the CA says the user needs an "intermediate certificate".
     * This is a cert along the trust chain between the signed server
     * cert and the implicit trusted roots (burned-into Java). If such
     * an intermediate cert is required, the CA will inform users of
     * this.
     *
     * Warning. This cert will be registered under the hostname (CN)
     * within the signed document. If this does not agree with the
     * current hostname, it won't "take effect". To check the hostname
     * (CN) of the cert being imported, one may call {@link
     * #getCertInfo getCertInfo}.
     *
     * @param cert the cert <b>for this server</b>
     * @param caCert a CA cert (may be null).
     * @return true if created successfully. False if there was an
     * error. It is hard to determine what the true nature of the
     * error was. To be safe, call {@link #getCertInfo getCertInfo}
     * first. If that method returns no errors, then the error is
     * likely not the fault of the user input.
     */
    boolean importServerCert(byte[] cert, byte[] caCert);

    /**
     * Get the current certificate used by this instance of the UVM.
     * Further information for this cert can be obtained via {@link
     * #getCertInfo getCertInfo}.
     *
     * @return the current cert, or null if there is a severe problem
     * on the server.
     */
    byte[] getCurrentServerCert();

    /**
     * Generate a certificate signature request (CSR), for submission
     * to a CA for signature. The output is a base64 encoded text
     * block suitable for submission in its exact form. The DN
     * information is based on the {@link #getCurrentCert current
     * cert}.
     *
     * @return the CSR, or null if there is an error. Any error is the
     * result of code failure. The logs will contain the error and
     * users are advised to contact Untangle support.
     */
    byte[] generateCSR();

    /**
     * Get information on the given certificate. This method is
     * offered for the UI which will not have access to the libraries
     * (which call native code) to inspect certs.
     *
     * @param certBytes the bytes of the certificate.
     *
     * @return the CertInfo, or null if the cert could not be parsed.
     */
    CertInfo getCertInfo(byte[] certBytes);
    
    /**
	 * Get information on the current certificate used by this 
	 * instance of the UVM. This method is
	 * offered for the UI which will not have access to the libraries
	 * (which call native code) to inspect certs.     
	 * 
	 * @return the CertInfo of the current cert, or null if there is a severe problem
	 * on the server.
	 */
	CertInfo getCurrentServerCertInfo();
}
