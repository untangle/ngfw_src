/**
 * $Id$
 */
package com.untangle.app.clam;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.SocketException;
import java.nio.channels.ClosedByInterruptException;
import java.util.StringTokenizer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.untangle.app.virus_blocker.VirusClient;
import com.untangle.app.virus_blocker.VirusClientContext;
import com.untangle.app.virus_blocker.VirusClientSocket;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;

/**
 * ClamClient is a client scanner implementation for clamd
 */
public class ClamClient extends VirusClient
{
    protected final Logger clogger = LogManager.getLogger(getClass());

    private final static String CRLF = "\r\n"; // end-of-line
    private final static String LF = "\n";
    private final static String LWSPO = "(\\p{Blank})+"; // linear-white-spaces
    private final static String LWSPA = "(\\p{Blank})*"; // any lwsp
    private final static String DGT = "(\\p{Digit})+"; // digits -> integer
    private final static String ALPHA = "(\\p{Alpha})+"; // alpha chars

    private final static String RESULT_OK = "OK";
    private final static String RESULT_FOUND = "FOUND";

    // must have at least these many parameters
    private final static int CLAMD_RESPONSE_PARAM_CNT = 2;
    private final static int CLAMD_RESULT_PARAM_CNT = 2;

    // client command for main clam client connection
    private final static String CMD_STREAM = "nINSTREAM" +LF;
    // daemon replies from main clam client connection
    private final static String REPLY_PORT = "^PORT" + LWSPO + DGT;
    private final static String REP_RESULT = "^stream:" + LWSPA + ALPHA;
    private final static Pattern REPLY_PORTP = Pattern.compile(REPLY_PORT, Pattern.CASE_INSENSITIVE);
    private final static Pattern REP_RESULTP = Pattern.compile(REP_RESULT, Pattern.CASE_INSENSITIVE);

    /**
     * Create a ClamClient
     * @param cContext - a VirusClientContext
     */
    public ClamClient( VirusClientContext cContext )
    {
        super(cContext);
    }

    /**
     * Run a scan
     */
    public void run()
    {
        VirusClientSocket clamcSocket = null;
        BufferedInputStream bis = null;
        OutputStream os = null;
        FileInputStream fis = null;
        InputStream is = null;

        try {
            clamcSocket = VirusClientSocket.create(cContext.getHost(), cContext.getPort());
            os = clamcSocket.getOutputStream();
            is = clamcSocket.getInputStream();
            fis = new FileInputStream(cContext.getMsgFile());
            bis = new BufferedInputStream(fis) ;
                // Send the nINSTREAM command to start the scan
                os.write(CMD_STREAM.getBytes());

                // Read the file in chunks and send to ClamAV
                byte[] buffer = new byte[8192]; // 8 KB buffer
                int bytesRead;

                while ((bytesRead = bis.read(buffer)) != -1) {
                    // Send the length of the data chunk
                    byte[] chunkLength = new byte[4];
                    chunkLength[0] = (byte) (bytesRead >> 24);
                    chunkLength[1] = (byte) (bytesRead >> 16);
                    chunkLength[2] = (byte) (bytesRead >> 8);
                    chunkLength[3] = (byte) (bytesRead);

                    os.write(chunkLength);
                    os.write(buffer, 0, bytesRead);
                }

                // Send end-of-stream marker (\x00\x00\x00\x00)
                os.write(new byte[]{0, 0, 0, 0});

                // Read the response from ClamAV
                byte[] responseBuffer = new byte[1024];
                int responseBytesRead = is.read(responseBuffer);
                String response = new String(responseBuffer, 0, responseBytesRead);
                Matcher clamdMatcher = REP_RESULTP.matcher(response);
                if (false == clamdMatcher.find())
                    throw new Exception(dbgName + ", clamd result is invalid: " + response);

                parseClamdResult(response);
        } catch (FileNotFoundException e) {
            cContext.setResultError();
            clogger.warn(dbgName + ", finish, file not present (" + cContext.getHost() + ":" + cContext.getPort() + ")", e);
        } catch (ClosedByInterruptException e) {
            // not thrown
            cContext.setResultError();
            clogger.warn(dbgName + ", clamc i/o channel interrupted:" + clamcSocket , e);
        } catch (SocketException e) {
            // thrown during read block
            cContext.setResultError();
            clogger.warn(dbgName + ", clamc socket closed/interrupted: " +clamcSocket , e);
        } catch (IOException e) {
            // not thrown
            cContext.setResultError();
            clogger.warn(dbgName + ", clamc i/o exception: " + clamcSocket , e);
        } catch (InterruptedException e) {
            // not thrown
            cContext.setResultError();
            clogger.warn(dbgName + ", clamc interrupted: " + clamcSocket,  e);
        } catch (Exception e) {
            // thrown during parse
            cContext.setResultError();
            clogger.warn(dbgName + ", clamc failed", e);
        } finally {
            try {
                if (bis != null) bis.close();
            } catch (IOException e) {
                clogger.warn(dbgName + "Failed to close Bufferedinputstream", e);
            }
            try {
                if (fis != null) fis.close();
            } catch (IOException e) {
                clogger.warn(dbgName + "Failed to close Fileinputstream", e);
            }
            try {
                if (os != null) os.close();
            } catch (IOException e) {
                clogger.warn(dbgName + "Failed to close outputstream", e);
            }
            try {
                if (is != null) is.close();
            } catch (IOException e) {
                clogger.warn(dbgName + "Failed to close input tream", e);
            }

            cleanExit(clamcSocket, cContext.getHost(), cContext.getPort());

        }
    }

    /**
     * Cleanup tho ClamClient
     * @param cSocket
     * @param host
     * @param port
     */
    private void cleanup( VirusClientSocket cSocket, String host, int port )
    {
        try {
            if (null != cSocket) {
                // close socket and its open streams
                //clogger.debug(dbgName + ", close: " + cSocket);
                cSocket.close(host, port);
            }
        } catch (Exception e) {
            // if socket and streams fail to close, nothing can be done
        }

        return;
    }

    /**
     * Exit and cleanup the ClamClient
     * @param cSocket
     * @param host
     * @param port
     */
    private void cleanExit( VirusClientSocket cSocket, String host, int port )
    {
        cleanup(cSocket, host, port);
        cleanExit();
        return;
    }

    /**
     * Parse the ClamD Response
     * @param response
     * @return - the port from the response
     * @throws Exception
     */
    private int parseClamdResponse( String response ) throws Exception
    {
        StringTokenizer sTokenizer = new StringTokenizer(response);
        int msgStreamPort = 0;
        int tIdx = 0;

        String tStr;

        while (true == sTokenizer.hasMoreTokens()) {
            tStr = sTokenizer.nextToken();
            switch(tIdx) {
            case 0:
                break; // skip PORT tag
            case 1:
                msgStreamPort = Integer.parseInt(tStr);
                break;
            default:
                clogger.warn(dbgName + ", clamd response has extra parameter: " + tStr);
                // continue because clamc doesn't care
                break;
            }
            tIdx++;
        }

        sTokenizer = null;

        if (CLAMD_RESPONSE_PARAM_CNT > tIdx)
            throw new Exception(dbgName + ", clamd response has less than " + CLAMD_RESPONSE_PARAM_CNT + " parameters");

        return msgStreamPort;
    }

    /**
     * Parse a clamdresult
     * Sets the result in the context
     * @param result
     * @throws Exception
     */
    private void parseClamdResult( String result ) throws Exception
    {
        StringTokenizer sTokenizer = new StringTokenizer(result);
        StringBuilder virusName = new StringBuilder();
        int tIdx = 0;
        boolean clean = true;

        String tStr;

        while (true == sTokenizer.hasMoreTokens()) {
            tStr = sTokenizer.nextToken();
            switch(tIdx) {
            case 0:
                break; // skip stream: tag
            case 1:
                if (false == RESULT_OK.equalsIgnoreCase(tStr)) {
                    virusName.append(tStr);
                    clean = false;
                }
                break;
            case 2:
                if (false == RESULT_FOUND.equalsIgnoreCase(tStr)) {
                    if (true == result.toUpperCase().endsWith(RESULT_FOUND)) {
                        clogger.warn(dbgName + ", clamd changed format of result: " + result);
                        virusName.append(" ").append(tStr);
                    } else {
                        clogger.warn(dbgName + ", clamd reported error result: " + result);
                        cContext.setResultError();
                        return;
                    }
                }
                break;
            default:
                if (false == RESULT_FOUND.equalsIgnoreCase(tStr)) {
                    virusName.append(" ").append(tStr);
                }
                break;
            }
            tIdx++;
        }

        sTokenizer = null;

        if (CLAMD_RESULT_PARAM_CNT > tIdx)
            throw new Exception(dbgName + ", clamd response has less than " + CLAMD_RESULT_PARAM_CNT + " parameters");

        cContext.setResult( clean, virusName.toString() );
        return;
    }
}
