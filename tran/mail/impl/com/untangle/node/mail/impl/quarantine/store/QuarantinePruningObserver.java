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

package com.untangle.node.mail.impl.quarantine.store;

import java.io.File;

import com.untangle.node.mail.papi.quarantine.InboxRecord;

/**
 * Callback interface, used while the
 * {@link com.untangle.node.mail.impl.quarantine.store.QuatantineStore#prune store is pruning}.
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
