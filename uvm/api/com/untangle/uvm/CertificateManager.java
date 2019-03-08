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

    static final String ROOT_CA_INSTALLER_SCRIPT = System.getProperty("uvm.bin.dir") + "/ut-rootgen-installer";
    static final String ROOT_CA_INSTALLER_DIRECTORY_NAME = System.getProperty("uvm.lib.dir") + "/untangle-vm/root_certificate_installer";;

    CertificateInformation getRootCertificateInformation();

    LinkedList<CertificateInformation> getServerCertificateList();

    boolean generateCertificateAuthority(String certSubject, String dummy);

    boolean generateServerCertificate(String certSubject, String altNames);

    public void removeServerCertificate(String fileName);

    public String validateActiveInspectorCertificates();
}
