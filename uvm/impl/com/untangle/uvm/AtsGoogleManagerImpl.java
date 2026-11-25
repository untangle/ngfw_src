package com.untangle.uvm;

import java.util.Objects;
import java.util.function.Supplier;

import com.untangle.uvm.util.SafeCheckParam;
import com.untangle.uvm.util.SafeType;

/**
 * Composed ATS facade; internal delegates are never exported.
 */
public final class AtsGoogleManagerImpl implements AtsGoogleManager
{
    private final SafeGoogleManager safe;
    private final Supplier<GoogleManager> delegate;

    /**
     * Create the composed ATS facade.
     * @param safe The normal Safe facade
     * @param delegate The internal delegate
     */
    AtsGoogleManagerImpl(SafeGoogleManager safe, Supplier<GoogleManager> delegate)
    {
        this.safe = Objects.requireNonNull(safe, "safe");
        this.delegate = Objects.requireNonNull(delegate, "delegate");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void disconnectGoogleDrive()
    {
        safe.disconnectGoogleDrive();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getAppSpecificGoogleDrivePath(@SafeCheckParam(SafeType.SIMPLE_TEXT) String appDirectory)
    {
        return safe.getAppSpecificGoogleDrivePath(appDirectory);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getAuthorizationUrl(@SafeCheckParam(SafeType.SIMPLE_TEXT) String windowProtocol, @SafeCheckParam(SafeType.SIMPLE_TEXT) String windowLocation) throws GoogleDriveOperationFailedException
    {
        return safe.getAuthorizationUrl(windowProtocol, windowLocation);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public GoogleCloudApp getGoogleCloudApp()
    {
        return safe.getGoogleCloudApp();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public GoogleSettings getSettings()
    {
        return safe.getSettings();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isGoogleDriveConnected()
    {
        return safe.isGoogleDriveConnected();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setSettings(GoogleSettings settings)
    {
        safe.setSettings(settings);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int uploadToDrive(@SafeCheckParam(SafeType.FILE_PATH) String filePath, @SafeCheckParam(SafeType.SIMPLE_TEXT) String folderName) throws GoogleDriveOperationFailedException
    {
        return safe.uploadToDrive(filePath, folderName);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String provideDriveCode(String nonce, @SafeCheckParam(SafeType.OAUTH_CODE) String code)
    {
        return delegate.get().provideDriveCode(nonce, code);
    }
}
