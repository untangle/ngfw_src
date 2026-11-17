/**
 * Restricted JSON-RPC contracts for this subsystem.
 */
package com.untangle.uvm;

import com.untangle.uvm.util.SafeCheckParam;
import com.untangle.uvm.util.SafeType;

/**
 * Reviewed callable operations for SafeGoogleManager.
 */
public interface SafeGoogleManager
{
    /**
     * disconnectGoogleDrive through the reviewed RPC contract.
     */
    void disconnectGoogleDrive();

    /**
     * getAppSpecificGoogleDrivePath through the reviewed RPC contract.
     * @param appDirectory The appDirectory argument
     * @return The delegated result
     */
    String getAppSpecificGoogleDrivePath(@SafeCheckParam(SafeType.SIMPLE_TEXT) String appDirectory);

    /**
     * getAuthorizationUrl through the reviewed RPC contract.
     * @param windowProtocol The windowProtocol argument
     * @param windowLocation The windowLocation argument
     * @return The delegated result
     * @throws GoogleDriveOperationFailedException If the operation fails
     */
    String getAuthorizationUrl(@SafeCheckParam(SafeType.SIMPLE_TEXT) String windowProtocol, @SafeCheckParam(SafeType.SIMPLE_TEXT) String windowLocation) throws GoogleDriveOperationFailedException;

    /**
     * getGoogleCloudApp through the reviewed RPC contract.
     * @return The delegated result
     */
    GoogleCloudApp getGoogleCloudApp();

    /**
     * getSettings through the reviewed RPC contract.
     * @return The delegated result
     */
    GoogleSettings getSettings();

    /**
     * isGoogleDriveConnected through the reviewed RPC contract.
     * @return The delegated result
     */
    boolean isGoogleDriveConnected();

    /**
     * setSettings through the reviewed RPC contract.
     * @param settings The settings argument
     */
    void setSettings(GoogleSettings settings);

    /**
     * uploadToDrive through the reviewed RPC contract.
     * @param filePath The filePath argument
     * @param folderName The folderName argument
     * @return The delegated result
     * @throws GoogleDriveOperationFailedException If the operation fails
     */
    int uploadToDrive(@SafeCheckParam(SafeType.FILE_PATH) String filePath, @SafeCheckParam(SafeType.SIMPLE_TEXT) String folderName) throws GoogleDriveOperationFailedException;
}
