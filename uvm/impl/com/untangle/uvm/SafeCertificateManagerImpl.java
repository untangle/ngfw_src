/**
 * Restricted JSON-RPC contracts for this subsystem.
 */
package com.untangle.uvm;

import java.util.LinkedList;
import java.util.Objects;
import java.util.function.Supplier;

import com.untangle.uvm.util.SafeCheckParam;
import com.untangle.uvm.util.SafeType;

/**
 * Concrete restricted facade; the full delegate is never exported.
 */
public final class SafeCertificateManagerImpl implements SafeCertificateManager
{
    private final Supplier<CertificateManager> delegate;

    /**
     * Create the restricted facade.
     * @param delegate The internal delegate
     */
    SafeCertificateManagerImpl(Supplier<CertificateManager> delegate)
    {
        this.delegate = Objects.requireNonNull(delegate, "delegate");
    }

    /**
     * generateCertificateAuthority through the reviewed RPC contract.
     * @param commonName The commonName argument
     * @param certSubject The certSubject argument
     * @return The delegated result
     */
    @Override
    public boolean generateCertificateAuthority(@SafeCheckParam(SafeType.NATURAL_NAME) String commonName, @SafeCheckParam(SafeType.CERT_SUBJECT) String certSubject)
    {
        return delegate.get().generateCertificateAuthority(commonName, certSubject);
    }

    /**
     * generateServerCertificate through the reviewed RPC contract.
     * @param certSubject The certSubject argument
     * @param altNames The altNames argument
     * @return The delegated result
     */
    @Override
    public boolean generateServerCertificate(@SafeCheckParam(SafeType.CERT_SUBJECT) String certSubject, @SafeCheckParam(SafeType.SAN_LIST) String altNames)
    {
        return delegate.get().generateServerCertificate(certSubject, altNames);
    }

    /**
     * getRootCertificateInformation through the reviewed RPC contract.
     * @return The delegated result
     */
    @Override
    public CertificateInformation getRootCertificateInformation()
    {
        return delegate.get().getRootCertificateInformation();
    }

    /**
     * getRootCertificateList through the reviewed RPC contract.
     * @return The delegated result
     */
    @Override
    public LinkedList<CertificateInformation> getRootCertificateList()
    {
        return delegate.get().getRootCertificateList();
    }

    /**
     * getServerCertificateList through the reviewed RPC contract.
     * @return The delegated result
     */
    @Override
    public LinkedList<CertificateInformation> getServerCertificateList()
    {
        return delegate.get().getServerCertificateList();
    }

    /**
     * importSignedRequest through the reviewed RPC contract.
     * @param certData The certData argument
     * @param extraData The extraData argument
     * @return The delegated result
     */
    @Override
    public ExecManagerResult importSignedRequest(@SafeCheckParam(SafeType.PEM) String certData, @SafeCheckParam(SafeType.PEM) String extraData)
    {
        return ((CertificateManagerImpl) delegate.get()).importSignedRequest(certData, extraData);
    }

    /**
     * removeCertificate through the reviewed RPC contract.
     * @param type The type argument
     * @param fileName The fileName argument
     */
    @Override
    public void removeCertificate(@SafeCheckParam(SafeType.ALPHANUM) String type, String fileName)
    {
        delegate.get().removeCertificate(type, fileName);
    }

    /**
     * setActiveRootCertificate through the reviewed RPC contract.
     * @param fileName The fileName argument
     */
    @Override
    public void setActiveRootCertificate(String fileName)
    {
        ((CertificateManagerImpl) delegate.get()).setActiveRootCertificate(fileName);
    }

    /**
     * uploadCertificate through the reviewed RPC contract.
     * @param certMode The certMode argument
     * @param certData The certData argument
     * @param keyData The keyData argument
     * @param extraData The extraData argument
     * @return The delegated result
     */
    @Override
    public ExecManagerResult uploadCertificate(@SafeCheckParam(SafeType.ALPHANUM) String certMode, @SafeCheckParam(SafeType.PEM) String certData, @SafeCheckParam(SafeType.PEM) String keyData, @SafeCheckParam(SafeType.PEM) String extraData)
    {
        return delegate.get().uploadCertificate(certMode, certData, keyData, extraData);
    }

    /**
     * validateActiveInspectorCertificates through the reviewed RPC contract.
     * @return The delegated result
     */
    @Override
    public String validateActiveInspectorCertificates()
    {
        return delegate.get().validateActiveInspectorCertificates();
    }
}
