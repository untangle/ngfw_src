/*
 * Copyright (c) 2003 Metavize Inc.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Metavize Inc. ("Confidential Information").  You shall
 * not disclose such Confidential Information.
 *
 * $Id: netcap_antisubscribe.h,v 1.1 2004/11/09 19:39:58 dmorris Exp $
 */

/**
 * Local antisubscribe functions
 */
int netcap_local_antisubscribe_add     ( void );
int netcap_local_antisubscribe_remove  ( void );

int netcap_local_antisubscribe_init    ( void );
int netcap_local_antisubscribe_cleanup ( void );

