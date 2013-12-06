package com.untangle.uvm;

import java.security.cert.X509Certificate;

public interface CertCacheManager
{
    public X509Certificate searchServerCertificate(String serverAddress);

    public X509Certificate fetchServerCertificate(String serverAddress);

    public void storeServerCertificate(String serverAddress, X509Certificate serverCertificate);
}
