/**
 * Restricted JSON-RPC contracts for this subsystem.
 */
package com.untangle.uvm;

import java.util.Objects;
import java.util.function.Supplier;

import com.untangle.uvm.util.SafeCheckParam;
import com.untangle.uvm.util.SafeType;

/**
 * Concrete restricted facade; the full delegate is never exported.
 */
public final class SafeGoogleManagerImpl implements SafeGoogleManager
{
    private final Supplier<GoogleManager> delegate;

    /**
     * Create the restricted facade.
     * @param delegate The internal delegate
     */
    SafeGoogleManagerImpl(Supplier<GoogleManager> delegate)
    {
        this.delegate = Objects.requireNonNull(delegate, "delegate");
    }

    /**
     * disconnectGoogleDrive through the reviewed RPC contract.
     */
    @Override
    public void disconnectGoogleDrive()
    {
        delegate.get().disconnectGoogleDrive();
    }

    /**
     * getAppSpecificGoogleDrivePath through the reviewed RPC contract.
     * @param appDirectory The appDirectory argument
     * @return The delegated result
     */
    @Override
    public String getAppSpecificGoogleDrivePath(@SafeCheckParam(SafeType.SIMPLE_TEXT) String appDirectory)
    {
        return delegate.get().getAppSpecificGoogleDrivePath(appDirectory);
    }

    /**
     * getAuthorizationUrl through the reviewed RPC contract.
     * @param windowProtocol The windowProtocol argument
     * @param windowLocation The windowLocation argument
     * @return The delegated result
     * @throws GoogleDriveOperationFailedException If the operation fails
     */
    @Override
    public String getAuthorizationUrl(@SafeCheckParam(SafeType.SIMPLE_TEXT) String windowProtocol, @SafeCheckParam(SafeType.SIMPLE_TEXT) String windowLocation) throws GoogleDriveOperationFailedException
    {
        return delegate.get().getAuthorizationUrl(windowProtocol, windowLocation);
    }

    /**
     * getGoogleCloudApp through the reviewed RPC contract.
     * @return The delegated result
     */
    @Override
    public GoogleCloudApp getGoogleCloudApp()
    {
        return delegate.get().getGoogleCloudApp();
    }

    /**
     * getSettings through the reviewed RPC contract.
     * @return The delegated result
     */
    @Override
    public GoogleSettings getSettings()
    {
        return delegate.get().getSettings();
    }

    /**
     * isGoogleDriveConnected through the reviewed RPC contract.
     * @return The delegated result
     */
    @Override
    public boolean isGoogleDriveConnected()
    {
        return delegate.get().isGoogleDriveConnected();
    }

    /**
     * setSettings through the reviewed RPC contract.
     * @param settings The settings argument
     */
    @Override
    public void setSettings(GoogleSettings settings)
    {
        delegate.get().setSettings(settings);
    }

    /**
     * uploadToDrive through the reviewed RPC contract.
     * @param filePath The filePath argument
     * @param folderName The folderName argument
     * @return The delegated result
     * @throws GoogleDriveOperationFailedException If the operation fails
     */
    @Override
    public int uploadToDrive(@SafeCheckParam(SafeType.FILE_PATH) String filePath, @SafeCheckParam(SafeType.SIMPLE_TEXT) String folderName) throws GoogleDriveOperationFailedException
    {
        return delegate.get().uploadToDrive(filePath, folderName);
    }
}
