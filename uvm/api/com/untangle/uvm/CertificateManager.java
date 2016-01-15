/*
 * $Id$
 */

package com.untangle.uvm;

/**
 * Abstraction to the application server used for external web applications.
 */

public interface CertificateManager
{
    String ROOT_CA_INSTALLER_SCRIPT = System.getProperty("uvm.bin.dir") + "/ut-rootgen-installer";
    String ROOT_CA_INSTALLER_DIRECTORY_NAME = System.getProperty("uvm.lib.dir") + "/untangle-vm/root_certificate_installer";;

    CertificateInformation getCertificateInformation();

    boolean generateCertificateAuthority(String certSubject, String dummy);

    boolean generateServerCertificate(String certSubject, String altNames);
}
