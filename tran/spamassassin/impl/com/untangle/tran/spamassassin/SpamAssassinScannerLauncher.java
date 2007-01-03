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
package com.untangle.tran.spamassassin;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.util.LinkedList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.untangle.mvvm.MvvmContextFactory;
import com.untangle.tran.spam.ReportItem;
import com.untangle.tran.spam.SpamReport;
import org.apache.log4j.Logger;

public class SpamAssassinScannerLauncher implements Runnable
{
    private static final String REPORT_CMD = "/usr/bin/spamc -u spamc -R -t 39";

    private static final Pattern REPORT_PATTERN =
        Pattern.compile("^[ ]*-?[0-9]+\\.[0-9]+ [A-Z0-9_]+");

    protected final Logger logger = Logger.getLogger(getClass());

    private File f = null;
    private float threshold;

    // These next must be volatile since they are written and read by different threads.  bug948
    protected volatile Process scanProcess = null;
    protected volatile SpamReport result = null;

    /**
     * Create a Launcher for the give file
     */
    public SpamAssassinScannerLauncher(File f, float threshold)
    {
        this.f = f;
        this.threshold = threshold;
    }


    /**
     * Starts the scan and waits for timeout milliseconds for a result
     * If a result is reached, it is returned.
     * If the time expires VirusScannerResult.ERROR is returned
     */
    public SpamReport doScan(int timeout)
    {
        Thread thread = MvvmContextFactory.context().newThread(this);
        long startTime = System.currentTimeMillis();
        try {
            synchronized (this) {
                // Don't start the thread until we have the monitor held.
                thread.start();

                this.wait(timeout);

                // FuckinA, Java can return from wait() spuriously!
                if (this.result == null) {
                    long currentTime = System.currentTimeMillis();
                    while (this.result == null && (currentTime - startTime) < timeout) {
                        this.wait(timeout - (currentTime - startTime));
                        currentTime = System.currentTimeMillis();
                    }
                }
            }
        } catch (InterruptedException e) {
            logger.warn("Spam scan interrupted, killing process, assuming clean");
            this.scanProcess.destroy();
            return cleanReport();
        }

        if (this.result == null) {
            logger.warn("Timer expired, killing process, assuming clean");

            /**
             * This is debugging information for bug 948
             */
            if (this.scanProcess == null) {
                logger.warn("ScannerLauncher Thread Status: " + thread.getState());
                logger.warn("ScannerLauncher Thread isAlive: " + thread.isAlive());
                logger.error("Spam process (" + getClass() + ") failed to launch.");
            } else {
                this.scanProcess.destroy();
            }

            return cleanReport();
        } else {
            return this.result;
        }
    }

    private SpamReport cleanReport()
    {
        return new SpamReport(new LinkedList<ReportItem>(), threshold);
    }



    /**
     * This runs the spam scan, and stores the result for retrieval.
     * Any threads in waitFor() are awoken so they can retrieve the
     * result
     */
    public void run()
    {
        try {
            String command = REPORT_CMD;
            this.scanProcess = MvvmContextFactory.context().exec(command);
            int i;

            try {
                OutputStream os = scanProcess.getOutputStream();
                byte[] buf = new byte[1024];
                FileInputStream fis = new FileInputStream(f);
                int len;
                while ((len = fis.read(buf)) > 0)
                    os.write(buf, 0, len);
                fis.close();
                os.close();

                List<ReportItem> items = new LinkedList<ReportItem>();
                InputStream is  = scanProcess.getInputStream();
                BufferedReader in = new BufferedReader(new InputStreamReader(is));

                String firstLine = null;
                String line;

                /**
                 * Drain spamc output
                 */

                // 12.5/5.0
                //  2.0 DATE_IN_PAST_96_XX     Date: is 96 hours or more before Received: date
                //  4.3 SUBJ_ILLEGAL_CHARS     Subject: has too many raw illegal characters
                //  0.1 HTML_50_60             BODY: Message is 50% to 60% HTML
                //  0.0 HTML_MESSAGE           BODY: HTML included in message
                //  3.5 BAYES_99               BODY: Bayesian spam probability is 99 to 100%
                //                             [score: 0.9939]
                //  0.0 MIME_HTML_ONLY         BODY: Message only has text/html MIME parts
                //  0.2 HTML_TITLE_EMPTY       BODY: HTML title contains no text
                //  2.2 RCVD_IN_WHOIS_INVALID  RBL: CompleteWhois: sender on invalid IP block
                //             [61.73.86.111 listed in combined-HIB.dnsiplists.completewhois.com]
                //  1.9 RCVD_IN_NJABL_DUL      RBL: NJABL: dialup sender did non-local SMTP
                //                             [61.73.86.111 listed in combined.njabl.org]
                // -1.8 AWL                    AWL: From: address is in the auto white-list

                while ((line = in.readLine()) != null) {
                    if (firstLine == null)
                        firstLine = line;
                    logger.debug(line);
                    Matcher matcher = REPORT_PATTERN.matcher(line);
                    if (matcher.lookingAt()) {
                        line = line.trim(); // Trim leading space
                        int j = line.indexOf(' ');
                        String score = line.substring(0, j);
                        int k = line.indexOf(' ', j + 1);
                        if (k < 0)
                            k = line.length();
                        String category = line.substring(j + 1, k);
                        if (logger.isDebugEnabled())
                            logger.debug("adding item " + score + ", " + category);
                        ReportItem ri = new ReportItem(Float.parseFloat(score),
                                                       category);
                        items.add(ri);
                    }
                }
                this.result = new SpamReport(items, threshold);

                scanProcess.waitFor();
                i = scanProcess.exitValue();
                in.close();
                is.close();
            }
            catch (java.io.IOException e) {
                /**
                 * This is only a warning because this happens when the process is killed because
                 * the timer expires
                 */
                logger.warn("Scan Exception: ", e);
                scanProcess.destroy();
                this.result = cleanReport();
                return;
            }
            catch (java.lang.InterruptedException e) {
                logger.warn("spamassassin interrupted: ", e);
                this.result = cleanReport();
                return;
            }
            catch (Exception e) {
                logger.error("Scan Exception: ", e);
                scanProcess.destroy();
                this.result = cleanReport();
                return;
            }


            /**
             * PROGRAM EXIT CODES
             * 0 : Currently, everything
             */
            if (i != 0) {
                logger.warn("spamassassin nonzero exit: " + i);
                this.result = cleanReport();
                return;
            }
        }
        catch (java.io.IOException e) {
            logger.error("spamassassin scan exception: ", e);
            this.result = cleanReport();
            return;
        }
        catch (Exception e) {
            logger.warn("spamassassin exception: ", e);
            this.result = cleanReport();
            return;
        }
        finally {
            synchronized (this) {this.notifyAll();}
        }
    }
}
