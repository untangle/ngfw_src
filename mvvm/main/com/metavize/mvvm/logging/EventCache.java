/*
 * Copyright (c) 2005 Metavize Inc.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Metavize Inc. ("Confidential Information").  You shall
 * not disclose such Confidential Information.
 *
 * $Id$
 */

package com.metavize.mvvm.logging;

public interface EventCache<E extends LogEvent>
  extends EventRepository<E> {
  
  void log(E e);
    
  void checkCold();

  /**
   * Sets a reference to the EventLogger as soon
   * as this cache is added to an EventLogger
   */
  void setEventLogger(EventLogger<E> eventLogger);

}
