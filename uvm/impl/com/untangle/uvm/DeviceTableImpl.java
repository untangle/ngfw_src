/**
 * $Id: DeviceTableImpl.java,v 1.00 2016/01/01 11:11:24 dmorris Exp $
 */

package com.untangle.uvm;

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

import org.apache.log4j.Logger;
import org.json.JSONObject;

import com.untangle.uvm.UvmContextFactory;
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
    private static final String CLOUD_LOOKUP_URL = "https://labs.untangle.com/Utility/v1/mac";
    private static final String CLOUD_LOOKUP_KEY = "B132C885-962B-4D63-8B2F-441B7A43CD93";
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

    private static final Logger logger = Logger.getLogger(DeviceTableImpl.class);

    private ConcurrentHashMap<String, DeviceTableEntry> deviceTable;

    private final Pulse saverPulse = new Pulse("device-table-saver", new DeviceTableSaver(), PERIODIC_SAVE_DELAY);

    private volatile long lastSaveTime = 0;

    /**
     * Constructor
     */
    protected DeviceTableImpl()
    {
        loadSavedDevices();

        saverPulse.start();
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
     * Get the list of devices
     * 
     * @return The list of devices
     */
    public synchronized LinkedList<DeviceTableEntry> getDevices()
    {
        LinkedList<DeviceTableEntry> list = new LinkedList<>(deviceTable.values());
        return list;
    }

    /**
     * Set the list of devices
     * 
     * @param newDevices
     *        The list of devices
     */
    public synchronized void setDevices(LinkedList<DeviceTableEntry> newDevices)
    {
        ConcurrentHashMap<String, DeviceTableEntry> oldDeviceTable = this.deviceTable;
        this.deviceTable = new ConcurrentHashMap<>();

        /**
         * For each entry, copy the value on top of the exitsing objects so
         * references are maintained If there aren't in the table, create new
         * entries
         */
        for (DeviceTableEntry entry : newDevices) {
            String macAddress = entry.getMacAddress();
            if (macAddress == null) continue;

            DeviceTableEntry existingEntry = oldDeviceTable.get(macAddress);
            if (existingEntry != null) {
                existingEntry.copy(entry);
                this.deviceTable.put(existingEntry.getMacAddress(), existingEntry);
            } else {
                addDevice(entry);
                entry.enableLogging();
                this.deviceTable.put(entry.getMacAddress(), entry);
            }
        }

        saverPulse.forceRun(0);
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
     * Lookup the hardware vendor for a MAC address
     * 
     * @param macAddress
     *        The MAC address
     * @return The hardware vendor, or null
     */
    public String lookupMacVendor(String macAddress)
    {
        logger.warn("lookupMacVendor:" + macAddress);
        if (macAddress == null) return null;

        try {
            String body = "[\n\"" + macAddress + "\"\n]\n";
            logger.info("Cloud MAC lookup = " + macAddress);

            URL myurl = new URL(UvmContextFactory.context().uriManager().getUri(CLOUD_LOOKUP_URL));
            HttpURLConnection mycon;
            if(myurl.getProtocol().equals("https")){
                mycon = (HttpsURLConnection) myurl.openConnection();
            }else{
                mycon = (HttpURLConnection) myurl.openConnection();
            }
            mycon.setRequestMethod("POST");

            mycon.setConnectTimeout(5000); // 5 seconds
            mycon.setReadTimeout(5000); // 5 seconds
            mycon.setRequestProperty("Content-length", String.valueOf(body.length()));
            mycon.setRequestProperty("Content-Type", "application/json");
            mycon.setRequestProperty("User-Agent", "Untangle NGFW Device Table");
            mycon.setRequestProperty("AuthRequest", CLOUD_LOOKUP_KEY);
            mycon.setDoOutput(true);
            mycon.setDoInput(true);

            DataOutputStream output = new DataOutputStream(mycon.getOutputStream());
            output.writeBytes(body);
            output.close();

            DataInputStream input = new DataInputStream(mycon.getInputStream());
            StringBuilder builder = new StringBuilder(256);

            for (int c = input.read(); c != -1; c = input.read()) {
                if ((char) c == '[') continue;
                if ((char) c == ']') continue;
                builder.append((char) c);
            }

            input.close();
            mycon.disconnect();

            String cloudString = builder.toString();
            if ((cloudString.indexOf('{') < 0) || (cloudString.indexOf('}') < 0)) cloudString = "{}";
            logger.info("Cloud MAC reply = CODE:" + mycon.getResponseCode() + " MSG:" + mycon.getResponseMessage() + " DATA: + " + cloudString);

            JSONObject cloudObject = new JSONObject(cloudString);
            if (cloudObject.has("Organization")) return (cloudObject.getString("Organization"));
        }

        catch (Exception exn) {
            logger.warn("Exception looking up MAC address vendor:", exn);
        }

        return (null);
    }

    /**
     * Save the device table
     */
    @SuppressWarnings("unchecked")
    public void saveDevices()
    {
        /**
         * If we just recently saved, within 60 seconds do not save again
         */
        if (System.currentTimeMillis() - lastSaveTime < (60 * 1000)) {
            logger.info("Saved recently, skipping...");
            return;
        }
        lastSaveTime = System.currentTimeMillis();

        /**
         * If this is the first time we're saving. Lookup any unknown MAC
         * vendors We only do this once so we don't flood the cloud server
         */
        if (lastSaveTime == 0) {
            for (DeviceTableEntry entry : deviceTable.values()) {
                /**
                 * If we don't know the MAC vendor, do a lookup in case we know
                 * it now with an updated DB
                 */
                if (entry.getMacVendor() == null || entry.getMacVendor().equals("")) {
                    String macVendor = lookupMacVendor(entry.getMacAddress());
                    if (macVendor != null && !("".equals(macVendor))) entry.setMacVendor(macVendor);
                }
            }
        }

        try {
            Collection<DeviceTableEntry> entries = deviceTable.values();
            logger.info("Saving devices to file... (" + entries.size() + " entries)");

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

            UvmContextFactory.context().settingsManager().save(DEVICES_SAVE_FILENAME, list, false, true);
            logger.info("Saving devices to file... done");
        } catch (Exception e) {
            logger.warn("Exception", e);
        }
    }

    /**
     * Load the saved devices
     */
    @SuppressWarnings("unchecked")
    private void loadSavedDevices()
    {
        try {
            this.deviceTable = new ConcurrentHashMap<>();

            logger.info("Loading devices from file...");
            LinkedList<DeviceTableEntry> savedEntries = UvmContextFactory.context().settingsManager().load(LinkedList.class, DEVICES_SAVE_FILENAME);
            if (savedEntries == null) {
                logger.info("Loaded  devices from file.   (no devices saved)");
            } else {
                for (DeviceTableEntry entry : savedEntries) {
                    try {
                        // if its invalid just ignore it
                        if (entry.getMacAddress() == null) {
                            logger.warn("Invalid entry: " + entry.toJSONString());
                            continue;
                        }

                        entry.enableLogging(); //enable logging now that the object has been built
                        deviceTable.put(entry.getMacAddress(), entry);
                    } catch (Exception e) {
                        logger.warn("Error loading device entry: " + entry.toJSONString(), e);
                    }
                }
                logger.info("Loaded  devices from file.   (" + savedEntries.size() + " entries)");
            }
        } catch (Exception e) {
            logger.warn("Failed to load devices", e);
        }
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

        String macVendor = UvmContextFactory.context().deviceTable().lookupMacVendor(newEntry.getMacAddress());
        if (macVendor != null && !("".equals(macVendor))) newEntry.setMacVendor(macVendor);

        deviceTable.put(newEntry.getMacAddress(), newEntry);
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
            saveDevices();
        }
    }
}
