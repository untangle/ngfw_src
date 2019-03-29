/**
 * $Id$
 */

package com.untangle.app.spam_blocker_lite;

import java.io.File;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.Socket;
import java.net.SocketException;
import java.nio.channels.ClosedByInterruptException;
import java.util.LinkedList;
import java.util.List;
import java.util.StringTokenizer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.untangle.app.spam_blocker.ReportItem;
import com.untangle.app.spam_blocker.SpamReport;
import org.apache.log4j.Logger;

/**
 * Implementation of a spam asssasin client
 */
public final class SpamAssassinClient implements Runnable
{
    private final File msgFile;
    private final String host;
    private final int port;
    private final float threshold;
    private boolean done = false;
    private volatile SpamReport spamReport;

    private final Logger logger = Logger.getLogger(getClass());

    public final static String SPAMD_DEFHOST = "127.0.0.1"; // default host
    public final static int SPAMD_DEFPORT = 783; // default port

    private final static Pattern REPORT_PATTERN = Pattern.compile("^[ ]*-?[0-9.]+ [A-Z0-9_]+");

    private final static int READ_SZ = 1024;

    private final static String CONTENTLEN = "Content-length:";
    private final static String CRLF = "\r\n"; // end-of-line
    private final static String LWSPO = "(\\p{Blank})+"; // linear-white-spaces
    private final static String LWSPA = "(\\p{Blank})*"; // any lwsp
    private final static String DGT = "(\\p{Digit})+"; // digits -> integer
    private final static String P_NMBR = DGT + "\\." + DGT; // pos number
    private final static String NMBR = "(-)?" + DGT + "\\." + DGT; // pos or neg number

    private final static String ALPHA = "(\\p{Alpha})+"; // alpha chars
    //unused// private final static String EX_CODE = ALPHA + "_" + ALPHA; // exit code

    // must have at least these many parameters
    private final static int SPAMD_RESPONSE_PARAM_CNT = 3;
    private final static int SPAMD_CONTENTLEN_PARAM_CNT = 1;
    private final static int SPAMD_RESULT_PARAM_CNT = 3;

    // spam client hdr - built during run-time
    private final static String REQUEST_CHDR = "REPORT SPAMC/1.3" + CRLF;
    private final static String REQ_USERNAME_TAG = "User: ";
    private final static String REQ_CONTENTLEN_TAG = CONTENTLEN + " ";

    // spam daemon hdr - checked during run-time
    private final static String REPLY_DHDR = "^SPAMD/" + P_NMBR + LWSPO + DGT + LWSPO;
    private final static String REP_CONTENTLEN_DHDR = "^" + CONTENTLEN + LWSPA + DGT;
    private final static String REP_SPAM_DHDR = "^Spam:" + LWSPA + ALPHA + LWSPA + ";" + LWSPA + NMBR + LWSPA + "/" + LWSPA + NMBR;
    private final static Pattern REPLY_DHDRP = Pattern.compile(REPLY_DHDR, Pattern.CASE_INSENSITIVE);
    private final static Pattern REP_CONTENTLEN_DHDRP = Pattern.compile(REP_CONTENTLEN_DHDR, Pattern.CASE_INSENSITIVE);
    private final static Pattern REP_SPAM_DHDRP = Pattern.compile(REP_SPAM_DHDR, Pattern.CASE_INSENSITIVE);

    private final String userNameCHdr;
    private final String contentLenCHdr;

    private Thread cThread;
    private String dbgName; // thread name and socket host
    private volatile boolean stop = false;

    /**
     * Constructor
     * 
     * @param msgFile
     *        The message file
     * @param host
     *        The host
     * @param port
     *        The port
     * @param threshold
     *        The spam threshold
     * @param userName
     *        The username
     */
    public SpamAssassinClient(File msgFile, String host, int port, float threshold, String userName)
    {
        this.msgFile = msgFile;
        this.host = host;
        this.port = port;
        this.threshold = threshold;

        this.userNameCHdr = new StringBuilder(REQ_USERNAME_TAG).append(userName).append(CRLF).toString();
        // add extra CRLF in case message doesn't end with CRLF
        this.contentLenCHdr = new StringBuilder(REQ_CONTENTLEN_TAG).append(Long.toString(msgFile.length() + CRLF.length())).append(CRLF).toString();
    }

    /**
     * Set the thread
     * 
     * @param cThread
     *        The thread
     */
    public void setThread(Thread cThread)
    {
        this.cThread = cThread;
        dbgName = new StringBuilder("<").append(cThread.getName()).append(">").append(host).append(":").append(port).toString();
        return;
    }

    /**
     * Start the scanner thread
     */
    public void startScan()
    {
        //logger.debug("start, thread: " + cThread + ", this: " + this);
        cThread.start(); // execute run() now
        return;
    }

    /**
     * Get the scan result
     * 
     * @return The result
     */
    public SpamReport getResult()
    {
        return spamReport;
    }

    /**
     * Check the scan progress
     * 
     * @param timeout
     *        The timeout
     */
    public void checkProgress(long timeout)
    {
        //logger.debug("check, thread: " + cThread + ", this: " + this);
        if (false == cThread.isAlive()) {
            logger.debug(dbgName + ", is not alive; not waiting");
            return;
        }

        long startTime = System.currentTimeMillis();
        long elapsedTime = 0;
        try {
            synchronized (this) {
                // retry when no result yet and time remains before timeout
                while (!done && elapsedTime < timeout) {
                    this.wait(timeout - elapsedTime);
                    elapsedTime = System.currentTimeMillis() - startTime;
                }
            }
        } catch (InterruptedException e) {
            logger.warn(dbgName + ", spamc interrupted: " + e);
        } catch (Exception e) {
            logger.warn(dbgName + ", spamc failed: " + e);
        }

        if (null == this.spamReport) {
            if (elapsedTime >= timeout) logger.warn(dbgName + ", spamc timer expired (timeout:" + (timeout / 1000) + "s) (elapsed: " + (elapsedTime / 1000) + "s)");
            else logger.warn(dbgName + ", spamc returned no result (timeout:" + (timeout / 1000) + "s) (elapsed: " + (elapsedTime / 1000) + "s)");
            stopScan();
        }

        return;
    }

    /**
     * Stop the scanner thread
     */
    public void stopScan()
    {
        //logger.debug("stop, thread: " + cThread + ", this: " + this);
        if (false == cThread.isAlive()) {
            logger.debug(dbgName + ", is not alive; no need to stop");
            return;
        }

        this.stop = true;
        cThread.interrupt(); // stop run() now
        return;
    }

    /**
     * Get the string representation
     * 
     * @return The string representation
     */
    public String toString()
    {
        return dbgName;
    }

    /**
     * The main thread function
     */
    public void run()
    {
        Socket socket = null;
        BufferedOutputStream bufOutputStream = null;
        BufferedReader bufReader = null;
        long startTime = System.currentTimeMillis();

        try {
            socket = new Socket(host, port);
            bufOutputStream = new BufferedOutputStream(socket.getOutputStream());
            bufReader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        } catch (Exception e) {
            logger.warn(dbgName + ", spamc could not connect to spamd: " + e);
            cleanExit();
            return;
        }

        FileInputStream fInputStream = null;
        try {
            if (this.stop) {
                logger.warn(dbgName + ", spamc interrupted post socket streams");
                return; // return after finally
            }

            // send spamc hdr
            // REPORT SPAMC/1.3
            // User: spamc
            // Content-length: 1235
            // <blank line>
            byte[] rBuf = REQUEST_CHDR.getBytes();
            bufOutputStream.write(rBuf, 0, rBuf.length);
            rBuf = userNameCHdr.getBytes();
            bufOutputStream.write(rBuf, 0, rBuf.length);
            rBuf = contentLenCHdr.getBytes();
            bufOutputStream.write(rBuf, 0, rBuf.length);
            rBuf = CRLF.getBytes(); // end of spamc hdr
            bufOutputStream.write(rBuf, 0, rBuf.length);
            bufOutputStream.flush();

            // send message
            fInputStream = new FileInputStream(this.msgFile);
            rBuf = new byte[READ_SZ];

            int rLen;
            while (0 < (rLen = fInputStream.read(rBuf))) {
                bufOutputStream.write(rBuf, 0, rLen);
            }
            rBuf = CRLF.getBytes(); // add extra CRLF
            bufOutputStream.write(rBuf, 0, rBuf.length);
            bufOutputStream.flush();
            // Can't close the bufOutputStream here or it closes the
            // whole socket.  Instead shutdown.
            socket.shutdownOutput();
            fInputStream.close();
            fInputStream = null;
            rBuf = null;

            if (this.stop) {
                logger.warn(dbgName + ", spamc interrupted post spamc header");
                return; // return after finally
            }

            // receive spamd hdr
            // SPAMD/1.1 0 EX_OK
            // Content-length: 638
            // Spam: True ; 9.5 / 5.0
            // <blank line>
            String line;
            if (null == (line = bufReader.readLine())) throw new Exception(dbgName + ", spamd/spamc terminated connection early");

            logger.debug(dbgName + ", " + line); // SPAMD/<ver> <retcode> <description>
            if (this.stop) {
                logger.warn(dbgName + ", spamc interrupted post spamd header response (elapsed time: " + ((System.currentTimeMillis() - startTime) / 1000) + "s)");
                return; // return after finally
            }

            Matcher spamdMatcher = REPLY_DHDRP.matcher(line);
            if (false == spamdMatcher.find()) throw new Exception(dbgName + ", spamd response is invalid: " + line);

            boolean isOK = parseSpamdResponse(line);

            // receive (and buffer) rest of spamd hdr and result
            List<String> spamdHdrList = new LinkedList<>();
            List<String> spamdDtlList = new LinkedList<>();
            boolean addDetail = false;
            while (!this.stop && (line = bufReader.readLine()) != null) {
                //logger.debug(dbgName + ", " + line);
                if (line.length() == 0) {
                    addDetail = true; // end of spamd hdr (details follow)
                    continue;
                }

                if (addDetail) {
                    spamdDtlList.add(line);
                } else {
                    spamdHdrList.add(line);
                }
            }

            if (this.stop) {
                logger.warn(dbgName + ", spamc interrupted post spamd header and reply");
                return; // return after finally
            }

            if (true == spamdHdrList.isEmpty()) {
                if (true == isOK) {
                    throw new Exception(dbgName + ", spamd terminated connection early (did not report result)");
                } else {
                    // spamd may send more info (in hdr) for some errors
                    // but spamd may send nothing for other errors
                    throw new Exception(dbgName + ", spamd reported protocol error from spamc");
                }
            }
            // hdr and detail list are never empty
            // but unlike hdr list, detail list could be empty
            // (e.g., if message is not spam - 0 score means no detail)
            // even though detail list is currently not empty
            // so we won't check if the detail list is empty

            // process rest of spamd hdr
            Long len = null;
            Float score = null;
            for (String spamdHdr : spamdHdrList) {
                // Content-length: <len>
                spamdMatcher = REP_CONTENTLEN_DHDRP.matcher(spamdHdr);
                if (true == spamdMatcher.find()) {
                    // readLine stripped line terminator chars (LFs)
                    // so "post count" LFs later
                    len = Long.valueOf(parseSpamdContentLength(spamdHdr));
                    continue;
                }

                // Spam: <isspam> ; <score> / <thres>
                // use score but discard isspam and thres
                // (because spamd thres may differ from our threshold)
                spamdMatcher = REP_SPAM_DHDRP.matcher(spamdHdr);
                if (true == spamdMatcher.find()) {
                    score = Float.valueOf(parseSpamdReply(spamdHdr));
                    continue;
                }

                logger.debug(dbgName + ", spamd sent extra header lines: " + spamdHdr);
            }

            if (null == len && null == score) {
                throw new Exception(dbgName + ", spamd did not report content-length and reply");
            } else if (null == len) {
                throw new Exception(dbgName + ", spamd did not report content-length");
            } else if (null == score) {
                throw new Exception(dbgName + ", spamd did not report reply");
            }

            parseSpamdResult(spamdDtlList, len.longValue(), score.floatValue());
            spamdHdrList.clear();
            spamdDtlList.clear();
            spamdHdrList = null;
            spamdDtlList = null;

        } catch (ClosedByInterruptException e) {
            // not thrown
            logger.warn(dbgName + ", spamc i/o channel interrupted: " + socket + ": " + e);
        } catch (SocketException e) {
            // thrown during read block
            logger.warn(dbgName + ", spamc socket closed/interrupted: " + socket + ": " + e);
        } catch (IOException e) {
            // not thrown
            logger.warn(dbgName + ", spamc i/o exception: " + socket + ": " + e);
        } catch (InterruptedException e) {
            // not thrown
            logger.warn(dbgName + ", spamc interrupted: " + socket + ": " + e);
        } catch (Exception e) {
            // thrown during parse
            logger.warn(dbgName + ", spamc failed: " + e);
        } finally {
            logger.debug(dbgName + ", finish");

            if(fInputStream != null){
                try{
                    fInputStream.close();
                }catch(Exception e){
                    logger.error(e);
                }
            }

            if(bufReader != null){
                try {
                    bufReader.close();
                } catch (java.io.IOException e) {
                    logger.error(e);
                }
            }

            if(bufOutputStream != null){
                try {
                    bufOutputStream.close();
                } catch (java.io.IOException e) {
                    logger.error(e);
                }
            }

            if(socket != null){
                try {
                    socket.close();
                } catch (java.io.IOException e) {
                    logger.error(e);
                }
            }

            cleanExit();
        }
        return;
    }

    /**
     * Clean exit
     */
    private void cleanExit()
    {
        synchronized (this) {
            this.done = true;
            this.notifyAll(); // notify waiting thread and finish run()
            return;
        }
    }

    /**
     * Parse the response from the spam scanner
     * 
     * @param response
     *        The response to parse
     * @return True if parsing was successful, otherwise false
     * @throws Exception
     */
    private boolean parseSpamdResponse(String response) throws Exception
    {
        StringTokenizer sTokenizer = new StringTokenizer(response, " \t\n\r\f/");
        String tStr = null;
        int tIdx = 0;
        boolean dumpRest = false;
        boolean isOK = true;
        while (false == dumpRest && true == sTokenizer.hasMoreTokens()) {
            tStr = sTokenizer.nextToken();
            switch (tIdx)
            {
            case 0:
                break; // skip SPAMD tag
            case 1:
                float version = Float.parseFloat(tStr);
                if (1.0 > version) throw new Exception(dbgName + ", spamd response has unsupported version: " + version);

                break;
            case 2:
                int retCode = Integer.parseInt(tStr);
                if (0 != retCode) {
                    logger.warn(dbgName + ", spamd response has non-success return code: " + retCode);
                    isOK = false;
                    // continue if non-success because spamc doesn't care
                    // -> spamd will terminate connection soon
                }

                break;
            default:
            case 3:
                dumpRest = true;
                break;
            }
            tIdx++;
        }

        if (true == dumpRest && (false == isOK || true == logger.isDebugEnabled())) {
            StringBuilder remaining = new StringBuilder(tStr);

            while (true == sTokenizer.hasMoreTokens()) {
                tStr = sTokenizer.nextToken();
                remaining.append(" ").append(tStr);
            }

            if (false == isOK) {
                logger.warn(dbgName + ", spamd response has non-success description: " + remaining);
            } else {
                logger.debug(dbgName + ", spamd response has success description: " + remaining);
            }
            // continue because spamc doesn't care
        }

        sTokenizer = null;

        if (SPAMD_RESPONSE_PARAM_CNT > tIdx) throw new Exception(dbgName + ", spamd response has less than " + SPAMD_RESPONSE_PARAM_CNT + " parameters");

        return isOK;
    }

    /**
     * Parse the content length
     * 
     * @param contentLength
     *        The content information
     * @return The numeric content length
     * @throws Exception
     */
    private long parseSpamdContentLength(String contentLength) throws Exception
    {
        StringTokenizer sTokenizer = new StringTokenizer(contentLength);
        long len = 0;
        int tIdx = 0;

        String tStr;

        while (true == sTokenizer.hasMoreTokens()) {
            tStr = sTokenizer.nextToken();
            switch (tIdx)
            {
            case 0:
                break; // skip Content-Length tag
            case 1:
                len = Long.parseLong(tStr);
                break;
            default:
                logger.warn(dbgName + ", spamd content-length has extra parameter: " + tStr);
                // continue because spamc doesn't care
                break;
            }
            tIdx++;
        }

        sTokenizer = null;

        if (SPAMD_CONTENTLEN_PARAM_CNT > tIdx) throw new Exception(dbgName + ", spamd content-length has less than " + SPAMD_CONTENTLEN_PARAM_CNT + " parameters");

        return len;
    }

    /**
     * Parse the spam score from the scanner reply
     * 
     * @param reply
     *        The scanner reply
     * @return The spam score
     * @throws Exception
     */
    private float parseSpamdReply(String reply) throws Exception
    {
        StringTokenizer sTokenizer = new StringTokenizer(reply, " \t\n\r\f;/");
        int tIdx = 0;
        float score = 0;

        String tStr;

        while (true == sTokenizer.hasMoreTokens()) {
            tStr = sTokenizer.nextToken();
            switch (tIdx)
            {
            case 0:
                break; // skip Spam tag
            case 1:
                break; // skip isspam flag; is identified later
            case 2:
                score = Float.parseFloat(tStr);
                break;
            case 3:
                break; // skip threshold; we already have it
            default:
                logger.warn(dbgName + ", spamd reply has extra parameter: " + tStr);
                // continue because spamc doesn't care
                break;
            }
            tIdx++;
        }

        if (SPAMD_RESULT_PARAM_CNT > tIdx) throw new Exception(dbgName + ", spamd reply has less than " + SPAMD_RESULT_PARAM_CNT + " parameters");

        return score;
    }

    //  2.0 DATE_IN_PAST_96_XX     Date: is 96 hours or more before Received: date
    //  4.3 SUBJ_ILLEGAL_CHARS     Subject: has too many raw illegal characters
    //  0.1 HTML_50_60             BODY: Message is 50% to 60% HTML
    //  0.0 HTML_MESSAGE           BODY: HTML included in message
    //  3.5 BAYES_99               BODY: Bayesian spam probability is 99 to 100%
    //                             [score: 0.9939]
    //  0.0 MIME_HTML_ONLY         BODY: Message only has text/html MIME parts
    //  0.2 HTML_TITLE_EMPTY       BODY: HTML title contains no text
    //  2.2 RCVD_IN_WHOIS_INVALID  RBL: CompleteWhois: sender on invalid IP block
    //                             [61.73.86.111 listed in combined-HIB.dnsiplists.completewhois.com]
    //  1.9 RCVD_IN_NJABL_DUL      RBL: NJABL: dialup sender did non-local SMTP
    //                             [61.73.86.111 listed in combined.njabl.org]
    // -1.8 AWL                    AWL: From: address is in the auto white-list

    /**
     * Parse the result from the scanner
     * 
     * @param detailList
     *        The list of details from the scanner
     * @param len
     *        The length
     * @param score
     *        The score
     * @throws Exception
     */
    private void parseSpamdResult(List<String> detailList, long len, float score) throws Exception
    {
        List<ReportItem> reportItemList = new LinkedList<>();
        String firstLine = null;
        // CR terminates final line (e.g., ends detail block); count CR

        String riScore;
        String riCateg;
        ReportItem reportItem;
        int j;
        int k;

        for (String detail : detailList) {

            if (firstLine == null) firstLine = detail;

            Matcher reportMatcher = REPORT_PATTERN.matcher(detail);
            if (reportMatcher.lookingAt()) {
                detail = detail.trim(); // Trim leading space
                j = detail.indexOf(' ');
                riScore = detail.substring(0, j);
                k = detail.indexOf(' ', j + 1);
                if (k < 0) k = detail.length();
                riCateg = detail.substring(j + 1, k);

                if (logger.isDebugEnabled()) logger.debug(dbgName + ", add item: " + riScore + ", " + riCateg);

                reportItem = new ReportItem(Float.parseFloat(riScore), riCateg);
                reportItemList.add(reportItem);
            }
        }

        this.spamReport = new SpamReport(reportItemList, score, this.threshold);

        // SpamReport creates its own item list and then copies contents
        reportItemList.clear();
        reportItemList = null;
        return;
    }
}
