/**
 * Restricted JSON-RPC contracts for this subsystem.
 */
package com.untangle.uvm;

import java.util.LinkedList;

import com.untangle.uvm.util.SafeCheckParam;
import com.untangle.uvm.util.SafeType;

/**
 * Reviewed callable operations for SafeCertificateManager.
 */
public interface SafeCertificateManager
{
    /**
     * generateCertificateAuthority through the reviewed RPC contract.
     * @param commonName The commonName argument
     * @param certSubject The certSubject argument
     * @return The delegated result
     */
    boolean generateCertificateAuthority(@SafeCheckParam(SafeType.NATURAL_NAME) String commonName, @SafeCheckParam(SafeType.CERT_SUBJECT) String certSubject);

    /**
     * generateServerCertificate through the reviewed RPC contract.
     * @param certSubject The certSubject argument
     * @param altNames The altNames argument
     * @return The delegated result
     */
    boolean generateServerCertificate(@SafeCheckParam(SafeType.CERT_SUBJECT) String certSubject, @SafeCheckParam(SafeType.SAN_LIST) String altNames);

    /**
     * getRootCertificateInformation through the reviewed RPC contract.
     * @return The delegated result
     */
    CertificateInformation getRootCertificateInformation();

    /**
     * getRootCertificateList through the reviewed RPC contract.
     * @return The delegated result
     */
    LinkedList<CertificateInformation> getRootCertificateList();

    /**
     * getServerCertificateList through the reviewed RPC contract.
     * @return The delegated result
     */
    LinkedList<CertificateInformation> getServerCertificateList();

    /**
     * importSignedRequest through the reviewed RPC contract.
     * @param certData The certData argument
     * @param extraData The extraData argument
     * @return The delegated result
     */
    ExecManagerResult importSignedRequest(@SafeCheckParam(SafeType.PEM) String certData, @SafeCheckParam(SafeType.PEM) String extraData);

    /**
     * removeCertificate through the reviewed RPC contract.
     * @param type The type argument
     * @param fileName The fileName argument
     */
    void removeCertificate(@SafeCheckParam(SafeType.ALPHANUM) String type, String fileName);

    /**
     * setActiveRootCertificate through the reviewed RPC contract.
     * @param fileName The fileName argument
     */
    void setActiveRootCertificate(String fileName);

    /**
     * uploadCertificate through the reviewed RPC contract.
     * @param certMode The certMode argument
     * @param certData The certData argument
     * @param keyData The keyData argument
     * @param extraData The extraData argument
     * @return The delegated result
     */
    ExecManagerResult uploadCertificate(@SafeCheckParam(SafeType.ALPHANUM) String certMode, @SafeCheckParam(SafeType.PEM) String certData, @SafeCheckParam(SafeType.PEM) String keyData, @SafeCheckParam(SafeType.PEM) String extraData);

    /**
     * validateActiveInspectorCertificates through the reviewed RPC contract.
     * @return The delegated result
     */
    String validateActiveInspectorCertificates();
}
