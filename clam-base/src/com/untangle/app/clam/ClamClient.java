/**
 * $Id$
 */
package com.untangle.app.clam;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.SocketException;
import java.nio.channels.ClosedByInterruptException;
import java.util.StringTokenizer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.untangle.app.virus_blocker.VirusClient;
import com.untangle.app.virus_blocker.VirusClientContext;
import com.untangle.app.virus_blocker.VirusClientSocket;
import org.apache.log4j.Logger;

/**
 * ClamClient is a client scanner implementation for clamd
 */
public class ClamClient extends VirusClient
{
    protected final Logger clogger = Logger.getLogger(getClass());

    private final static String CRLF = "\r\n"; // end-of-line
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
    private final static String CMD_STREAM = "STREAM" + CRLF;
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
        VirusClientSocket msgcSocket = null;

        try {
            clamcSocket = VirusClientSocket.create(cContext.getHost(), cContext.getPort());
        } catch (Exception e) {
            clogger.warn(dbgName + ", finish, clamc could not connect to main clamd; clamd may not be configured or clamd may be overloaded", e);
            cContext.setResultError();
            cleanExit();
            return;
        }
        //clogger.debug("run, thread: " + cThread + ", this: " + this + ", create: " + clamcSocket);

        try {
            BufferedOutputStream bufOutputStream = clamcSocket.getBufferedOutputStream();
            BufferedReader bufReader = clamcSocket.getBufferedReader();

            if (true == this.stop) {
                clogger.warn(dbgName + ", clamc interrupted post socket streams");
                return; // return after finally
            }

            // send clamc stream cmd (on main socket)
            // STREAM
            byte[] rBuf = CMD_STREAM.getBytes();
            bufOutputStream.write(rBuf, 0, rBuf.length);
            bufOutputStream.flush();

            // receive clamd msg port reply (on main socket)
            // PORT <port no>
            String line;
            if (null == (line = bufReader.readLine()))
                throw new Exception(dbgName + ", clamd/clamc terminated connection early");

            clogger.debug(dbgName + ", " + line); // PORT <port no>
            if (true == this.stop) {
                clogger.warn(dbgName + ", clamc interrupted post clamd port response");
                return; // return after finally
            }

            Matcher clamdMatcher = REPLY_PORTP.matcher(line);
            if (false == clamdMatcher.find())
                throw new Exception(dbgName + ", clamd response is invalid: " + line);

            int msgPort = parseClamdResponse(line);

            try {
                msgcSocket = VirusClientSocket.create(cContext.getHost(), msgPort);
            } catch (Exception e) {
                clogger.warn(dbgName + ", finish, clamc could not connect to msg clamd (" + cContext.getHost() + ":" + msgPort + ")", e);
                cleanExit(clamcSocket, cContext.getHost(), cContext.getPort());
                clamcSocket = null;
                return;
            }

            BufferedOutputStream bufMsgOutputStream = msgcSocket.getBufferedOutputStream();
            try {
                // send message (on msg socket)
                //
                // if msg socket is interrupted (because of a thrown exception),
                // we don't exit until we've checked the main socket
                // - clamd will always have a result to report

                FileInputStream fInputStream = new FileInputStream(cContext.getMsgFile());
                rBuf = new byte[READ_SZ];

                int rLen;
                while (0 < (rLen = fInputStream.read(rBuf))) {
                    bufMsgOutputStream.write(rBuf, 0, rLen);
                    bufMsgOutputStream.flush();
                }
                fInputStream.close();
                fInputStream = null;
                rBuf = null;
            } catch (SocketException e) {
                // thrown during read block
                clogger.warn(dbgName + ", clamc msg socket closed/interrupted: " + clamcSocket + ", " + msgcSocket, e);
                // fall through and check clamd result
            } catch (IOException e) {
                // not thrown
                clogger.warn(dbgName + ", clamc msg i/o exception: " + clamcSocket + ", " + msgcSocket, e);
                // fall through and check clamd result
            } catch (Exception e) {
                clogger.warn(dbgName + ", clamc msg failed", e);
                // fall through and check clamd result
            }

            bufMsgOutputStream = null;

            // signal end of msg by closing msg socket
            cleanup(msgcSocket, cContext.getHost(), msgPort);
            msgcSocket = null;

            // ignore interrupt because one of above exceptions can force
            // clamd to report "premature" result
            // if (true == this.stop) {
            //     clogger.warn(dbgName + ", clamc interrupted post msg stream");
            //     return; // return after finally
            // }

            // receive clamd result (on main socket)
            if (null == (line = bufReader.readLine()))
                throw new Exception(dbgName + ", clamd/clamc terminated connection early (did not report result)");

            clogger.debug(dbgName + ", " + line); // stream: <result>
            // ignore interrupt because one of above exceptions can force
            // clamd to report "premature" result
            // if (true == this.stop) {
            //     clogger.warn(dbgName + ", clamc interrupted post clamd result");
            //     return; // return after finally
            // }

            clamdMatcher = REP_RESULTP.matcher(line);
            if (false == clamdMatcher.find())
                throw new Exception(dbgName + ", clamd result is invalid: " + line);

            parseClamdResult(line);

            bufReader = null;
            bufOutputStream = null;
        } catch (ClosedByInterruptException e) {
            // not thrown
            cContext.setResultError();
            clogger.warn(dbgName + ", clamc i/o channel interrupted:" + clamcSocket + ", " + msgcSocket, e);
        } catch (SocketException e) {
            // thrown during read block
            cContext.setResultError();
            clogger.warn(dbgName + ", clamc socket closed/interrupted: " + clamcSocket + ", " + msgcSocket, e);
        } catch (IOException e) {
            // not thrown
            cContext.setResultError();
            clogger.warn(dbgName + ", clamc i/o exception: " + clamcSocket + ", " + msgcSocket, e);
        } catch (InterruptedException e) {
            // not thrown
            cContext.setResultError();
            clogger.warn(dbgName + ", clamc interrupted: " + clamcSocket + ", " + msgcSocket, e);
        } catch (Exception e) {
            // thrown during parse
            cContext.setResultError();
            clogger.warn(dbgName + ", clamc failed", e);
        } finally {
            //clogger.debug(dbgName + ", finish");
            cleanExit(clamcSocket, cContext.getHost(), cContext.getPort());
            clamcSocket = null;
        }
        
        return;
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
