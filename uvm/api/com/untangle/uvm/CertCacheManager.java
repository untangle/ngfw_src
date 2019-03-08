/**
 * $Id$
 */
package com.untangle.uvm;

import java.security.cert.X509Certificate;

public interface CertCacheManager
{
    public X509Certificate fetchServerCertificate(String serverAddress);

    public void updateServerCertificate(String serverAddress, X509Certificate serverCertificate);
}
