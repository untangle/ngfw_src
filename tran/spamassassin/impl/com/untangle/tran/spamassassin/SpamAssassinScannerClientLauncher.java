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

import java.io.File;
import java.lang.Thread;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.untangle.mvvm.MvvmContextFactory;
import com.untangle.tran.spam.ReportItem;
import com.untangle.tran.spam.SpamReport;
import org.apache.log4j.Logger;

public class SpamAssassinScannerClientLauncher {
    protected final Logger logger = Logger.getLogger(getClass());

    private Map<SpamAssassinClient, SpamAssassinClientContext> clientMap;

    private File msgFile;
    private float threshold;

    /**
     * Create a ClientLauncher for the given file
     */
    public SpamAssassinScannerClientLauncher(File msgFile, float threshold) {
        this.msgFile = msgFile;
        this.threshold = threshold;
    }

    public SpamReport doScan(long timeout) {
        SpamAssassinClient[] clients = createClients(); // create scanners

        for (SpamAssassinClient clientStart : clients) {
            clientStart.startScan(); // start scanning
        }

        // wait for results or stop scanning if too much time has passed
        long remainingTime = timeout;
        long startTime = System.currentTimeMillis();
        for (SpamAssassinClient clientWait : clients) {
            if (0 < remainingTime) {
                // time remains; let other clients continue
                logger.debug("spamc: " + clientWait + ", wait: " + remainingTime);
                clientWait.checkProgress(remainingTime);
                remainingTime = timeout - (System.currentTimeMillis() - startTime);
            } else {
                // no time remains; stop other clients
                logger.warn("spamc: " + clientWait + ", stop (timed out)");
                clientWait.stopScan();
            }
        }

        SpamReport[] results = getResults(); // get results
        freeClients(); // destroy scanners

        // consolidate results into final item list
        // -> add unique and remove dupl items
        List<ReportItem> itemFinalList = new LinkedList<ReportItem>();
        for (SpamReport result : results) {
            //logger.debug("result: " + result);
            consolidate(itemFinalList, result);
        }

        // if final item list is empty, final result will be clean
        SpamReport resultFinal = new SpamReport(itemFinalList, threshold);
        logger.debug("spamc: " + resultFinal);

        return resultFinal;
    }

    private SpamAssassinClient createClient(SpamAssassinClientContext cContext) {
        SpamAssassinClient client = new SpamAssassinClient(cContext, "spamc");
        Thread thread = MvvmContextFactory.context().newThread(client);
        client.setThread(thread);
        clientMap.put(client, cContext);
        return client;
    }

    private Object[] getClients() {
        Set<SpamAssassinClient> clientSet = clientMap.keySet();
        return clientSet.toArray();
    }

    private SpamAssassinClient[] createClients() {
        clientMap = new HashMap();

        SpamAssassinClientContext cContext = new SpamAssassinClientContext(msgFile, SpamAssassinClientSocket.SPAMD_DEFHOST, SpamAssassinClientSocket.SPAMD_DEFPORT, threshold);
        SpamAssassinClient client = createClient(cContext);

/* XXXX
        cContext = new SpamAssassinClientContext(msgFile, SpamAssassinClientSocket.SPAMD_DEFHOST, SpamAssassinClientSocket.SPAMD_DEFPORT, threshold);
        client = createClient(cContext);
*/

        Object[] cObjects = getClients();
        SpamAssassinClient[] clients = new SpamAssassinClient[cObjects.length];
        int idx = 0;

        for (Object cObject : cObjects) {
            clients[idx] = (SpamAssassinClient) cObject;
            idx++;
        }

        return clients;
    }

    private Object[] getClientContexts() {
        return clientMap.values().toArray();
    }

    private SpamReport[] getResults() {
        Object[] cContexts = getClientContexts();
        SpamReport[] results = new SpamReport[cContexts.length];
        int idx = 0;
        for (Object cContext : cContexts) {
            results[idx] = ((SpamAssassinClientContext) cContext).getResult();
            idx++;
        }

        return results;
    }

    private void freeClients() {
        clientMap.clear();
        return;
    }

    public void consolidate(List<ReportItem> baseItemList, SpamReport result) {
        if (null == result)
            return;

        List<ReportItem> itemList = result.getItems();

        if (true == baseItemList.isEmpty()) {
            baseItemList.addAll(itemList);
            return;
        }

        Object[] baseObjects = baseItemList.toArray();

        ReportItem baseItem;
        int categoryCompare;
        int scoreCompare;
        boolean isMatch;

        for (ReportItem item : itemList) {
            isMatch = false;

            for (Object baseObject : baseObjects) {
                baseItem = (ReportItem) baseObject;
                categoryCompare = baseItem.getCategory().compareTo(item.getCategory());
                scoreCompare = Float.compare(baseItem.getScore(), item.getScore());

                if (0 == categoryCompare) {
                    if (0 > scoreCompare) {
                        // found "same" item but new item scored higher
                        // -> replace original item with new item
                        baseItemList.remove(baseObject);
                        baseItemList.add(item);
                    } // else keep original item

                    isMatch = true;
                    break; // done
                }
            }

            if (false == isMatch) {
                // don't have this item yet
                // -> add new item
                baseItemList.add(item);
            }
        }

        return;
    }

    private SpamReport cleanReport() {
        return new SpamReport(new LinkedList<ReportItem>(), threshold);
    }
}
