/*
 * $Id: CertificateManager.java 35697 2013-08-22 05:43:10Z mahotz $
 */

package com.untangle.uvm;

/**
 * Abstraction to the application server used for external web applications.
 */

public interface CertificateManager
{
    CertificateInformation getCertificateInformation();

    boolean generateCertificateAuthority(String certSubject, String dummy);

    boolean generateServerCertificate(String certSubject, String altNames);
}
