/**
 * $Id: DeviceTableImpl.java,v 1.00 2016/01/01 11:11:24 dmorris Exp $
 */

package com.untangle.uvm;

import com.untangle.uvm.util.Constants;
import com.untangle.uvm.util.Pulse;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONObject;

import javax.net.ssl.HttpsURLConnection;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.LinkedList;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * DeviceTable stores a known "devices" (MAC addresses) that have ever been
 * seen. This table is useful for storing information know about the various
 * devices
 *
 * Other Documentation in DeviceTable.java
 */
public class DeviceTableImpl implements DeviceTable
{
    private static final String CLOUD_LOOKUP_URL = UvmContextFactory.context().uriManager().getUri("https://labs.edge.arista.com/Utility/v1/mac");
    private static final String CLOUD_LOOKUP_KEY = "B132C885-962B-4D63-8B2F-441B7A43CD93";
    private static final String CONTENT_LENGTH = "Content-length";
    private static final String CONTENT_TYPE = "Content-Type";
    private static final String CONTENT_TYPE_VALUE = "application/json";
    private static final String AUTH_REQUEST = "AuthRequest";
    private static final String USER_AGENT = "User-Agent";
    private static final String USER_AGENT_VALUE = "Untangle NGFW Device Table";
    private static final String HTTP_METHOD = "POST";
    private static final int HIGH_WATER_SIZE = 30000; /* absolute max */
    private static final int LOW_WATER_SIZE = 25000; /*
                                                      * max size to reduce to
                                                      * when pruning map
                                                      */
    private static final int PERIODIC_SAVE_DELAY = 1000 * 60 * 60 * 6; /* 6 hours */
    private static final int PERIODIC_REMOVE_DELAY = 1000 * 60 * 60 * 24; /* 24 hours */
    private static final long WAIT_BETWEEN_TWO_SAVES = 60 * 1000; /* 60 sec */

    private static final String DEVICES_SAVE_FILENAME = System.getProperty("uvm.settings.dir") + "/untangle-vm/devices.js";

    private static final Logger logger = LogManager.getLogger(DeviceTableImpl.class);    

    private ConcurrentHashMap<String, DeviceTableEntry> deviceTable;

    private DevicesSettings devicesSettings;

    private static final int MIN_THRESHOLD = 1;
    private static final int MAX_THRESHOLD = 999;
    public static final String ORGANIZATION = "Organization";
    public static final String MAC = "MAC";
    private final Runnable autoDeleteTask = () -> autoDeleteDevices(false);
    private final Pulse autoDeletePulse = new Pulse("auto-remove-devices", new Thread(autoDeleteTask), PERIODIC_REMOVE_DELAY);

    private final Pulse saverPulse = new Pulse("device-table-saver", new DeviceTableSaver(), PERIODIC_SAVE_DELAY);

    private volatile long lastSaveTime = 0;
    /**
     * Constructor
     */
    protected DeviceTableImpl()
    {
        saverPulse.start();
        loadSavedDevices();
        autoDeletePulse.start();
    }

    /**
     * Get the size of the device table
     *
     * @return The size of the device table
     */
    public int size()
    {
        return deviceTable.size();
    }

    /**
     * Get the devices settings
     * @return {@link DevicesSettings}
     */
    public DevicesSettings getDevicesSettings() {
        updateSettingsFromDeviceTable();
        return this.devicesSettings;
    }

    /**
     * Set the deivices settings
     * @param newSettings {@link DevicesSettings}
     */
    public synchronized void setDevicesSettings(DevicesSettings newSettings) {
        if (newSettings.isAutoDeviceRemove() &&
                (newSettings.getAutoRemovalThreshold() < MIN_THRESHOLD || newSettings.getAutoRemovalThreshold() > MAX_THRESHOLD))
            throw new IllegalArgumentException("Invalid auto device removal threshold");

        logger.info("Setting new device settings: {}", newSettings);
        ConcurrentHashMap<String, DeviceTableEntry> oldDeviceTable = deviceTable;
        deviceTable = new ConcurrentHashMap<>();

        // For each entry, If it isn't in the table, create new entry. else keep as it is.
        newSettings.getDevices()
            .stream()
            .filter(entry -> null != entry.getMacAddress())
            .forEach(entry -> {
                DeviceTableEntry existingEntry = oldDeviceTable.get(entry.getMacAddress());
                if (existingEntry != null) {
                    existingEntry.copy(entry);
                    deviceTable.put(existingEntry.getMacAddress(), existingEntry);
                } else {
                    addDevice(entry);
                    entry.enableLogging();
                }
            });
        logger.info("Loaded devices from file. ({} entries)", deviceTable.size());

        // If configs have been changed run devices removal
        if (null != devicesSettings && newSettings.isAutoDeviceRemove()) {
            if (!devicesSettings.isAutoDeviceRemove() || devicesSettings.getAutoRemovalThreshold() != newSettings.getAutoRemovalThreshold()) {
                devicesSettings.setAutoDeviceRemove(true);
                devicesSettings.setAutoRemovalThreshold(newSettings.getAutoRemovalThreshold());
                autoDeleteDevices(true);        // skip save as we will be saving the settings later in this method.
            }
        }

        // Change current settings to new settings
        devicesSettings = newSettings;
        // Save the settings. Skip last save time check
        saveDevicesSettings(false);
    }

    /**
     * Saves the settings to devices.js file
     * Checks if last save time is older than WAIT_BETWEEN_TWO_SAVES
     * if lastSaveTimeCheck is true waits for remaining time else continues
     * @param lastSaveTimeCheck if false, last save time check is skipped
     */
    public synchronized void saveDevicesSettings(boolean lastSaveTimeCheck) {
        // If we just recently saved, within 60 seconds wait.
        long currentTime = System.currentTimeMillis();
        long timeSinceLastSave = currentTime - lastSaveTime;
        long waitTime;
        if (lastSaveTimeCheck && timeSinceLastSave < WAIT_BETWEEN_TWO_SAVES) {
            logger.info("Saved recently, waiting...");
            waitTime = WAIT_BETWEEN_TWO_SAVES - timeSinceLastSave;
            try {
                wait(waitTime);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt(); // Restore interrupted status
            }
        }
        lastSaveTime = System.currentTimeMillis();
        logger.info("lastSaveTime: {}", lastSaveTime);
        try {
            /* If this is the first time we're saving. Lookup any unknown MAC
             * vendors We only do this once so we don't flood the cloud server
            */
            if (lastSaveTime == 0)
                populateMacVendor();

            LinkedList<DeviceTableEntry> list = getDevicesList();
            this.devicesSettings.setDevices(list);

            // Save the devices settings to devices.js file
            logger.info("Saving devices settings to file");
            UvmContextFactory.context().settingsManager().save(DEVICES_SAVE_FILENAME, this.devicesSettings, true, true);
            logger.info("Saving devices to file... done");
        } catch (Exception e) {
            logger.error("Exception while saving the devices settings: ", e);
        }
    }

    /**
     * populates the mac vendor for devices whose mac vendor is blank
     */
    private void populateMacVendor() {
        /* If we don't know the MAC vendor, do a lookup
         * in case we know it now with an updated DB
         */
        deviceTable.values()
            .stream()
            .filter(entry -> null != entry.getMacAddress() && StringUtils.isBlank(entry.getMacVendor()))
            .forEach(entry -> {
                try{
                    JSONArray macAddressVendor = lookupMacVendor(entry.getMacAddress());
                    if(macAddressVendor.length() > 0) {
                        JSONObject macAddrVendor = macAddressVendor.getJSONObject(0);
                        if (macAddrVendor.has(MAC) && macAddrVendor.has(ORGANIZATION)
                                && StringUtils.isNotBlank(macAddrVendor.getString(ORGANIZATION)))
                            entry.setMacVendor(macAddrVendor.getString(ORGANIZATION));
                    }
                } catch (Exception e) {
                    logger.error("Error while fetching mac vendor for address {}", entry.getMacAddress(), e);
                }
            });
    }

    /**
     * If devices size is greater than HIGH_WATER_SIZE remove devices
     * whose last seen is oldest till devices size equals LOW_WATER_SIZE
     * @return LinkedList of {@link DeviceTableEntry}
     */
    private LinkedList<DeviceTableEntry> getDevicesList() {
        logger.info("Filtering devices from device table to save in devices.js file");
        LinkedList<DeviceTableEntry> list = new LinkedList<>(deviceTable.values());

        if (list.size() > HIGH_WATER_SIZE) {
            // remove entries with oldest (lowest) lastSeenTime
            logger.info("Device list over max size, pruning oldest entries");
            list.sort((o1, o2) -> Long.compare(o2.getLastSessionTime(), o1.getLastSessionTime()));

            logger.info("Device table  too large. Removing {} eldest entries.", list.size() - LOW_WATER_SIZE);
            while (list.size() > LOW_WATER_SIZE) {
                DeviceTableEntry entry = list.removeLast();
                deviceTable.remove(entry.getMacAddress());
            }
            logger.info("Device table new size: {}", deviceTable.size());
        }
        return list;
    }

    /**
     * Get a device table entry for a MAC address
     *
     * @param macAddress
     *        The MAC address
     * @return The device table entry, or null if not found
     */
    public DeviceTableEntry getDevice(String macAddress)
    {
        if (macAddress == null) return null;

        return deviceTable.get(macAddress);
    }

    /**
     * Add a device to the table, creating and returning a new entry, or
     * returning the existing entry if found.
     *
     * @param macAddress
     *        The mac address
     * @return The device table entry
     */
    public synchronized DeviceTableEntry addDevice(String macAddress)
    {
        DeviceTableEntry newEntry = getDevice(macAddress);
        if (newEntry != null) {
            return newEntry;
        }

        if ("00:00:00:00:00:00".equals(macAddress)) {
            // Many networks have 00:00:00:00:00:00 devices, so we shouldn't print this
            // because it prints everytime a 00:00:00:00:00:00 device creates a session
            // logger.warn("Ignoring 00:00:00:00:00:00 device.");
            return null;
        }

        try {
            logger.info("Discovered new device: " + macAddress);
            newEntry = new DeviceTableEntry(macAddress);
            newEntry.enableLogging();

            addDevice(newEntry);

            DeviceTableEvent event = new DeviceTableEvent(newEntry, macAddress, "add", null, null);
            UvmContextFactory.context().logEvent(event);

            saverPulse.forceRun(0);

            return newEntry;
        } catch (Exception e) {
            logger.warn("Failed to add new device: " + macAddress, e);
        }

        return null;
    }

    /**
     * Lookup the hardware vendors for the MAC addresses
     *
     * @param macAddresses The MAC address List String
     * @return The hardware vendor for the given mac addresses, or null 
     */
    public JSONArray lookupMacVendor(String macAddresses)
    {
        logger.warn("lookupMacVendor: {}", macAddresses);
        if (StringUtils.isBlank(macAddresses)) return null;
        String[] macAddressList = macAddresses.split(",");
        try {
            if (macAddressList != null) {
                JSONArray macAdressList = new JSONArray(macAddressList);
                logger.info("Cloud MAC lookup: {}", macAdressList);
                
                String body = macAdressList.toString();
                
                // forming the URL
                URL myurl = new URL(UvmContextFactory.context().uriManager().getUri(CLOUD_LOOKUP_URL));
                HttpURLConnection mycon;
                if(myurl.getProtocol().equals("https")){
                        mycon = (HttpsURLConnection) myurl.openConnection();
                }else{
                        mycon = (HttpURLConnection) myurl.openConnection();
                }
                mycon.setRequestMethod(HTTP_METHOD);
                mycon.setConnectTimeout(5000); // 5 seconds
                mycon.setReadTimeout(5000); // 5 seconds
                mycon.setRequestProperty(CONTENT_LENGTH, String.valueOf(body.length()));
                mycon.setRequestProperty(CONTENT_TYPE, CONTENT_TYPE_VALUE);
                mycon.setRequestProperty(USER_AGENT, USER_AGENT_VALUE);
                mycon.setRequestProperty(AUTH_REQUEST, CLOUD_LOOKUP_KEY);
                mycon.setDoOutput(true);
                mycon.setDoInput(true);

                DataOutputStream output = new DataOutputStream(mycon.getOutputStream());
                output.writeBytes(body);
                output.close();

                DataInputStream input = new DataInputStream(mycon.getInputStream());
                StringBuilder builder = new StringBuilder(256);

                for (int c = input.read(); c != -1; c = input.read()) {
                    builder.append((char) c);
                }

                input.close();
                mycon.disconnect();

                String cloudString = builder.toString();
                return new JSONArray(cloudString);
            }
        }

        catch (Exception exn) {
            logger.warn("Exception looking up MAC address vendor:", exn);
        }

        return (null);
    }

    /**
     * Gets the Mac Vendor String value using provided macAddress
     * @param macAddress
     * @return macVendor name
     */
    @Override
    public String getMacVendorFromMacAddress(String macAddress) {
        JSONArray macAddressVendor = lookupMacVendor(macAddress);
        try {
            if (macAddressVendor != null && macAddressVendor.length() > 0) {
                JSONObject macAddrVendor = macAddressVendor.getJSONObject(0);
                if (macAddrVendor.has(MAC) &&
                    macAddrVendor.has(ORGANIZATION) &&
                    StringUtils.isNotBlank(macAddrVendor.getString(ORGANIZATION))) {
                    return macAddrVendor.getString(ORGANIZATION);
                }
            }
        } catch (Exception e) {
            logger.error("Error while fetching mac vendor for address {}", macAddress, e);
        }
        return null;
    }

    /**
     * Load the saved devices
     */
    @SuppressWarnings("unchecked")
    private void loadSavedDevices() {
        try {
            this.deviceTable = new ConcurrentHashMap<>();
            DevicesSettings settings = null;       // Initialise default settings
            boolean writeFlag = false;
            boolean oldSettings = false;

            /* In 17.4 we are updating devices.js structure from LinkedList type to DevicesSettings
             * On upgrade for the first time try block will throw ClassCastException.
             * In that case we need to load settings as LinkedList.
             */
            logger.info("Loading devices from file...");
            try {
                settings = UvmContextFactory.context().settingsManager().load(DevicesSettings.class, DEVICES_SAVE_FILENAME);
            } catch (ClassCastException e) {
                oldSettings = true;
                logger.error("ClassCastException while reading devices settings file: ", e);
            } catch (Exception e) {
                logger.error("Exception while reading devices settings file: ", e);
            }

            if (oldSettings) {
                settings = readOldSettings();
                writeFlag = true;
            }

            if (null == settings) {
                // Fresh Install Write Default Settings
                settings = new DevicesSettings();
                writeFlag = true;
            }

            if (null != settings.getDevices())
                updateDevicesToDeviceTable(settings.getDevices());
            else
                logger.info("Loaded devices from file. (no devices saved)");

            if (writeFlag)
                this.setDevicesSettings(settings);
            else
                this.devicesSettings = settings;

        } catch (Exception e) {
            logger.warn("Failed to load devices settings ", e);
        }
    }

    /**
     * reads old devices settings (List of devices) and sets it to DevicesSettings
     * called once for upgrade/backup restore scenarios
     * @return {@link DevicesSettings}
     */
    @SuppressWarnings("unchecked")
    private DevicesSettings readOldSettings() {
        DevicesSettings settings = new DevicesSettings();;
        LinkedList<DeviceTableEntry> savedEntries = null;
        logger.info("Reading old settings file");
        try {
            savedEntries = UvmContextFactory.context().settingsManager().load(LinkedList.class, DEVICES_SAVE_FILENAME);
        } catch (Exception e) {
            logger.error("Exception while reading old devices settings file: ", e);
        }
        if(null != savedEntries)
            settings.setDevices(savedEntries);
        return settings;
    }


    /**
     * Updates the devices table from read settings devices
     * @param devices LinkedList of {@link DeviceTableEntry}
     */
    private void updateDevicesToDeviceTable(LinkedList<DeviceTableEntry> devices) {
        devices.stream()
            .filter(entry -> null != entry.getMacAddress())
            .forEach(entry -> {
                //enable logging now that the object has been built
                entry.enableLogging();
                this.deviceTable.put(entry.getMacAddress(), entry);
            });
        logger.info("Loaded devices from file. ({} entries)", deviceTable.size());
    }

    /**
     * Add a device table entry to the table
     *
     * @param newEntry
     */
    private void addDevice(DeviceTableEntry newEntry)
    {
        if (newEntry == null) {
            logger.warn("Invalid arguments");
            return;
        }

        newEntry.enableLogging(); // no on by default, make sure its on when going in the table

        try{
            if (newEntry.getMacAddress()!= null) {
                JSONArray macAddressVendor = lookupMacVendor(newEntry.getMacAddress());
                if(macAddressVendor.length() > 0){
                    JSONObject macAddrVendor = macAddressVendor.getJSONObject(0);
                    if (macAddrVendor.has(MAC) && macAddrVendor.has(ORGANIZATION) && StringUtils.isNotBlank(macAddrVendor.getString(ORGANIZATION)))
                        newEntry.setMacVendor(macAddrVendor.getString(ORGANIZATION));
                }
            }
            deviceTable.put(newEntry.getMacAddress(), newEntry);
            // update settings from device table
            updateSettingsFromDeviceTable();
        } catch (Exception exn) {
            logger.warn("Exception looking up MAC address vendor:", exn);
        }
    }

    /**
     * If autoDeviceRemove is configured, checks for devices
     * whose last seen is older than configured threshold
     * saves the settings at end in devices.js file
     * @param skipSave if true skips saving the settings
     */
    public void autoDeleteDevices(boolean skipSave) {
        // return if auto device removal is disabled
        if (null == devicesSettings) return;
        if (!devicesSettings.isAutoDeviceRemove()) return;
        int autoRemovalThreshold = devicesSettings.getAutoRemovalThreshold();
        try {
            // Filter devicesMap to retain only those devices whose lastSeen is within the threshold
            deviceTable = deviceTable.entrySet()
                .stream()
                .filter(entry -> {
                    return excludedFromRemoval(entry, autoRemovalThreshold);
                })
                .collect(Collectors.toConcurrentMap(
                        Map.Entry::getKey,
                        Map.Entry::getValue,
                        (existing, replacement) -> existing,
                        ConcurrentHashMap::new
                ));
            // update settings from device table
            updateSettingsFromDeviceTable();
            // save the devices settings
            if (!skipSave)
                saveDevicesSettings(false);
        } catch (Exception e) {
            logger.error("Exception while removing devices: ", e);
        }
    }

    /**
     * Check if device table entry can be excluded from removal
     * @param entry Map of {@link DeviceTableEntry}
     * @param autoRemovalThreshold last seen threshold in days
     * @return true if entry is to be retained else false
     */
    private static boolean excludedFromRemoval(Map.Entry<String, DeviceTableEntry> entry, int autoRemovalThreshold) {
        if (null == entry.getValue()) return false;
        long currentTime = System.currentTimeMillis();
        long lastSeen = entry.getValue().getLastSessionTime();
        long daysSinceLastSeen = (currentTime - lastSeen) / (24 * 60 * 60 * 1000); // Convert millis to days
        return daysSinceLastSeen < autoRemovalThreshold;
    }

    /**
     * updates devices in settings from device table
     */
    public void updateSettingsFromDeviceTable() {
        LinkedList<DeviceTableEntry> list = new LinkedList<>(deviceTable.values());
        devicesSettings.setDevices(list);
    }

    /**
     * Runnable class to periodically save the device table
     */
    private class DeviceTableSaver implements Runnable
    {
        /**
         * Main runnable function
         */
        public void run()
        {
            saveDevicesSettings(true);
        }
    }
}
