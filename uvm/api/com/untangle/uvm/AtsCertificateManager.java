package com.untangle.uvm;

import com.untangle.uvm.util.SafeCheckParam;
import com.untangle.uvm.util.SafeType;

/**
 * Combined Safe and additional ATS operations.
 */
public interface AtsCertificateManager extends SafeCertificateManager
{
    /**
     * getRootCertificateInformation through the additional ATS contract.
     * @param fileName The fileName argument
     * @return The delegated result
     */
    CertificateInformation getRootCertificateInformation(@SafeCheckParam(SafeType.FILENAME) String fileName);

    /**
     * getServerCertificateInformation through the additional ATS contract.
     * @param fileName The fileName argument
     * @return The delegated result
     */
    CertificateInformation getServerCertificateInformation(@SafeCheckParam(SafeType.FILENAME) String fileName);
}
