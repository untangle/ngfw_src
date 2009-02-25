-- settings schema for release-6.0
-- $HeadURL$
-- Copyright (c) 2003-2009 Untangle, Inc.
-- All rights reserved.
--
-- This software is the confidential and proprietary information of
-- Untangle, Inc. ("Confidential Information"). You shall
-- not disclose such Confidential Information.
--
-- $Id$


CREATE TABLE settings.n_proxy_nonce (
    id int8 NOT NULL,
    nonce text,
    create_time timestamp,
    PRIMARY KEY (id)
);
