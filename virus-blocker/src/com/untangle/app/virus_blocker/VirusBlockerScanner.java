/**
 * $Id: VirusBlockerScanner.java 37269 2014-02-26 23:46:16Z dmorris $
 */

package com.untangle.app.virus_blocker;

import java.util.Date;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.io.DataOutputStream;
import java.io.DataInputStream;
import java.io.File;
import org.apache.log4j.Logger;
import com.untangle.uvm.UvmContextFactory;
import com.untangle.app.virus_blocker.VirusScanner;
import com.untangle.app.virus_blocker.VirusScannerResult;
import com.untangle.uvm.vnet.AppSession;

/**
 * The virus blocker scanner
 */
public class VirusBlockerScanner implements VirusScanner
{
    private final Logger logger = Logger.getLogger(getClass());

    private static final int timeout = 29500; /* 29.5 sec */

    private VirusBlockerApp app;

    /**
     * Constructor
     * 
     * @param app
     *        The virus blocker application
     */
    public VirusBlockerScanner(VirusBlockerApp app)
    {
        this.app = app;
    }

    /**
     * Get the vendor name
     * 
     * @return The vendor name
     */
    public String getVendorName()
    {
        return "virus_blocker";
    }

    /**
     * Get the date of the last virus signature update
     * 
     * @return The date of the last virus signature update
     */
    public Date getLastSignatureUpdate()
    {
        DataOutputStream txstream = null;
        DataInputStream rxstream = null;
        Socket socket = null;
        byte buffer[] = new byte[256];
        long timeSeconds = 0;
        int txcount = 0;
        int rxcount = 0;

        // transmit the info request to the daemon and grab the response 
        try {
            InetSocketAddress address = new InetSocketAddress("127.0.0.1", 1344);
            socket = new Socket();
            socket.connect(address, 1000);
            socket.setSoTimeout(1000);
            txstream = new DataOutputStream(socket.getOutputStream());
            rxstream = new DataInputStream(socket.getInputStream());
            txstream.writeBytes("INFO 1\r\n");
            txcount = txstream.size();
            rxcount = rxstream.read(buffer);
        } catch (Exception exn) {
            logger.warn("Unable to query last update.", exn);
            return null;
        }finally{
            if(socket != null){
                try{
                    socket.close();
                }catch(Exception e){
                    logger.warn(e);
                }
            }
            if(txstream != null){
                try{
                    txstream.close();
                }catch(Exception e){
                    logger.warn(e);
                }
            }
            if(rxstream != null){
                try{
                    rxstream.close();
                }catch(Exception e){
                    logger.warn(e);
                }
            }
        }

        // close the streams and socket ignoring exceptions
        try {
            if (txstream != null) txstream.close();
            if (rxstream != null) rxstream.close();
            if (socket != null) socket.close();
        } catch (Exception exn) {
        }

        // REPLY EXAMPLE: 200 1 1407481200 11883905 1402501923
        // REPLY FORMAT: 200 1 xxxxxxxxxx yyyyyyyyyy zzzzzzzzzz
        // 200 = result code which seems to always be 200
        // 1 = block number which seems to always be 1
        // xxxxxxxxxx = license expiration time
        // yyyyyyyyyy = number of database records
        // zzzzzzzzzz = database last update time
        String message = new String(buffer, 0, rxcount).trim();
        if (message.startsWith("200 1 ") == false) {
            logger.warn("Invalid INFO response: " + message);
            return (new Date(0));
        }

        // split the string on the spaces so we can find the update timestamp
        String[] tokens = message.split(" ");
        timeSeconds = Long.parseLong(tokens[4]);
        return (new Date(timeSeconds * 1000l));
    }

    /**
     * Scan a file for a virus
     * 
     * @param scanfile
     *        The file to scan
     * @param session
     *        The application session
     * @return The scan result
     */
    public VirusScannerResult scanFile(File scanfile, AppSession session)
    {

        if (UvmContextFactory.context().licenseManager().isLicenseValid("virus-blocker")) {
            VirusBlockerScannerLauncher scan = new VirusBlockerScannerLauncher(scanfile, session, app.getSettings().getEnableCloudScan(), app.getSettings().getEnableLocalScan());
            return scan.doScan(VirusBlockerScanner.timeout);
        } else if (UvmContextFactory.context().licenseManager().isLicenseValid("virus-blocker-cloud")) {
            VirusBlockerScannerLauncher scan = new VirusBlockerScannerLauncher(scanfile, session, app.getSettings().getEnableCloudScan(), false);
            return scan.doScan(VirusBlockerScanner.timeout);
        }
        return VirusScannerResult.ERROR;
    }
}
