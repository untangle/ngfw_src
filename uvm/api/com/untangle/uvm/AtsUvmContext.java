package com.untangle.uvm;

/**
 * Combined Safe and additional ATS operations.
 */
public interface AtsUvmContext extends SafeUvmContext
{
    /**
     * appManager through the combined ATS contract.
     * @return The delegated result
     */
    @Override
    AtsAppManager appManager();

    /**
     * certificateManager through the combined ATS contract.
     * @return The delegated result
     */
    @Override
    AtsCertificateManager certificateManager();

    /**
     * cloudManager through the combined ATS contract.
     * @return The delegated result
     */
    @Override
    AtsCloudManager cloudManager();

    /**
     * configManager through the combined ATS contract.
     * @return The delegated result
     */
    @Override
    AtsConfigManager configManager();

    /**
     * deviceTable through the combined ATS contract.
     * @return The delegated result
     */
    @Override
    AtsDeviceTable deviceTable();

    /**
     * geographyManager through the combined ATS contract.
     * @return The delegated result
     */
    @Override
    AtsGeographyManager geographyManager();

    /**
     * googleManager through the combined ATS contract.
     * @return The delegated result
     */
    @Override
    AtsGoogleManager googleManager();

    /**
     * hostTable through the combined ATS contract.
     * @return The delegated result
     */
    @Override
    AtsHostTable hostTable();

    /**
     * metricManager through the combined ATS contract.
     * @return The delegated result
     */
    @Override
    AtsMetricManager metricManager();

    /**
     * netspaceManager through the combined ATS contract.
     * @return The delegated result
     */
    @Override
    AtsNetspaceManager netspaceManager();

    /**
     * networkManager through the combined ATS contract.
     * @return The delegated result
     */
    @Override
    AtsNetworkManager networkManager();

    /**
     * systemManager through the combined ATS contract.
     * @return The delegated result
     */
    @Override
    AtsSystemManager systemManager();

    /**
     * uriManager through the combined ATS contract.
     * @return The delegated result
     */
    @Override
    AtsUriManager uriManager();

    /**
     * userTable through the combined ATS contract.
     * @return The delegated result
     */
    @Override
    AtsUserTable userTable();
}
