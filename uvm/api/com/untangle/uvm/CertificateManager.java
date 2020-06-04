/**
 * $Id$
 */
package com.untangle.uvm;

import java.util.LinkedList;

/**
 * Abstraction to the application server used for external web applications.
 */

public interface CertificateManager
{
    // this is where we store certificate files - MUST HAVE TRAILING SLASH
    static final String CERT_STORE_PATH = System.getProperty("uvm.settings.dir") + "/untangle-certificates/";

    static final String APACHE_PEM_FILE = "/etc/apache2/ssl/apache.pem";
    static final String CERT_FILE_PASSWORD = "password";

    LinkedList<CertificateInformation> getServerCertificateList();

    public CertificateInformation getServerCertificateInformation(String fileName);

    LinkedList<CertificateInformation> getRootCertificateList();

    CertificateInformation getRootCertificateInformation();

    boolean generateCertificateAuthority(String commonName, String certSubject);

    boolean generateServerCertificate(String certSubject, String altNames);

    ExecManagerResult uploadCertificate(String certMode, String certData, String keyData, String extraData);

    public void removeCertificate(String type, String fileName);

    public String validateActiveInspectorCertificates();
}
