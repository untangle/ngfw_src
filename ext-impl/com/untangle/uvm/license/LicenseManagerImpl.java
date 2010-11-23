/*
 * Copyright (c) 2003-2007 Untangle, Inc.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Untangle, Inc. ("Confidential Information"). You shall
 * not disclose such Confidential Information.
 *
 * $Id$
 */
package com.untangle.uvm.license;

import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import org.apache.log4j.Logger;

import com.untangle.uvm.LocalUvmContextFactory;
import com.untangle.uvm.util.Pulse;
import com.untangle.uvm.SettingsManager;

public class LicenseManagerImpl implements LicenseManager
{
    private static final String EXPIRED = "expired";

    /* update every 2 hours, leaves an hour window */
    private static final long TIMER_DELAY = 1000 * 60 * 60 * 2;

    /* validate the product for three hours. */
    private static final long VALIDATION_PERIOD = 3 * 60 * 60 * 1000;

    /* has to be declared after all static declarations */
    private static final LicenseManagerImpl INSTANCE;

    /**
     * Map from the product name to the latest valid license available for this product
     * This is where the fully evaluated license are stored
     * This map stores the evaluated (validated) licenses
     */
    private Map<String, License> licenseMap = new HashMap<String, License>();

    /**
     * A list of all known licenses
     * These are fully evaluated (validated) license
     */
    private List<License> licenseList = new LinkedList<License>();

    /**
     * The current settings
     * Contains a list of all known licenses store locally
     * Note: the licenses in the settings don't have metadata
     */
    private LicenseSettings settings;

    /**
     * Amount of time to update the license for, if the timer task dies, all
     * license will expire in <code>validationPeriod</code> milliseconds
     */
    private final long validationPeriod = VALIDATION_PERIOD;

    /** Timer task */
    private final LicenseUpdateTask task = new LicenseUpdateTask();

    /** Pulse that updates the expiration dates, this is a daemon task. */
    private final Pulse pulse = new Pulse("uvm-lmi", true, task);

    private final Logger logger = Logger.getLogger(getClass());

    private LicenseManagerImpl()
    {
        this.readLicenses();
        this.mapLicenses();
    }

    /**
     * Reload all of the licenses from the file system.
     */
    @Override
    public final void reloadLicenses()
    {
        scheduleAndWait();
    }

    @Override
    public final License getLicense(String identifier)
    {
        License license = this.licenseMap.get(identifier);

        if (license != null)
            return license;
        
        /**
         * Special for development environment
         * Assume all licenses are valid
         * This should be removed if you want to test the licensing in the dev environment
         */
        if (LocalUvmContextFactory.context().isDevel()) {
            logger.warn("Creating development license: " + identifier);
            license = new License(identifier, identifier, "Development", 0, 9999999999l, "development", 1, Boolean.TRUE);
            this.licenseMap.put(identifier,license);
            return license;
        }

        logger.warn("No license found for: " + identifier + " - Creating invalid license...");
        license = new License(identifier, identifier, "Subscription", 0, 0, "invalid", 1, Boolean.FALSE);
        this.licenseMap.put(identifier,license);
        return license;
    }

    @Override
    public final boolean hasPremiumLicense()
    {
        return this.settings.getLicenses().size() > 0;
    }

    public void initializeSettings()
    {
        logger.info("Initializing Settings...");

        List<License> licenses = new LinkedList<License>();
        this.settings = new LicenseSettings(licenses);

        this._saveSettings(this.settings);
    }

    /**
     * Read the licenses and load them into the current settings object
     */
    @SuppressWarnings("unchecked") //LinkedList<License> <-> LinkedList
    private synchronized void readLicenses()
    {
        SettingsManager settingsManager = LocalUvmContextFactory.context().settingsManager();
        LinkedList<License> licenses;
        
        try {
            this.settings = settingsManager.loadBasePath(LicenseSettings.class, System.getProperty("uvm.conf.dir"), "licenses", "licenses");
        } catch (SettingsManager.SettingsException e) {
            logger.error("Unable to read license file: ", e );
            return;
        }

        if (this.settings == null)
            initializeSettings();

        return;
    }

    /**
     * update the license map.
     */
    private synchronized void mapLicenses()
    {
        /* Create a new map of all of the valid licenses */
        Map<String, License> newMap = new HashMap<String, License>();
        LinkedList<License> newList = new LinkedList<License>();

        for (License lic : this.settings.getLicenses()) {

            /**
             * Create a duplicate - we're about to fill in metadata
             * But we don't want to mess with the original
             */
            License license = new License(lic);

            /**
             * Complete Meta-data
             */
            if (isLicenseValid(license)) {
                license.setValid(Boolean.TRUE);
            } else {
                license.setValid(Boolean.FALSE);
            }
            
            String identifier = license.getName();
            License current = newMap.get(identifier);

            /* current license is newer and better */
            if ((current != null) && (current.getEnd() > license.getEnd()))
                continue;

            newMap.put(identifier, license);
            newList.add(license);
        }

        this.licenseMap = Collections.unmodifiableMap(newMap);
        this.licenseList = Collections.unmodifiableList(newList);
    }

    @SuppressWarnings("fallthrough")
    private boolean isLicenseValid(License license)
    {
        int version = license.getKeyVersion();

        long now = System.currentTimeMillis();

        /* Verify the key hasn't already hasn't expired */
        if (license.getStart() > now) {
            logger.warn( "The license: " + license + " isn't valid yet." );
            return false;
        }

        if ((license.getEnd() < now)) {
            logger.warn( "The license: " + license + " has already expired." );
            return false;
        }

        /* Verify the duration lines up properly */
        long duration = license.getEnd() - license.getStart();

//         if (LicenseType.SUBSCRIPTION.equals(licenseType)) {
//             duration -= LICENSE_SUBSCRIPTION_EXTENSION;
//         }

//         switch (license.getType()) {
//         case "Trial":
//             /* fallthrough */
//         case "Trial14":
//             /* special case for the old license key duration */
//             if (duration == OLD_TRIAL_KEY_DURATION)
//                 break;
//             /* fallthrough */
//         default:
//             // XXX
//             // since there is no a single fucking line of documentation
//             // this will have to do until 8.1
//             // XXX
//             //if (licenseType.getDuration() != duration) {
//             //    logger.warn("bad duration: " + licenseType.getDuration() + " != " + duration);
//             //    return false;
//             //}
//         }

        switch (version) {
        case 1:
            // XXX
            // since there is no a single fucking line of documentation
            // this will have to do until 8.1
            // XXX
            return true;
            //             try {
            //                 String expected = createSelfSignedLicenseKey(license);
            //                 if (!expected.equals(license.getKey())) {
            //                     logger.debug( "Invalid license key for " +
            //                     // license );
            //                     logger.debug( "expected " + expected );
            //                     logger.debug( "key " + license.getKey());
            //                     return false;
            //                 }

            //                 return true;
            //             } catch (NoSuchAlgorithmException e) {
            //                 /* perhaps this should just return true for safety */
            //                 return false;
            //             }
        default:
            logger.warn( "Unknown key version: " + version );
            return false;
        }
    }

    private String toHex(byte data[])
    {
        String response = "";
        for (byte b : data) {
            int c = b;
            if (c < 0)
                c = c + 0x100;
            response += String.format("%02x", c);
        }

        return response;
    }

    private String timeRemaining(long now, long expiration)
    {
        if (now > expiration)
            return EXPIRED;

        /* Calculate the number of days remaining */
        long days = (expiration - now) / (60 * 60 * 24);

        switch ((int) days) {
        case 0:
            return "expires today";

        case 1:
            return "1 day remains";

        default:
            return "" + days + " days remain";
        }
    }

    private void scheduleAndWait()
    {
        /*
         * update all of the licenses for products, run all of them in the timer
         * task to avoid synchronization issues.
         */
        if (!this.pulse.beat(4000)) {
            logger.debug("unable to wait for the license task to complete.");
        }
    }

    /**
     * This starts the task beating at a fixed rate
     */
    private void scheduleFixedTask()
    {
        this.pulse.start(this.TIMER_DELAY);
    }

    /* statics */
    public static LicenseManager getInstance()
    {
        return INSTANCE;
    }

    /**
     * Set the current settings to new Settings
     * Also save the settings to disk if save is true
     */
    private void _saveSettings(LicenseSettings newSettings)
    {
        /**
         * Save the settings
         */
        SettingsManager settingsManager = LocalUvmContextFactory.context().settingsManager();
        try {
            settingsManager.saveBasePath(LicenseSettings.class, System.getProperty("uvm.conf.dir"), "licenses", "licenses", newSettings);
        } catch (SettingsManager.SettingsException e) {
            logger.warn("Failed to save settings.",e);
        }

        /**
         * Change current settings
         */
        this.settings = newSettings;
        try {logger.info("New Settings: \n" + new org.json.JSONObject(this.settings).toString(2));} catch (Exception e) {}
    }
    
    private class LicenseUpdateTask implements Runnable
    {
        public void run() {
            logger.debug("testing licenses and updating products." );

            synchronized (LicenseManagerImpl.this) {
                readLicenses();
                mapLicenses();
            }

            synchronized (this) {
                /* notify any threads that are waiting on this */
                notifyAll();
            }
        }
    }

    static {
        /* Add the license to the map */
//         LICENSE_MAP.put(LicenseType.TRIAL.getName(), LicenseType.TRIAL);
//         LICENSE_MAP.put(LicenseType.TRIAL14.getName(), LicenseType.TRIAL14);
//         LICENSE_MAP.put(LicenseType.SUBSCRIPTION.getName(), LicenseType.SUBSCRIPTION);
//         LICENSE_MAP.put(LicenseType.DEVELOPMENT.getName(), LicenseType.DEVELOPMENT);

        INSTANCE = new LicenseManagerImpl();

        /*
         * This will actually run it twice, but the second call is blocking
         * until the licenses have been loaded
         */
        INSTANCE.scheduleFixedTask();
        INSTANCE.reloadLicenses();
    }
}
