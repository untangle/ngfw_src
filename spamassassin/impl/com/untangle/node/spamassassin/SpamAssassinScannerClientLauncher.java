/*
 * $HeadURL$
 * Copyright (c) 2003-2007 Untangle, Inc.
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License, version 2,
 * as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful, but
 * AS-IS and WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE, TITLE, or
 * NONINFRINGEMENT.  See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 */
package com.untangle.node.spamassassin;

import java.io.File;
import java.lang.Thread;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.untangle.node.spam.ReportItem;
import com.untangle.node.spam.SpamReport;
import com.untangle.uvm.LocalUvmContextFactory;
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

        boolean allNull = true;

        // consolidate results into final item list
        // -> add unique and remove dupl items
        List<ReportItem> itemFinalList = new LinkedList<ReportItem>();
        for (SpamReport result : results) {
            if (null != result) {
                allNull = false;
                //logger.debug("result: " + result);
                consolidate(itemFinalList, result);
            }
        }

        if (allNull) {
            logger.debug("All null reports indicates timeout");
            return null;
        } else {
            // if final item list is empty, final result will be clean
            SpamReport resultFinal = new SpamReport(itemFinalList, threshold);
            logger.debug("spamc: " + resultFinal);
            return resultFinal;
        }
    }

    private SpamAssassinClient createClient(SpamAssassinClientContext cContext) {
        SpamAssassinClient client = new SpamAssassinClient(cContext, "spamc");
        Thread thread = LocalUvmContextFactory.context().newThread(client);
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

    private Collection<SpamAssassinClientContext> getClientContexts() {
        return clientMap.values();
    }

    private SpamReport[] getResults() {
        Collection<SpamAssassinClientContext> cContexts = getClientContexts();
        SpamReport[] results = new SpamReport[cContexts.size()];
        int idx = 0;
        for (SpamAssassinClientContext cContext : cContexts) {
            if (cContext.isDone()) {
                results[idx] = cContext.getResult();
            } else {
                results[idx] = null;
            }
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
            return; // same as itemList that is empty

        List<ReportItem> itemList = result.getItems();
        if (true == baseItemList.isEmpty()) {
            // it's okay if itemList is empty
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
