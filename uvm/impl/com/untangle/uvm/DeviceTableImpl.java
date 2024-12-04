/**
 * $Id: DeviceTableImpl.java,v 1.00 2016/01/01 11:11:24 dmorris Exp $
 */

package com.untangle.uvm;

import java.util.List;
import java.util.Map;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.concurrent.ConcurrentHashMap;

import javax.net.ssl.HttpsURLConnection;
import java.net.HttpURLConnection;
import java.net.URL;
import java.io.DataInputStream;
import java.io.DataOutputStream;

import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;
import org.apache.commons.lang3.StringUtils;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONArray;


import com.untangle.uvm.util.Pulse;

/**
 * DeviceTable stores a known "devices" (MAC addresses) that have ever been
 * seen. This table is useful for storing information know about the various
 * devices
 *
 * Other Documentation in DeviceTable.java
 */
public class DeviceTableImpl implements DeviceTable
{
    private static final String CLOUD_LOOKUP_URL = UvmContextFactory.context().uriManager().getUri("https://labs.untangle.com/Utility/v1/mac");
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
    private static final int PERIODIC_SAVE_DELAY = 1000 * 60 * 60 * 6; /*
                                                                        * 6
                                                                        * hours
                                                                        */
    private static final String DEVICES_SAVE_FILENAME = System.getProperty("uvm.settings.dir") + "/untangle-vm/devices.js";

    private static final Logger logger = LogManager.getLogger(DeviceTableImpl.class);    

    private ConcurrentHashMap<String, DeviceTableEntry> deviceTable;

    private DevicesSettings devicesSettings;

    private final Pulse saverPulse = new Pulse("device-table-saver", new DeviceTableSaver(), PERIODIC_SAVE_DELAY);

    private volatile long lastSaveTime = 0;

    /**
     * Constructor
     */
    protected DeviceTableImpl()
    {
        saverPulse.start();
        loadSavedDevices();
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
     * Get the device table
     *
     * @return The device table
     */
    public Map<String, DeviceTableEntry> getDeviceTable()
    {
        return deviceTable;
    }

    /**
     * Get the devices settings
     * @return {@link DevicesSettings}
     */
    public DevicesSettings getDevicesSettings() {
        LinkedList<DeviceTableEntry> list = new LinkedList<>(deviceTable.values());
        this.devicesSettings.setDevices(list);
        return this.devicesSettings;
    }

    /**
     * Set the deivices settings
     * @param newSettings {@link DevicesSettings}
     */
    public synchronized void setDevicesSettings(DevicesSettings newSettings) {
        if (newSettings.isAutoDeviceRemove() && (newSettings.getAutoRemovalThreshold() < 1 || newSettings.getAutoRemovalThreshold() > 999)) {
            throw new IllegalArgumentException("Invalid auto device removal threshold");
        }
        ConcurrentHashMap<String, DeviceTableEntry> oldDeviceTable = this.deviceTable;
        this.deviceTable = new ConcurrentHashMap<>();

        // For each entry, If it isn't in the table, create new entry. else keep as it is.
        for (DeviceTableEntry entry : newSettings.getDevices()) {
            String macAddress = entry.getMacAddress();
            if (macAddress == null) continue;

            DeviceTableEntry existingEntry = oldDeviceTable.get(macAddress);
            if (existingEntry != null) {
                existingEntry.copy(entry);
                this.deviceTable.put(existingEntry.getMacAddress(), existingEntry);
            } else {
                addDevice(entry);
                entry.enableLogging();
            }
        }
        // Change current settings
        this.devicesSettings = newSettings;

        // Save the settings
        this.saveSettings(false);
    }

    /**
     * 
     * @param lastSaveTimeCheck
     */
    public void saveSettings(boolean lastSaveTimeCheck) {
        // If we just recently saved, within 60 seconds do not save again
        if (lastSaveTimeCheck && System.currentTimeMillis() - lastSaveTime < (60 * 1000)) {
            logger.info("Saved recently, skipping...");
            return;
        }
        lastSaveTime = System.currentTimeMillis();
        try {
            /* If this is the first time we're saving. Lookup any unknown MAC
              vendors We only do this once so we don't flood the cloud server
            */
            if (lastSaveTime == 0)
                populateMacVendor();

            LinkedList<DeviceTableEntry> list = getDevicesList();
            this.devicesSettings.setDevices(list);

            UvmContextFactory.context().settingsManager().save(DEVICES_SAVE_FILENAME, this.devicesSettings, true, true);
            logger.info("Saving devices to file... done");
        } catch (Exception e) {
            logger.error(e);
        }
    }

    /**
     *
     * @return
     */
    private LinkedList<DeviceTableEntry> getDevicesList() {
        Collection<DeviceTableEntry> entries = deviceTable.values();
        logger.info("Saving devices to file... ({} entries)",  entries.size());

        LinkedList<DeviceTableEntry> list = new LinkedList<>();
        for (DeviceTableEntry entry : entries) {
            list.add(entry);
        }

        if (list.size() > HIGH_WATER_SIZE) {
            logger.info("Device list over max size, pruning oldest entries"); // remove entries with oldest (lowest) lastSeenTime
            Collections.sort(list, new Comparator<DeviceTableEntry>()
            {
                /**
                 * Compare function for sorting and finding old entries
                 *
                 * @param o1
                 *        The first entry
                 * @param o2
                 *        The second entry
                 * @return The compare result
                 */
                public int compare(DeviceTableEntry o1, DeviceTableEntry o2)
                {
                    if (o1.getLastSessionTime() < o2.getLastSessionTime()) return 1;
                    if (o1.getLastSessionTime() == o2.getLastSessionTime()) return 0;
                    return -1;
                }
            });
            logger.info("Device table  too large. Removing " + (list.size() - LOW_WATER_SIZE) + " eldest entries.");
            while (list.size() > LOW_WATER_SIZE) {
                DeviceTableEntry entry = list.removeLast();
                this.deviceTable.remove(entry.getMacAddress());
            }
            logger.info("Device table new size: " + this.deviceTable.size());
        }

        return list;
    }

    /**
     *
     * @throws JSONException
     */
    private void populateMacVendor() throws JSONException {
        for (DeviceTableEntry entry : deviceTable.values()) {
            /* If we don't know the MAC vendor, do a lookup in case we know
               it now with an updated DB
             */
            if (null != entry.getMacAddress() && StringUtils.isBlank(entry.getMacVendor())) {
                JSONArray macAddressVendor = lookupMacVendor(entry.getMacAddress());
                if(macAddressVendor.length() > 0) {
                    JSONObject macAddrVendor = macAddressVendor.getJSONObject(0);
                    if (macAddrVendor.has("MAC") && macAddrVendor.has("Organization") && StringUtils.isNotBlank(macAddrVendor.getString("Organization")))
                        entry.setMacVendor(macAddrVendor.getString("Organization"));
                }
            }
        }
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
     * Load the saved devices
     */
    @SuppressWarnings("unchecked")
    private void loadSavedDevices() {
        try {
            this.deviceTable = new ConcurrentHashMap<>();
            DevicesSettings settings = new DevicesSettings();       // Initialise default settings
            boolean writeFlag = false;

            logger.info("Loading devices from file...");
            try {
                settings = UvmContextFactory.context().settingsManager().load(DevicesSettings.class, DEVICES_SAVE_FILENAME);
            } catch (ClassCastException e) {
                writeFlag = true;
                LinkedList<DeviceTableEntry> savedEntries = UvmContextFactory.context().settingsManager().load(LinkedList.class, DEVICES_SAVE_FILENAME);
                convertSettingsToV1(savedEntries, settings);
            } catch (Exception e) {
                logger.error(e);
                // Write Default Settings
                writeFlag = true;
            }

            if (null != settings.getDevices()) {
                for (DeviceTableEntry entry : settings.getDevices()) {
                    // if its invalid just ignore it
                    if (entry.getMacAddress() == null) {
                        logger.warn("Invalid entry: {}", entry.toJSONString());
                        continue;
                    }
                    entry.enableLogging(); //enable logging now that the object has been built
                    this.deviceTable.put(entry.getMacAddress(), entry);
                }
                logger.info("Loaded devices from file. ({} entries)", this.deviceTable.size());
            } else {
                logger.info("Loaded devices from file. (no devices saved)");
            }

            if (writeFlag) {
                this.setDevicesSettings(settings);
            } else {
                this.devicesSettings = settings;
            }
        } catch (Exception e) {
            logger.warn("Failed to load devices ", e);
        }
    }

    /**
     *
     * @param savedEntries
     * @param settings
     * @return
     */
    private void convertSettingsToV1(LinkedList<DeviceTableEntry> savedEntries, DevicesSettings settings) {
        settings.setDevices(savedEntries);
        settings.setVersion(1);
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
                    if (macAddrVendor.has("MAC") && macAddrVendor.has("Organization") && macAddrVendor.getString("Organization") != null && macAddrVendor.getString("Organization") != "") newEntry.setMacVendor(macAddrVendor.getString("Organization"));
                }
            }
            deviceTable.put(newEntry.getMacAddress(), newEntry);
            LinkedList<DeviceTableEntry> list = new LinkedList<>(deviceTable.values());
            devicesSettings.setDevices(list);
        } catch (Exception exn) {
            logger.warn("Exception looking up MAC address vendor:", exn);
        }
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
//            saveDevices();
            saveSettings(true);
        }
    }
}
