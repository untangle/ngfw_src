package com.untangle.uvm;

import java.util.LinkedList;
import java.util.Objects;
import java.util.function.Supplier;

import com.untangle.uvm.util.SafeCheckParam;
import com.untangle.uvm.util.SafeType;

/**
 * Composed ATS facade; internal delegates are never exported.
 */
public final class AtsCertificateManagerImpl implements AtsCertificateManager
{
    private final SafeCertificateManager safe;
    private final Supplier<CertificateManager> delegate;

    /**
     * Create the composed ATS facade.
     * @param safe The normal Safe facade
     * @param delegate The internal delegate
     */
    AtsCertificateManagerImpl(SafeCertificateManager safe, Supplier<CertificateManager> delegate)
    {
        this.safe = Objects.requireNonNull(safe, "safe");
        this.delegate = Objects.requireNonNull(delegate, "delegate");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean generateCertificateAuthority(@SafeCheckParam(SafeType.NATURAL_NAME) String commonName, @SafeCheckParam(SafeType.CERT_SUBJECT) String certSubject)
    {
        return safe.generateCertificateAuthority(commonName, certSubject);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean generateServerCertificate(@SafeCheckParam(SafeType.CERT_SUBJECT) String certSubject, @SafeCheckParam(SafeType.SAN_LIST) String altNames)
    {
        return safe.generateServerCertificate(certSubject, altNames);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public CertificateInformation getRootCertificateInformation()
    {
        return safe.getRootCertificateInformation();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public LinkedList<CertificateInformation> getRootCertificateList()
    {
        return safe.getRootCertificateList();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public LinkedList<CertificateInformation> getServerCertificateList()
    {
        return safe.getServerCertificateList();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ExecManagerResult importSignedRequest(@SafeCheckParam(SafeType.PEM) String certData, @SafeCheckParam(SafeType.PEM) String extraData)
    {
        return safe.importSignedRequest(certData, extraData);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void removeCertificate(@SafeCheckParam(SafeType.ALPHANUM) String type, String fileName)
    {
        safe.removeCertificate(type, fileName);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setActiveRootCertificate(String fileName)
    {
        safe.setActiveRootCertificate(fileName);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ExecManagerResult uploadCertificate(@SafeCheckParam(SafeType.ALPHANUM) String certMode, @SafeCheckParam(SafeType.PEM) String certData, @SafeCheckParam(SafeType.PEM) String keyData, @SafeCheckParam(SafeType.PEM) String extraData)
    {
        return safe.uploadCertificate(certMode, certData, keyData, extraData);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String validateActiveInspectorCertificates()
    {
        return safe.validateActiveInspectorCertificates();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public CertificateInformation getRootCertificateInformation(@SafeCheckParam(SafeType.FILENAME) String fileName)
    {
        return ((CertificateManagerImpl) delegate.get()).getRootCertificateInformation(fileName);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public CertificateInformation getServerCertificateInformation(@SafeCheckParam(SafeType.FILENAME) String fileName)
    {
        return delegate.get().getServerCertificateInformation(fileName);
    }
}
