/*
 * $Id: VirusBlockerScannerLauncher.java 37269 2014-02-26 23:46:16Z dmorris $
 */

package com.untangle.node.virus_blocker;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.net.InetSocketAddress;
import java.net.Socket;

import com.untangle.uvm.UvmContextFactory;
import com.untangle.node.virus_blocker.VirusScannerLauncher;
import com.untangle.node.virus_blocker.VirusScannerResult;

public class VirusBlockerScannerLauncher extends VirusScannerLauncher
{
    private static final String SCANNER_HOST = "localhost";
    private static final String SCANNER_PORT = "8088";
    private static final long SCANNER_MAXSIZE = 10485760;
    private static final long SCANNER_MINSIZE = 1;

    /**
     * Create a Launcher for the give file
     */
    public VirusBlockerScannerLauncher(File scanfile)
    {
        super(scanfile);
    }

    /**
     * This runs the virus scan, and stores the result for retrieval. Any
     * threads in waitFor() are awoken so they can retrieve the result
     */
    public void run()
    {
        File scanFile = new File(scanfilePath);
        String virusName = null;

        logger.debug("Scanning file: " + scanfilePath);

        try {
            // Bug #9796 - to avoid large memory usage don't scan large files // XXX
            long scanFileLength = scanFile.length();
            if (scanFileLength > SCANNER_MAXSIZE) {
                logger.debug("Passing large file: " + (scanFile.length() / 1024) + "K");
                setResult(VirusScannerResult.CLEAN);
                return;
            }
            // ignore small or empty files
            if (scanFileLength < SCANNER_MINSIZE) {
                logger.debug("Passing small file: " + scanFile.length() + " bytes");
                setResult(VirusScannerResult.CLEAN);
                return;
            }

        } catch (Exception exn) {
            logger.warn("Exception checking file length: ", exn);
            setResult(VirusScannerResult.ERROR);
            return;
        }

        DataOutputStream txstream = null;
        DataInputStream rxstream = null;
        Socket socket = null;
        byte buffer[] = new byte[256];
        long timeSeconds = 0;
        int txcount = 0;
        int rxcount = 0;

        // Transmit the scan request to the daemon and grab the response
        // Syntax = SCANFILE options filename - available options bits: (see docs for details)
        // 1 = BDAM_SCANOPT_ARCHIVES
        // 2 = BDAM_SCANOPT_PACKED
        // 4 = BDAM_SCANOPT_EMAILS
        // 8 = enable virus heuristics scanner
        // 16 = BDAM_SCANOPT_DISINFECT
        // 32 = return in-progress information
        // 64 = BDAM_SCANOPT_SPAMCHECK
        try {
            InetSocketAddress address = new InetSocketAddress("127.0.0.1", 1344);
            socket = new Socket();
            socket.connect(address, 10000);
            socket.setSoTimeout(10000);
            txstream = new DataOutputStream(socket.getOutputStream());
            rxstream = new DataInputStream(socket.getInputStream());
            txstream.writeBytes("SCANFILE 15 " + scanfilePath + "\r\n");
            txcount = txstream.size();
            rxcount = rxstream.read(buffer);
        } catch (Exception exn) {
            logger.warn("Exception scanning file: ", exn);
            setResult(VirusScannerResult.ERROR);
            return;
        }

        // close the streams and socket ignoring exceptions
        try {
            if (txstream != null) txstream.close();
            if (rxstream != null) rxstream.close();
            if (socket != null) socket.close();
        } catch (Exception exn) {
        }

        // REPLY EXAMPLE: 222 V Trojan.GenericKD.1359402
        // REPLY FORMAT: ccc ttt nnn
        // ccc = result code
        // ttt = malware type (Virus, Spyware, adWare, Dialer, App)
        // nnn = malware name

        String message = new String(buffer, 0, rxcount).trim();
        logger.debug("Scan result: " + message);

        // split the string on the spaces so we can find all the fields
        String[] tokens = message.split(" ");
        int retcode = 0;

        try {
            retcode = Integer.valueOf(tokens[0]);
        } catch (Exception exn) {
            logger.warn("Exception parsing result code: " + tokens[0], exn);
        }

        switch (retcode)
        {
        case 227: // clean
            setResult(VirusScannerResult.CLEAN);
            break;
        case 222: // known infection
            setResult(new VirusScannerResult(false, tokens[2]));
            break;
        case 223: // likely infection
            setResult(new VirusScannerResult(false, tokens[2]));
            break;
        case 225: // password protected file
            setResult(VirusScannerResult.CLEAN);
            break;
        case 221: // scan aborted or failed
            setResult(VirusScannerResult.ERROR);
            break;
        case 224: // corrupted file
            setResult(VirusScannerResult.ERROR);
            break;
        default:
            setResult(VirusScannerResult.ERROR);
            break;
        }
    }

    private void setResult(VirusScannerResult value)
    {
        this.result = value;

        synchronized (this) {
            this.notifyAll();
        }
    }
}
