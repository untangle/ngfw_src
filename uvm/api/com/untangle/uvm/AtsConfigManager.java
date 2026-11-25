package com.untangle.uvm;

import com.untangle.uvm.util.SafeCheckParam;
import com.untangle.uvm.util.SafeType;

/**
 * Combined Safe and additional ATS operations.
 */
public interface AtsConfigManager extends SafeConfigManager
{
    /**
     * getDomainName through the additional ATS contract.
     * @return The delegated result
     */
    Object getDomainName();

    /**
     * getHostName through the additional ATS contract.
     * @return The delegated result
     */
    Object getHostName();

    /**
     * restoreSystemBackup through the additional ATS contract.
     * @param argFileName The argFileName argument
     * @param maintainRegex The maintainRegex argument
     * @return The delegated result
     */
    Object restoreSystemBackup(@SafeCheckParam(SafeType.FILE_PATH) String argFileName, @SafeCheckParam(SafeType.REGEX_PATTERN) String maintainRegex);

    /**
     * setDomainName through the additional ATS contract.
     * @param argName The argName argument
     * @return The delegated result
     */
    Object setDomainName(@SafeCheckParam(SafeType.HOSTNAME) String argName);

    /**
     * setHostName through the additional ATS contract.
     * @param argName The argName argument
     * @return The delegated result
     */
    Object setHostName(@SafeCheckParam(SafeType.HOSTNAME) String argName);
}
