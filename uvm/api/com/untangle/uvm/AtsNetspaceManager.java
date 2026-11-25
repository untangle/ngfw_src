package com.untangle.uvm;

import com.untangle.uvm.NetspaceManager.IPVersion;
import com.untangle.uvm.app.IPMaskedAddress;

/**
 * Combined Safe and additional ATS operations.
 */
public interface AtsNetspaceManager extends SafeNetspaceManager
{
    /**
     * getAvailableAddressSpace through the additional ATS contract.
     * @param version The version argument
     * @param hostId The hostId argument
     * @return The delegated result
     */
    IPMaskedAddress getAvailableAddressSpace(IPVersion version, int hostId);
}
