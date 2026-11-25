package com.untangle.uvm;

import org.json.JSONObject;

import com.untangle.uvm.util.SafeCheckParam;
import com.untangle.uvm.util.SafeType;

/**
 * Combined Safe and additional ATS operations.
 */
public interface AtsCloudManager extends SafeCloudManager
{
    /**
     * accountLogin through the additional ATS contract.
     * @param login The login argument
     * @param password The password argument
     * @return The delegated result
     * @throws Exception If the operation fails
     */
    JSONObject accountLogin(@SafeCheckParam(SafeType.EMAIL) String login, @SafeCheckParam(SafeType.OPAQUE_SECRET) String password) throws Exception;
}
