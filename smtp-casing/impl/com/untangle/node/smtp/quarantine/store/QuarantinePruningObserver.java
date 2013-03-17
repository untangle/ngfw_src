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

package com.untangle.node.smtp.quarantine.store;

import java.io.File;

import com.untangle.node.smtp.quarantine.InboxRecord;

/**
 * Callback interface, used while the
 * {@link com.untangle.node.smtp.quarantine.store.QuatantineStore#prune store is pruning}.
 * <br><br>
 * Some day, this may make for interesting GUI stuff.
 */
public interface QuarantinePruningObserver {

    public static final QuarantinePruningObserver NOOP =
        new QuarantinePruningObserver() {
            public void preVisitInboxForOldMessages(String address, RelativeFileName inboxDir) {}

            public void pruningOldMessage(String recipient,
                                          File data,
                                          InboxRecord record) {}

            public void postVisitInboxForOldMessages(String address, RelativeFileName inboxDir) {}

            public void pruningOldMailbox(String account,
                                          RelativeFileName dirName,
                                          long lastTouched) {}

        };


    public void preVisitInboxForOldMessages(String address, RelativeFileName inboxDir);

    public void pruningOldMessage(String recipient,
                                  File data,
                                  InboxRecord record);

    public void postVisitInboxForOldMessages(String address, RelativeFileName inboxDir);

    public void pruningOldMailbox(String account,
                                  RelativeFileName dirName,
                                  long lastTouched);

}
