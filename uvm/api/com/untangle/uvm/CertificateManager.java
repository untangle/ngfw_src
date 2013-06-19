/*
 * $Id$
 */

package com.untangle.uvm;

/**
 * Abstraction to the application server used for external web applications.
 */

public interface CertificateManager
{
    CertificateInformation getCertificateInformation();

    boolean generateCertificateAuthority(String certSubject);

    boolean generateServerCertificate(String certSubject);
}
