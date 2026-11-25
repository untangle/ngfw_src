package com.untangle.uvm;

import com.untangle.uvm.util.SafeCheckParam;
import com.untangle.uvm.util.SafeType;

/**
 * Combined Safe and additional ATS operations.
 */
public interface AtsGoogleManager extends SafeGoogleManager
{
    /**
     * provideDriveCode through the additional ATS contract.
     * @param nonce The nonce argument
     * @param code The code argument
     * @return The delegated result
     */
    String provideDriveCode(String nonce, @SafeCheckParam(SafeType.OAUTH_CODE) String code);
}
