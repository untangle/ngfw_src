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

package com.untangle.node.webfilter;

import java.util.List;

import com.untangle.node.http.UserWhitelistMode;
import com.untangle.uvm.logging.EventManager;
import com.untangle.uvm.node.IPMaddrRule;
import com.untangle.uvm.node.MimeTypeRule;
import com.untangle.uvm.node.Node;
import com.untangle.uvm.node.StringRule;
import com.untangle.uvm.node.Validator;

/**
 * Interface the the WebFilter Node.
 *
 * @author <a href="mailto:amread@untangle.com">Aaron Read</a>
 * @version 1.0
 */
public interface WebFilter extends Node
{
    WebFilterBaseSettings getBaseSettings();
    void setBaseSettings(WebFilterBaseSettings baseSettings);

    List<IPMaddrRule> getPassedClients(int start, int limit,
                                       String... sortColumns);
    void updatePassedClients(List<IPMaddrRule> added, List<Long> deleted,
                             List<IPMaddrRule> modified);

    List<StringRule> getPassedUrls(int start, int limit, String... sortColumns);
    void updatePassedUrls(List<StringRule> added, List<Long> deleted,
                          List<StringRule> modified);

    List<StringRule> getBlockedUrls(int start, int limit,
                                    String... sortColumns);
    void updateBlockedUrls(List<StringRule> added, List<Long> deleted,
                           List<StringRule> modified);

    List<MimeTypeRule> getBlockedMimeTypes(int start, int limit,
                                           String... sortColumns);
    void updateBlockedMimeTypes(List<MimeTypeRule> added, List<Long> deleted,
                                List<MimeTypeRule> modified);

    List<StringRule> getBlockedExtensions(int start, int limit,
                                          String... sortColumns);
    void updateBlockedExtensions(List<StringRule> added, List<Long> deleted,
                                 List<StringRule> modified);

    List<BlacklistCategory> getBlacklistCategories(int start, int limit,
                                                   String... sortColumns);
    void updateBlacklistCategories(List<BlacklistCategory> added,
                                   List<Long> deleted,
                                   List<BlacklistCategory> modified);

    /**
     * Update all settings once, in a single transaction
     */
    @SuppressWarnings("unchecked")
	void updateAll(WebFilterBaseSettings baseSettings,
            List[] passedClients, List[] passedUrls,
            List[] blockedUrls, List[] blockedMimeTypes,
            List[] blockedExtensions, List[] blacklistCategories);

    Validator getValidator();

    /**
     * Reconfigure node. This method should be called after some settings are updated
     * in order to reconfigure the node accordingly.
     */
    void reconfigure();

    WebFilterBlockDetails getDetails(String nonce);
    boolean unblockSite(String nonce, boolean global);

    UserWhitelistMode getUserWhitelistMode();

    EventManager<WebFilterEvent> getEventManager();

    EventManager<UnblockEvent> getUnblockEventManager();

}
