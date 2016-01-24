/**
 * $Id: DeviceTableImpl.java,v 1.00 2016/01/01 11:11:24 dmorris Exp $
 */
package com.untangle.uvm;

import java.util.Map;
import java.util.HashMap;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.log4j.Logger;

import com.untangle.uvm.UvmContextFactory;
import com.untangle.uvm.util.Pulse;

/**
 * DevicTable stores a known "devices" (MAC addresses) that have ever been seen.
 * This table is useful for storing information know about the various devices
 *
 * Other Documentation in DeviceTable.java
 */
public class DeviceTableImpl implements DeviceTable
{
    private static final int HIGH_WATER_SIZE = 12000; /* absolute max */
    private static final int LOW_WATER_SIZE = 10000; /* max size to reduce to when pruning map */

    private static final int PERIODIC_SAVE_DELAY = 1000 * 60 * 60 * 6; /* 6 hours */
    
    private static final String DEVICES_SAVE_FILENAME = System.getProperty("uvm.settings.dir") + "/untangle-vm/devices.js";
    private static final Logger logger = Logger.getLogger(DeviceTableImpl.class);

    private ConcurrentHashMap<String, DeviceTableEntry> deviceTable;
    private HashMap<String,String> macVendorTable = new HashMap<String,String>();

    private final Pulse saverPulse = new Pulse("device-table-saver", true, new DeviceTableSaver());
    
    private volatile long lastSaveTime = 0;
    
    protected DeviceTableImpl()
    {
        initializeMacVendorTable();
        
        this.lastSaveTime = System.currentTimeMillis();
        loadSavedDevices();

        saverPulse.start( PERIODIC_SAVE_DELAY );
    }

    public int size()
    {
        return deviceTable.size();
    }
    
    public Map<String, DeviceTableEntry> getDeviceTable()
    {
        return deviceTable;
    }

    public LinkedList<DeviceTableEntry> getDevices()
    {
        LinkedList<DeviceTableEntry> list = new LinkedList<DeviceTableEntry>( deviceTable.values() );
        return list;
    }

    public void setDevices( LinkedList<DeviceTableEntry> devices )
    {
        logger.info("Saving new device list (" + devices.size() + " entries)");
        this.deviceTable = new ConcurrentHashMap<String,DeviceTableEntry>();

        if ( devices != null ) {
            for ( DeviceTableEntry entry : devices ) {
                deviceTable.put( entry.getMacAddress(), entry );
            }
        }
        
    }
    
    public DeviceTableEntry getDevice( String macAddress )
    {
        return deviceTable.get( macAddress );
    }

    public synchronized DeviceTableEntry addDevice( String macAddress )
    {
        DeviceTableEntry newEntry = getDevice( macAddress );
        if ( newEntry != null ) {
            return newEntry;
        }

        try {
            logger.info("Discovered new device: " + macAddress);
            newEntry = new DeviceTableEntry( macAddress );
            newEntry.enableLogging();

            newEntry.updateLastSeenTime();
            
            deviceTable.put( macAddress, newEntry );

            String macVendor = UvmContextFactory.context().deviceTable().lookupMacVendor( macAddress );
            if ( macVendor != null && !("".equals(macVendor)) )
                newEntry.setMacVendor( macVendor );
            
            DeviceTableEvent event = new DeviceTableEvent( newEntry, macAddress, "add", null );
            UvmContextFactory.context().logEvent(event);

            saveDevices();

            return newEntry;
        }
        catch (Exception e) {
            logger.warn("Failed to add new device: " + macAddress, e);
        }
        
        return null;
    }
    
    public String lookupMacVendor( String macAddress )
    {
        if ( macAddress == null )
            return null;

        String macPrefix = macAddress.substring( 0, 8 );
        return macVendorTable.get( macPrefix );
    }

    @SuppressWarnings("unchecked")
    public void saveDevices()
    {
        lastSaveTime = System.currentTimeMillis();
        
        try {
            Collection<DeviceTableEntry> entries = deviceTable.values();
            logger.info("Saving devices to file... (" + entries.size() + " entries)");

            LinkedList<DeviceTableEntry> list = new LinkedList<DeviceTableEntry>();
            for ( DeviceTableEntry entry : entries ) { list.add(entry); }

            if (list.size() > HIGH_WATER_SIZE) {
                logger.info("Device list over max size, pruning oldest entries"); // remove entries with oldest (lowest) lastSeenTime
                Collections.sort( list, new Comparator<DeviceTableEntry>() { public int compare(DeviceTableEntry o1, DeviceTableEntry o2) {
                    if ( o1.getLastSeenTime() < o2.getLastSeenTime() ) return 1;
                    if ( o1.getLastSeenTime() == o2.getLastSeenTime() ) return 0;
                    return -1;
                } });
                while ( list.size() > LOW_WATER_SIZE ) list.removeLast();
            }
            
            UvmContextFactory.context().settingsManager().save( DEVICES_SAVE_FILENAME, list, false, false );
            logger.info("Saving devices to file... done");
        } catch (Exception e) {
            logger.warn("Exception",e);
        }
    }

    @SuppressWarnings("unchecked")
    private void loadSavedDevices()
    {
        try {
            this.deviceTable = new ConcurrentHashMap<String,DeviceTableEntry>();

            logger.info("Loading devices from file...");
            LinkedList<DeviceTableEntry> savedEntries = UvmContextFactory.context().settingsManager().load( LinkedList.class, DEVICES_SAVE_FILENAME );
            if ( savedEntries == null ) {
                logger.info("Loaded  devices from file.   (no devices saved)");
            } else {
                for ( DeviceTableEntry entry : savedEntries ) {
                    try {
                        // if its invalid just ignore it
                        if ( entry.getMacAddress() == null ) {
                            logger.warn("Invalid entry: " + entry.toJSONString());
                            continue;
                        }

                        entry.enableLogging(); //enable logging now that the object has been built

                        /**
                         * If we don't know the MAC vendor, do a lookup in case we know it now with an updated DB
                         */
                        if (entry.getMacVendor() == null || entry.getMacVendor().equals("")) {
                            String macVendor = UvmContextFactory.context().deviceTable().lookupMacVendor( entry.getMacAddress() );
                            if ( macVendor != null && !("".equals(macVendor)) )
                                entry.setMacVendor( macVendor );
                        }

                        deviceTable.put( entry.getMacAddress(), entry );
                    } catch ( Exception e ) {
                        logger.warn( "Error loading device entry: " + entry, e);
                    }
                }
                logger.info("Loaded  devices from file.   (" + savedEntries.size() + " entries)");
            }
        } catch (Exception e) {
            logger.warn("Failed to load devices",e);
        }
    }

    private void initializeMacVendorTable()
    {
        this.macVendorTable = new HashMap<String,String>();

        Runnable task = new Runnable()
        {
            public void run()
            {
                String filename = System.getProperty("uvm.lib.dir") + "/untangle-vm/oui-formatted.txt";

                long t0 = System.currentTimeMillis();

                java.io.BufferedReader br = null;
                try {
                    br = new java.io.BufferedReader(new java.io.FileReader(filename));
                    for (String line = br.readLine(); line != null ; line = br.readLine()) {
                        String[] parts = line.split("\\s+",2);
                        if ( parts.length < 2 )
                            continue;

                        String macPrefix = parts[0];
                        String vendor = parts[1];
                        //logger.debug( macPrefix + " ---> " + vendor );

                        macVendorTable.put( macPrefix, vendor );
                    }
                } catch (Exception e) {
                    logger.warn("Failed to load MAC OUI data.", e);
                } finally {
                    if ( br != null ) {
                        try {br.close();} catch(Exception e) {}
                    }
                }

                long t1 = System.currentTimeMillis();
                logger.info("Loaded MAC OUI table: " + (t1 - t0) + " millis");
            }
        };
        Thread t = new Thread(task, "MAC-oui-loader");
        t.setDaemon(true);
        t.start();
        return;
    }

    private class DeviceTableSaver implements Runnable
    {
        public void run()
        {
            saveDevices();
        }
    }
    
}
