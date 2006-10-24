/*
 * Copyright (c) 2003-2006 Untangle Networks, Inc.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Metavize Inc. ("Confidential Information").  You shall
 * not disclose such Confidential Information.
 *
 * $Id$
 */

package com.metavize.tran.mail.papi.smtp;
import static com.metavize.tran.util.ASCIIUtil.bbToString;
import java.nio.ByteBuffer;

/**
 * Class reprsenting an unparsable line reveived
 * when a Command was expected. 
 */
public class UnparsableCommand 
  extends Command {

  private ByteBuffer m_unparsedLine;

  public UnparsableCommand(ByteBuffer badLine) {
    super(CommandType.UNKNOWN);
    m_unparsedLine = badLine;
  }

  @Override
  public String getArgString() {
    return bbToString(m_unparsedLine);
  }  

  @Override
  public ByteBuffer getBytes() {
    return m_unparsedLine.duplicate();
  }
}