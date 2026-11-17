/**
 * Restricted JSON-RPC contracts for this subsystem.
 */
package com.untangle.uvm;

import java.net.InetAddress;

/**
 * Reviewed callable operations for SafeAdminManager.
 */
public interface SafeAdminManager
{
    /**
     * getKernelVersion through the reviewed RPC contract.
     * @return The delegated result
     */
    String getKernelVersion();

    /**
     * getModificationState through the reviewed RPC contract.
     * @return The delegated result
     */
    String getModificationState();

    /**
     * getRebootCount through the reviewed RPC contract.
     * @return The delegated result
     */
    String getRebootCount();

    /**
     * getSettings through the reviewed RPC contract.
     * @return The delegated result
     */
    AdminSettings getSettings();

    /**
     * getSettingsV2 through the reviewed RPC contract.
     * @return The delegated result
     */
    AdminSettings getSettingsV2();

    /**
     * logAdminLoginEvent through the reviewed RPC contract.
     * @param login The login argument
     * @param local The local argument
     * @param clientAddress The clientAddress argument
     * @param succeeded The succeeded argument
     * @param reason The reason argument
     */
    void logAdminLoginEvent(String login, boolean local, InetAddress clientAddress, boolean succeeded, String reason);

    /**
     * setSettings through the reviewed RPC contract.
     * @param settings The settings argument
     */
    void setSettings(AdminSettings settings);

    /**
     * setSettingsV2 through the reviewed RPC contract.
     * @param settings The settings argument
     */
    void setSettingsV2(AdminSettings settings);
}
