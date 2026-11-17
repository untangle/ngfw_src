/**
 * Restricted JSON-RPC contracts for this subsystem.
 */
package com.untangle.uvm;

import com.untangle.uvm.util.SafeCheckParam;
import com.untangle.uvm.util.SafeType;

/**
 * Reviewed callable operations for SafeMailSender.
 */
public interface SafeMailSender
{
    /**
     * getSettings through the reviewed RPC contract.
     * @return The delegated result
     */
    MailSettings getSettings();

    /**
     * sendTestMessage through the reviewed RPC contract.
     * @param recipient The recipient argument
     * @return The delegated result
     */
    String sendTestMessage(@SafeCheckParam(SafeType.EMAIL) String recipient);

    /**
     * setSettings through the reviewed RPC contract.
     * @param settings The settings argument
     */
    void setSettings(MailSettings settings);
}
