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

package com.metavize.tran.mail.impl.imap;

import java.nio.ByteBuffer;
import org.apache.log4j.Logger;
import com.metavize.tran.mail.papi.imap.IMAPTokenizer;
import com.metavize.mvvm.tapi.TCPSession;
import com.metavize.tran.mime.MIMEPart;
import com.metavize.tran.mime.MIMEMessageHeaders;
import com.metavize.tran.token.AbstractParser;
import com.metavize.tran.token.TokenStreamer;
import com.metavize.tran.token.Chunk;
import com.metavize.tran.token.Token;
import com.metavize.tran.token.PassThruToken;
import com.metavize.tran.token.ParseException;
import com.metavize.tran.token.ParseResult;
import java.util.List;
import java.util.LinkedList;
import static com.metavize.tran.util.Ascii.*;
import com.metavize.tran.mail.papi.MessageInfo;
import com.metavize.tran.mail.papi.MessageBoundaryScanner;
import com.metavize.tran.mail.papi.MIMEAccumulator;
import com.metavize.tran.mail.papi.imap.ImapChunk;
import com.metavize.tran.mail.papi.imap.UnparsableMIMEChunk;
import com.metavize.tran.mail.papi.imap.BeginImapMIMEToken;
import com.metavize.tran.mail.papi.ContinuedMIMEToken;
import java.io.IOException;
import com.metavize.tran.mail.papi.AddressKind;
import com.metavize.tran.mail.papi.MessageInfoFactory;

/**
 * 'name says it all...
 *
 * <br><br>
 * For the sake of documentation, here is the state
 * table for the embedded parser
 * <br><br>
<table cellpadding="3" cellspacing="0">
  <tbody>
    <tr>
      <td
 style="border-top: thin solid rgb(0, 0, 0); border-left: thin solid rgb(0, 0, 0);"></td>
      <td
 style="border-top: thin solid rgb(0, 0, 0); border-left: thin solid rgb(0, 0, 0); font-size: 10pt;"
 align="left" valign="bottom">EOL</td>
      <td
 style="border-top: thin solid rgb(0, 0, 0); border-left: thin solid rgb(0, 0, 0); font-size: 10pt;"
 align="left" valign="bottom">"FETCH"</td>
      <td
 style="border-top: thin solid rgb(0, 0, 0); border-left: thin solid rgb(0, 0, 0); font-size: 10pt;"
 align="left" valign="bottom">"BODY"</td>
      <td
 style="border-top: thin solid rgb(0, 0, 0); border-left: thin solid rgb(0, 0, 0); font-size: 10pt;"
 align="left" valign="bottom">"RFC822"</td>
      <td
 style="border-top: thin solid rgb(0, 0, 0); border-left: thin solid rgb(0, 0, 0); font-size: 10pt;"
 align="left" valign="bottom">"PEEK"</td>
      <td
 style="border-top: thin solid rgb(0, 0, 0); border-left: thin solid rgb(0, 0, 0); font-size: 10pt;"
 align="left" valign="bottom">word</td>
      <td
 style="border-top: thin solid rgb(0, 0, 0); border-left: thin solid rgb(0, 0, 0); font-size: 10pt;"
 align="left" valign="bottom">Qstr</td>
      <td
 style="border-top: thin solid rgb(0, 0, 0); border-left: thin solid rgb(0, 0, 0); font-size: 10pt;"
 align="left" valign="bottom">Literal</td>
      <td
 style="border-top: thin solid rgb(0, 0, 0); border-left: thin solid rgb(0, 0, 0); font-size: 10pt;"
 align="left" valign="bottom">OB [</td>
      <td
 style="border-top: thin solid rgb(0, 0, 0); border-left: thin solid rgb(0, 0, 0); font-size: 10pt;"
 align="left" valign="bottom">CB ]</td>
      <td
 style="border-top: thin solid rgb(0, 0, 0); border-left: thin solid rgb(0, 0, 0); font-size: 10pt;"
 align="left" valign="bottom">LT &lt;</td>
      <td
 style="border-top: thin solid rgb(0, 0, 0); border-left: thin solid rgb(0, 0, 0); font-size: 10pt;"
 align="left" valign="bottom">Paren (</td>
      <td
 style="border-top: thin solid rgb(0, 0, 0); border-left: thin solid rgb(0, 0, 0); font-size: 10pt;"
 align="left" valign="bottom">Dot .</td>
      <td
 style="border-top: thin solid rgb(0, 0, 0); border-left: thin solid rgb(0, 0, 0); border-right: thin solid rgb(0, 0, 0); font-size: 10pt;"
 align="left" valign="bottom">Delim</td>
    </tr>
    <tr>
      <td
 style="border-top: thin solid rgb(0, 0, 0); border-left: thin solid rgb(0, 0, 0); font-size: 10pt;"
 align="left" valign="bottom">(s1) Skipping Literal</td>
      <td
 style="border-top: thin solid rgb(0, 0, 0); border-left: thin solid rgb(0, 0, 0); font-size: 10pt;"
 align="right" valign="bottom">0</td>
      <td
 style="border-top: thin solid rgb(0, 0, 0); border-left: thin solid rgb(0, 0, 0); font-size: 10pt;"
 align="right" valign="bottom">0</td>
      <td
 style="border-top: thin solid rgb(0, 0, 0); border-left: thin solid rgb(0, 0, 0); font-size: 10pt;"
 align="right" valign="bottom">0</td>
      <td
 style="border-top: thin solid rgb(0, 0, 0); border-left: thin solid rgb(0, 0, 0); font-size: 10pt;"
 align="right" valign="bottom">0</td>
      <td
 style="border-top: thin solid rgb(0, 0, 0); border-left: thin solid rgb(0, 0, 0); font-size: 10pt;"
 align="right" valign="bottom">0</td>
      <td
 style="border-top: thin solid rgb(0, 0, 0); border-left: thin solid rgb(0, 0, 0); font-size: 10pt;"
 align="right" valign="bottom">0</td>
      <td
 style="border-top: thin solid rgb(0, 0, 0); border-left: thin solid rgb(0, 0, 0); font-size: 10pt;"
 align="right" valign="bottom">0</td>
      <td
 style="border-top: thin solid rgb(0, 0, 0); border-left: thin solid rgb(0, 0, 0); font-size: 10pt;"
 align="right" valign="bottom">0</td>
      <td
 style="border-top: thin solid rgb(0, 0, 0); border-left: thin solid rgb(0, 0, 0); font-size: 10pt;"
 align="right" valign="bottom">0</td>
      <td
 style="border-top: thin solid rgb(0, 0, 0); border-left: thin solid rgb(0, 0, 0); font-size: 10pt;"
 align="right" valign="bottom">0</td>
      <td
 style="border-top: thin solid rgb(0, 0, 0); border-left: thin solid rgb(0, 0, 0); font-size: 10pt;"
 align="right" valign="bottom">0</td>
      <td
 style="border-top: thin solid rgb(0, 0, 0); border-left: thin solid rgb(0, 0, 0); font-size: 10pt;"
 align="right" valign="bottom">0</td>
      <td
 style="border-top: thin solid rgb(0, 0, 0); border-left: thin solid rgb(0, 0, 0); font-size: 10pt;"
 align="right" valign="bottom">0</td>
      <td
 style="border-top: thin solid rgb(0, 0, 0); border-left: thin solid rgb(0, 0, 0); border-right: thin solid rgb(0, 0, 0); font-size: 10pt;"
 align="right" valign="bottom">0</td>
    </tr>
    <tr>
      <td
 style="border-top: thin solid rgb(0, 0, 0); border-left: thin solid rgb(0, 0, 0); font-size: 10pt;"
 align="left" valign="bottom">(s2) Draining Body</td>
      <td
 style="border-top: thin solid rgb(0, 0, 0); border-left: thin solid rgb(0, 0, 0); font-size: 10pt;"
 align="right" valign="bottom">0</td>
      <td
 style="border-top: thin solid rgb(0, 0, 0); border-left: thin solid rgb(0, 0, 0); font-size: 10pt;"
 align="right" valign="bottom">0</td>
      <td
 style="border-top: thin solid rgb(0, 0, 0); border-left: thin solid rgb(0, 0, 0); font-size: 10pt;"
 align="right" valign="bottom">0</td>
      <td
 style="border-top: thin solid rgb(0, 0, 0); border-left: thin solid rgb(0, 0, 0); font-size: 10pt;"
 align="right" valign="bottom">0</td>
      <td
 style="border-top: thin solid rgb(0, 0, 0); border-left: thin solid rgb(0, 0, 0); font-size: 10pt;"
 align="right" valign="bottom">0</td>
      <td
 style="border-top: thin solid rgb(0, 0, 0); border-left: thin solid rgb(0, 0, 0); font-size: 10pt;"
 align="right" valign="bottom">0</td>
      <td
 style="border-top: thin solid rgb(0, 0, 0); border-left: thin solid rgb(0, 0, 0); font-size: 10pt;"
 align="right" valign="bottom">0</td>
      <td
 style="border-top: thin solid rgb(0, 0, 0); border-left: thin solid rgb(0, 0, 0); font-size: 10pt;"
 align="right" valign="bottom">0</td>
      <td
 style="border-top: thin solid rgb(0, 0, 0); border-left: thin solid rgb(0, 0, 0); font-size: 10pt;"
 align="right" valign="bottom">0</td>
      <td
 style="border-top: thin solid rgb(0, 0, 0); border-left: thin solid rgb(0, 0, 0); font-size: 10pt;"
 align="right" valign="bottom">0</td>
      <td
 style="border-top: thin solid rgb(0, 0, 0); border-left: thin solid rgb(0, 0, 0); font-size: 10pt;"
 align="right" valign="bottom">0</td>
      <td
 style="border-top: thin solid rgb(0, 0, 0); border-left: thin solid rgb(0, 0, 0); font-size: 10pt;"
 align="right" valign="bottom">0</td>
      <td
 style="border-top: thin solid rgb(0, 0, 0); border-left: thin solid rgb(0, 0, 0); font-size: 10pt;"
 align="right" valign="bottom">0</td>
      <td
 style="border-top: thin solid rgb(0, 0, 0); border-left: thin solid rgb(0, 0, 0); border-right: thin solid rgb(0, 0, 0); font-size: 10pt;"
 align="right" valign="bottom">0</td>
    </tr>
    <tr>
      <td
 style="border-top: thin solid rgb(0, 0, 0); border-left: thin solid rgb(0, 0, 0); font-size: 10pt;"
 align="left" valign="bottom">(s3) Scanning New Line</td>
      <td
 style="border-top: thin solid rgb(0, 0, 0); border-left: thin solid rgb(0, 0, 0); font-size: 10pt;"
 align="right" valign="bottom">4</td>
      <td
 style="border-top: thin solid rgb(0, 0, 0); border-left: thin solid rgb(0, 0, 0); font-size: 10pt;"
 align="right" valign="bottom">2</td>
      <td
 style="border-top: thin solid rgb(0, 0, 0); border-left: thin solid rgb(0, 0, 0); font-size: 10pt;"
 align="right" valign="bottom">1</td>
      <td
 style="border-top: thin solid rgb(0, 0, 0); border-left: thin solid rgb(0, 0, 0); font-size: 10pt;"
 align="right" valign="bottom">1</td>
      <td
 style="border-top: thin solid rgb(0, 0, 0); border-left: thin solid rgb(0, 0, 0); font-size: 10pt;"
 align="right" valign="bottom">1</td>
      <td
 style="border-top: thin solid rgb(0, 0, 0); border-left: thin solid rgb(0, 0, 0); font-size: 10pt;"
 align="right" valign="bottom">1</td>
      <td
 style="border-top: thin solid rgb(0, 0, 0); border-left: thin solid rgb(0, 0, 0); font-size: 10pt;"
 align="right" valign="bottom">1</td>
      <td
 style="border-top: thin solid rgb(0, 0, 0); border-left: thin solid rgb(0, 0, 0); font-size: 10pt;"
 align="right" valign="bottom">3</td>
      <td
 style="border-top: thin solid rgb(0, 0, 0); border-left: thin solid rgb(0, 0, 0); font-size: 10pt;"
 align="right" valign="bottom">4</td>
      <td
 style="border-top: thin solid rgb(0, 0, 0); border-left: thin solid rgb(0, 0, 0); font-size: 10pt;"
 align="right" valign="bottom">4</td>
      <td
 style="border-top: thin solid rgb(0, 0, 0); border-left: thin solid rgb(0, 0, 0); font-size: 10pt;"
 align="right" valign="bottom">4</td>
      <td
 style="border-top: thin solid rgb(0, 0, 0); border-left: thin solid rgb(0, 0, 0); font-size: 10pt;"
 align="right" valign="bottom">4</td>
      <td
 style="border-top: thin solid rgb(0, 0, 0); border-left: thin solid rgb(0, 0, 0); font-size: 10pt;"
 align="right" valign="bottom">4</td>
      <td
 style="border-top: thin solid rgb(0, 0, 0); border-left: thin solid rgb(0, 0, 0); border-right: thin solid rgb(0, 0, 0); font-size: 10pt;"
 align="right" valign="bottom">4</td>
    </tr>
    <tr>
      <td
 style="border-top: thin solid rgb(0, 0, 0); border-left: thin solid rgb(0, 0, 0); font-size: 10pt;"
 align="left" valign="bottom">Saw "FETCH"</td>
      <td
 style="border-top: thin solid rgb(0, 0, 0); border-left: thin solid rgb(0, 0, 0); font-size: 10pt;"
 align="right" valign="bottom">6</td>
      <td
 style="border-top: thin solid rgb(0, 0, 0); border-left: thin solid rgb(0, 0, 0); font-size: 10pt;"
 align="right" valign="bottom">4</td>
      <td
 style="border-top: thin solid rgb(0, 0, 0); border-left: thin solid rgb(0, 0, 0); font-size: 10pt;"
 align="right" valign="bottom">4</td>
      <td
 style="border-top: thin solid rgb(0, 0, 0); border-left: thin solid rgb(0, 0, 0); font-size: 10pt;"
 align="right" valign="bottom">4</td>
      <td
 style="border-top: thin solid rgb(0, 0, 0); border-left: thin solid rgb(0, 0, 0); font-size: 10pt;"
 align="right" valign="bottom">4</td>
      <td
 style="border-top: thin solid rgb(0, 0, 0); border-left: thin solid rgb(0, 0, 0); font-size: 10pt;"
 align="right" valign="bottom">4</td>
      <td
 style="border-top: thin solid rgb(0, 0, 0); border-left: thin solid rgb(0, 0, 0); font-size: 10pt;"
 align="right" valign="bottom">4</td>
      <td
 style="border-top: thin solid rgb(0, 0, 0); border-left: thin solid rgb(0, 0, 0); font-size: 10pt;"
 align="right" valign="bottom">9</td>
      <td
 style="border-top: thin solid rgb(0, 0, 0); border-left: thin solid rgb(0, 0, 0); font-size: 10pt;"
 align="right" valign="bottom">4</td>
      <td
 style="border-top: thin solid rgb(0, 0, 0); border-left: thin solid rgb(0, 0, 0); font-size: 10pt;"
 align="right" valign="bottom">4</td>
      <td
 style="border-top: thin solid rgb(0, 0, 0); border-left: thin solid rgb(0, 0, 0); font-size: 10pt;"
 align="right" valign="bottom">4</td>
      <td
 style="border-top: thin solid rgb(0, 0, 0); border-left: thin solid rgb(0, 0, 0); font-size: 10pt;"
 align="right" valign="bottom">5</td>
      <td
 style="border-top: thin solid rgb(0, 0, 0); border-left: thin solid rgb(0, 0, 0); font-size: 10pt;"
 align="right" valign="bottom">4</td>
      <td
 style="border-top: thin solid rgb(0, 0, 0); border-left: thin solid rgb(0, 0, 0); border-right: thin solid rgb(0, 0, 0); font-size: 10pt;"
 align="right" valign="bottom">4</td>
    </tr>
    <tr>
      <td
 style="border-top: thin solid rgb(0, 0, 0); border-left: thin solid rgb(0, 0, 0); font-size: 10pt;"
 align="left" valign="bottom">(s4) Skipping to end of line</td>
      <td
 style="border-top: thin solid rgb(0, 0, 0); border-left: thin solid rgb(0, 0, 0); font-size: 10pt;"
 align="right" valign="bottom">6</td>
      <td
 style="border-top: thin solid rgb(0, 0, 0); border-left: thin solid rgb(0, 0, 0); font-size: 10pt;"
 align="right" valign="bottom">4</td>
      <td
 style="border-top: thin solid rgb(0, 0, 0); border-left: thin solid rgb(0, 0, 0); font-size: 10pt;"
 align="right" valign="bottom">4</td>
      <td
 style="border-top: thin solid rgb(0, 0, 0); border-left: thin solid rgb(0, 0, 0); font-size: 10pt;"
 align="right" valign="bottom">4</td>
      <td
 style="border-top: thin solid rgb(0, 0, 0); border-left: thin solid rgb(0, 0, 0); font-size: 10pt;"
 align="right" valign="bottom">4</td>
      <td
 style="border-top: thin solid rgb(0, 0, 0); border-left: thin solid rgb(0, 0, 0); font-size: 10pt;"
 align="right" valign="bottom">4</td>
      <td
 style="border-top: thin solid rgb(0, 0, 0); border-left: thin solid rgb(0, 0, 0); font-size: 10pt;"
 align="right" valign="bottom">4</td>
      <td
 style="border-top: thin solid rgb(0, 0, 0); border-left: thin solid rgb(0, 0, 0); font-size: 10pt;"
 align="right" valign="bottom">9</td>
      <td
 style="border-top: thin solid rgb(0, 0, 0); border-left: thin solid rgb(0, 0, 0); font-size: 10pt;"
 align="right" valign="bottom">4</td>
      <td
 style="border-top: thin solid rgb(0, 0, 0); border-left: thin solid rgb(0, 0, 0); font-size: 10pt;"
 align="right" valign="bottom">4</td>
      <td
 style="border-top: thin solid rgb(0, 0, 0); border-left: thin solid rgb(0, 0, 0); font-size: 10pt;"
 align="right" valign="bottom">4</td>
      <td
 style="border-top: thin solid rgb(0, 0, 0); border-left: thin solid rgb(0, 0, 0); font-size: 10pt;"
 align="right" valign="bottom">4</td>
      <td
 style="border-top: thin solid rgb(0, 0, 0); border-left: thin solid rgb(0, 0, 0); font-size: 10pt;"
 align="right" valign="bottom">4</td>
      <td
 style="border-top: thin solid rgb(0, 0, 0); border-left: thin solid rgb(0, 0, 0); border-right: thin solid rgb(0, 0, 0); font-size: 10pt;"
 align="right" valign="bottom">4</td>
    </tr>
    <tr>
      <td
 style="border-top: thin solid rgb(0, 0, 0); border-left: thin solid rgb(0, 0, 0); font-size: 10pt;"
 align="left" valign="bottom">(s5) Saw "FETCH .. ("</td>
      <td
 style="border-top: thin solid rgb(0, 0, 0); border-left: thin solid rgb(0, 0, 0); font-size: 10pt;"
 align="right" valign="bottom">6</td>
      <td
 style="border-top: thin solid rgb(0, 0, 0); border-left: thin solid rgb(0, 0, 0); font-size: 10pt;"
 align="right" valign="bottom">4</td>
      <td
 style="border-top: thin solid rgb(0, 0, 0); border-left: thin solid rgb(0, 0, 0); font-size: 10pt;"
 align="right" valign="bottom">7</td>
      <td
 style="border-top: thin solid rgb(0, 0, 0); border-left: thin solid rgb(0, 0, 0); font-size: 10pt;"
 align="right" valign="bottom">8</td>
      <td
 style="border-top: thin solid rgb(0, 0, 0); border-left: thin solid rgb(0, 0, 0); font-size: 10pt;"
 align="right" valign="bottom">4</td>
      <td
 style="border-top: thin solid rgb(0, 0, 0); border-left: thin solid rgb(0, 0, 0); font-size: 10pt;"
 align="right" valign="bottom">4</td>
      <td
 style="border-top: thin solid rgb(0, 0, 0); border-left: thin solid rgb(0, 0, 0); font-size: 10pt;"
 align="right" valign="bottom">4</td>
      <td
 style="border-top: thin solid rgb(0, 0, 0); border-left: thin solid rgb(0, 0, 0); font-size: 10pt;"
 align="right" valign="bottom">9</td>
      <td
 style="border-top: thin solid rgb(0, 0, 0); border-left: thin solid rgb(0, 0, 0); font-size: 10pt;"
 align="right" valign="bottom">4</td>
      <td
 style="border-top: thin solid rgb(0, 0, 0); border-left: thin solid rgb(0, 0, 0); font-size: 10pt;"
 align="right" valign="bottom">4</td>
      <td
 style="border-top: thin solid rgb(0, 0, 0); border-left: thin solid rgb(0, 0, 0); font-size: 10pt;"
 align="right" valign="bottom">4</td>
      <td
 style="border-top: thin solid rgb(0, 0, 0); border-left: thin solid rgb(0, 0, 0); font-size: 10pt;"
 align="right" valign="bottom">4</td>
      <td
 style="border-top: thin solid rgb(0, 0, 0); border-left: thin solid rgb(0, 0, 0); font-size: 10pt;"
 align="right" valign="bottom">4</td>
      <td
 style="border-top: thin solid rgb(0, 0, 0); border-left: thin solid rgb(0, 0, 0); border-right: thin solid rgb(0, 0, 0); font-size: 10pt;"
 align="right" valign="bottom">4</td>
    </tr>
    <tr>
      <td
 style="border-top: thin solid rgb(0, 0, 0); border-left: thin solid rgb(0, 0, 0); font-size: 10pt;"
 align="left" valign="bottom">(s6) Saw "FETCH...(...BODY"</td>
      <td
 style="border-top: thin solid rgb(0, 0, 0); border-left: thin solid rgb(0, 0, 0); font-size: 10pt;"
 align="right" valign="bottom">6</td>
      <td
 style="border-top: thin solid rgb(0, 0, 0); border-left: thin solid rgb(0, 0, 0); font-size: 10pt;"
 align="right" valign="bottom">5</td>
      <td
 style="border-top: thin solid rgb(0, 0, 0); border-left: thin solid rgb(0, 0, 0); font-size: 10pt;"
 align="right" valign="bottom">4</td>
      <td
 style="border-top: thin solid rgb(0, 0, 0); border-left: thin solid rgb(0, 0, 0); font-size: 10pt;"
 align="right" valign="bottom">8</td>
      <td
 style="border-top: thin solid rgb(0, 0, 0); border-left: thin solid rgb(0, 0, 0); font-size: 10pt;"
 align="right" valign="bottom">5</td>
      <td
 style="border-top: thin solid rgb(0, 0, 0); border-left: thin solid rgb(0, 0, 0); font-size: 10pt;"
 align="right" valign="bottom">5</td>
      <td
 style="border-top: thin solid rgb(0, 0, 0); border-left: thin solid rgb(0, 0, 0); font-size: 10pt;"
 align="right" valign="bottom">5</td>
      <td
 style="border-top: thin solid rgb(0, 0, 0); border-left: thin solid rgb(0, 0, 0); font-size: 10pt;"
 align="right" valign="bottom">10</td>
      <td
 style="border-top: thin solid rgb(0, 0, 0); border-left: thin solid rgb(0, 0, 0); font-size: 10pt;"
 align="right" valign="bottom">11</td>
      <td
 style="border-top: thin solid rgb(0, 0, 0); border-left: thin solid rgb(0, 0, 0); font-size: 10pt;"
 align="right" valign="bottom">5</td>
      <td
 style="border-top: thin solid rgb(0, 0, 0); border-left: thin solid rgb(0, 0, 0); font-size: 10pt;"
 align="right" valign="bottom">5</td>
      <td
 style="border-top: thin solid rgb(0, 0, 0); border-left: thin solid rgb(0, 0, 0); font-size: 10pt;"
 align="right" valign="bottom">5</td>
      <td
 style="border-top: thin solid rgb(0, 0, 0); border-left: thin solid rgb(0, 0, 0); font-size: 10pt;"
 align="right" valign="bottom">12</td>
      <td
 style="border-top: thin solid rgb(0, 0, 0); border-left: thin solid rgb(0, 0, 0); border-right: thin solid rgb(0, 0, 0); font-size: 10pt;"
 align="right" valign="bottom">5</td>
    </tr>
    <tr>
      <td
 style="border-top: thin solid rgb(0, 0, 0); border-left: thin solid rgb(0, 0, 0); font-size: 10pt;"
 align="left" valign="bottom">(s7) Saw "FETCH...(...RFC822"</td>
      <td
 style="border-top: thin solid rgb(0, 0, 0); border-left: thin solid rgb(0, 0, 0); font-size: 10pt;"
 align="right" valign="bottom">6</td>
      <td
 style="border-top: thin solid rgb(0, 0, 0); border-left: thin solid rgb(0, 0, 0); font-size: 10pt;"
 align="right" valign="bottom">5</td>
      <td
 style="border-top: thin solid rgb(0, 0, 0); border-left: thin solid rgb(0, 0, 0); font-size: 10pt;"
 align="right" valign="bottom">7</td>
      <td
 style="border-top: thin solid rgb(0, 0, 0); border-left: thin solid rgb(0, 0, 0); font-size: 10pt;"
 align="right" valign="bottom">4</td>
      <td
 style="border-top: thin solid rgb(0, 0, 0); border-left: thin solid rgb(0, 0, 0); font-size: 10pt;"
 align="right" valign="bottom">5</td>
      <td
 style="border-top: thin solid rgb(0, 0, 0); border-left: thin solid rgb(0, 0, 0); font-size: 10pt;"
 align="right" valign="bottom">5</td>
      <td
 style="border-top: thin solid rgb(0, 0, 0); border-left: thin solid rgb(0, 0, 0); font-size: 10pt;"
 align="right" valign="bottom">5</td>
      <td
 style="border-top: thin solid rgb(0, 0, 0); border-left: thin solid rgb(0, 0, 0); font-size: 10pt;"
 align="right" valign="bottom">13</td>
      <td
 style="border-top: thin solid rgb(0, 0, 0); border-left: thin solid rgb(0, 0, 0); font-size: 10pt;"
 align="right" valign="bottom">5</td>
      <td
 style="border-top: thin solid rgb(0, 0, 0); border-left: thin solid rgb(0, 0, 0); font-size: 10pt;"
 align="right" valign="bottom">5</td>
      <td
 style="border-top: thin solid rgb(0, 0, 0); border-left: thin solid rgb(0, 0, 0); font-size: 10pt;"
 align="right" valign="bottom">5</td>
      <td
 style="border-top: thin solid rgb(0, 0, 0); border-left: thin solid rgb(0, 0, 0); font-size: 10pt;"
 align="right" valign="bottom">5</td>
      <td
 style="border-top: thin solid rgb(0, 0, 0); border-left: thin solid rgb(0, 0, 0); font-size: 10pt;"
 align="right" valign="bottom">14</td>
      <td
 style="border-top: thin solid rgb(0, 0, 0); border-left: thin solid rgb(0, 0, 0); border-right: thin solid rgb(0, 0, 0); font-size: 10pt;"
 align="right" valign="bottom">5</td>
    </tr>
    <tr>
      <td
 style="border-top: thin solid rgb(0, 0, 0); border-left: thin solid rgb(0, 0, 0); font-size: 10pt;"
 align="left" valign="bottom">(s8) Saw "FETCH .. (...BODY["</td>
      <td
 style="border-top: thin solid rgb(0, 0, 0); border-left: thin solid rgb(0, 0, 0); font-size: 10pt;"
 align="right" valign="bottom">6</td>
      <td
 style="border-top: thin solid rgb(0, 0, 0); border-left: thin solid rgb(0, 0, 0); font-size: 10pt;"
 align="right" valign="bottom">14</td>
      <td
 style="border-top: thin solid rgb(0, 0, 0); border-left: thin solid rgb(0, 0, 0); font-size: 10pt;"
 align="right" valign="bottom">14</td>
      <td
 style="border-top: thin solid rgb(0, 0, 0); border-left: thin solid rgb(0, 0, 0); font-size: 10pt;"
 align="right" valign="bottom">14</td>
      <td
 style="border-top: thin solid rgb(0, 0, 0); border-left: thin solid rgb(0, 0, 0); font-size: 10pt;"
 align="right" valign="bottom">14</td>
      <td
 style="border-top: thin solid rgb(0, 0, 0); border-left: thin solid rgb(0, 0, 0); font-size: 10pt;"
 align="right" valign="bottom">14</td>
      <td
 style="border-top: thin solid rgb(0, 0, 0); border-left: thin solid rgb(0, 0, 0); font-size: 10pt;"
 align="right" valign="bottom">14</td>
      <td
 style="border-top: thin solid rgb(0, 0, 0); border-left: thin solid rgb(0, 0, 0); font-size: 10pt;"
 align="right" valign="bottom">10</td>
      <td
 style="border-top: thin solid rgb(0, 0, 0); border-left: thin solid rgb(0, 0, 0); font-size: 10pt;"
 align="right" valign="bottom">5</td>
      <td
 style="border-top: thin solid rgb(0, 0, 0); border-left: thin solid rgb(0, 0, 0); font-size: 10pt;"
 align="right" valign="bottom">15</td>
      <td
 style="border-top: thin solid rgb(0, 0, 0); border-left: thin solid rgb(0, 0, 0); font-size: 10pt;"
 align="right" valign="bottom">5</td>
      <td
 style="border-top: thin solid rgb(0, 0, 0); border-left: thin solid rgb(0, 0, 0); font-size: 10pt;"
 align="right" valign="bottom">5</td>
      <td
 style="border-top: thin solid rgb(0, 0, 0); border-left: thin solid rgb(0, 0, 0); font-size: 10pt;"
 align="right" valign="bottom">14</td>
      <td
 style="border-top: thin solid rgb(0, 0, 0); border-left: thin solid rgb(0, 0, 0); border-right: thin solid rgb(0, 0, 0); font-size: 10pt;"
 align="right" valign="bottom">5</td>
    </tr>
    <tr>
      <td
 style="border-top: thin solid rgb(0, 0, 0); border-left: thin solid rgb(0, 0, 0); font-size: 10pt;"
 align="left" valign="bottom">(s9) Saw "FETCH .. (...BODY[]"</td>
      <td
 style="border-top: thin solid rgb(0, 0, 0); border-left: thin solid rgb(0, 0, 0); font-size: 10pt;"
 align="right" valign="bottom">6</td>
      <td
 style="border-top: thin solid rgb(0, 0, 0); border-left: thin solid rgb(0, 0, 0); font-size: 10pt;"
 align="right" valign="bottom">6</td>
      <td
 style="border-top: thin solid rgb(0, 0, 0); border-left: thin solid rgb(0, 0, 0); font-size: 10pt;"
 align="right" valign="bottom">7</td>
      <td
 style="border-top: thin solid rgb(0, 0, 0); border-left: thin solid rgb(0, 0, 0); font-size: 10pt;"
 align="right" valign="bottom">8</td>
      <td
 style="border-top: thin solid rgb(0, 0, 0); border-left: thin solid rgb(0, 0, 0); font-size: 10pt;"
 align="right" valign="bottom">5</td>
      <td
 style="border-top: thin solid rgb(0, 0, 0); border-left: thin solid rgb(0, 0, 0); font-size: 10pt;"
 align="right" valign="bottom">5</td>
      <td
 style="border-top: thin solid rgb(0, 0, 0); border-left: thin solid rgb(0, 0, 0); font-size: 10pt;"
 align="right" valign="bottom">5</td>
      <td
 style="border-top: thin solid rgb(0, 0, 0); border-left: thin solid rgb(0, 0, 0); font-size: 10pt;"
 align="right" valign="bottom">13</td>
      <td
 style="border-top: thin solid rgb(0, 0, 0); border-left: thin solid rgb(0, 0, 0); font-size: 10pt;"
 align="right" valign="bottom">5</td>
      <td
 style="border-top: thin solid rgb(0, 0, 0); border-left: thin solid rgb(0, 0, 0); font-size: 10pt;"
 align="right" valign="bottom">5</td>
      <td
 style="border-top: thin solid rgb(0, 0, 0); border-left: thin solid rgb(0, 0, 0); font-size: 10pt;"
 align="right" valign="bottom">5</td>
      <td
 style="border-top: thin solid rgb(0, 0, 0); border-left: thin solid rgb(0, 0, 0); font-size: 10pt;"
 align="right" valign="bottom">5</td>
      <td
 style="border-top: thin solid rgb(0, 0, 0); border-left: thin solid rgb(0, 0, 0); font-size: 10pt;"
 align="right" valign="bottom">5</td>
      <td
 style="border-top: thin solid rgb(0, 0, 0); border-left: thin solid rgb(0, 0, 0); border-right: thin solid rgb(0, 0, 0); font-size: 10pt;"
 align="right" valign="bottom">5</td>
    </tr>
    <tr>
      <td
 style="border-top: thin solid rgb(0, 0, 0); border-left: thin solid rgb(0, 0, 0); font-size: 10pt;"
 align="left" valign="bottom">(s10) Checking for "BODY.PEEK"</td>
      <td
 style="border-top: thin solid rgb(0, 0, 0); border-left: thin solid rgb(0, 0, 0); font-size: 10pt;"
 align="right" valign="bottom">6</td>
      <td
 style="border-top: thin solid rgb(0, 0, 0); border-left: thin solid rgb(0, 0, 0); font-size: 10pt;"
 align="right" valign="bottom">5</td>
      <td
 style="border-top: thin solid rgb(0, 0, 0); border-left: thin solid rgb(0, 0, 0); font-size: 10pt;"
 align="right" valign="bottom">5</td>
      <td
 style="border-top: thin solid rgb(0, 0, 0); border-left: thin solid rgb(0, 0, 0); font-size: 10pt;"
 align="right" valign="bottom">5</td>
      <td
 style="border-top: thin solid rgb(0, 0, 0); border-left: thin solid rgb(0, 0, 0); font-size: 10pt;"
 align="right" valign="bottom">7</td>
      <td
 style="border-top: thin solid rgb(0, 0, 0); border-left: thin solid rgb(0, 0, 0); font-size: 10pt;"
 align="right" valign="bottom">5</td>
      <td
 style="border-top: thin solid rgb(0, 0, 0); border-left: thin solid rgb(0, 0, 0); font-size: 10pt;"
 align="right" valign="bottom">5</td>
      <td
 style="border-top: thin solid rgb(0, 0, 0); border-left: thin solid rgb(0, 0, 0); font-size: 10pt;"
 align="right" valign="bottom">10</td>
      <td
 style="border-top: thin solid rgb(0, 0, 0); border-left: thin solid rgb(0, 0, 0); font-size: 10pt;"
 align="right" valign="bottom">5</td>
      <td
 style="border-top: thin solid rgb(0, 0, 0); border-left: thin solid rgb(0, 0, 0); font-size: 10pt;"
 align="right" valign="bottom">5</td>
      <td
 style="border-top: thin solid rgb(0, 0, 0); border-left: thin solid rgb(0, 0, 0); font-size: 10pt;"
 align="right" valign="bottom">5</td>
      <td
 style="border-top: thin solid rgb(0, 0, 0); border-left: thin solid rgb(0, 0, 0); font-size: 10pt;"
 align="right" valign="bottom">5</td>
      <td
 style="border-top: thin solid rgb(0, 0, 0); border-left: thin solid rgb(0, 0, 0); font-size: 10pt;"
 align="right" valign="bottom">5</td>
      <td
 style="border-top: thin solid rgb(0, 0, 0); border-left: thin solid rgb(0, 0, 0); border-right: thin solid rgb(0, 0, 0); font-size: 10pt;"
 align="right" valign="bottom">5</td>
    </tr>
    <tr>
      <td
 style="border-top: thin solid rgb(0, 0, 0); border-left: thin solid rgb(0, 0, 0); border-bottom: thin solid rgb(0, 0, 0); font-size: 10pt;"
 align="left" valign="bottom">(s11) Skipping current Token, then going
to s6</td>
      <td
 style="border-top: thin solid rgb(0, 0, 0); border-left: thin solid rgb(0, 0, 0); border-bottom: thin solid rgb(0, 0, 0); font-size: 10pt;"
 align="right" valign="bottom">6</td>
      <td
 style="border-top: thin solid rgb(0, 0, 0); border-left: thin solid rgb(0, 0, 0); border-bottom: thin solid rgb(0, 0, 0); font-size: 10pt;"
 align="right" valign="bottom">5</td>
      <td
 style="border-top: thin solid rgb(0, 0, 0); border-left: thin solid rgb(0, 0, 0); border-bottom: thin solid rgb(0, 0, 0); font-size: 10pt;"
 align="right" valign="bottom">5</td>
      <td
 style="border-top: thin solid rgb(0, 0, 0); border-left: thin solid rgb(0, 0, 0); border-bottom: thin solid rgb(0, 0, 0); font-size: 10pt;"
 align="right" valign="bottom">5</td>
      <td
 style="border-top: thin solid rgb(0, 0, 0); border-left: thin solid rgb(0, 0, 0); border-bottom: thin solid rgb(0, 0, 0); font-size: 10pt;"
 align="right" valign="bottom">5</td>
      <td
 style="border-top: thin solid rgb(0, 0, 0); border-left: thin solid rgb(0, 0, 0); border-bottom: thin solid rgb(0, 0, 0); font-size: 10pt;"
 align="right" valign="bottom">5</td>
      <td
 style="border-top: thin solid rgb(0, 0, 0); border-left: thin solid rgb(0, 0, 0); border-bottom: thin solid rgb(0, 0, 0); font-size: 10pt;"
 align="right" valign="bottom">5</td>
      <td
 style="border-top: thin solid rgb(0, 0, 0); border-left: thin solid rgb(0, 0, 0); border-bottom: thin solid rgb(0, 0, 0); font-size: 10pt;"
 align="right" valign="bottom">5</td>
      <td
 style="border-top: thin solid rgb(0, 0, 0); border-left: thin solid rgb(0, 0, 0); border-bottom: thin solid rgb(0, 0, 0); font-size: 10pt;"
 align="right" valign="bottom">5</td>
      <td
 style="border-top: thin solid rgb(0, 0, 0); border-left: thin solid rgb(0, 0, 0); border-bottom: thin solid rgb(0, 0, 0); font-size: 10pt;"
 align="right" valign="bottom">5</td>
      <td
 style="border-top: thin solid rgb(0, 0, 0); border-left: thin solid rgb(0, 0, 0); border-bottom: thin solid rgb(0, 0, 0); font-size: 10pt;"
 align="right" valign="bottom">5</td>
      <td
 style="border-top: thin solid rgb(0, 0, 0); border-left: thin solid rgb(0, 0, 0); border-bottom: thin solid rgb(0, 0, 0); font-size: 10pt;"
 align="right" valign="bottom">5</td>
      <td
 style="border-top: thin solid rgb(0, 0, 0); border-left: thin solid rgb(0, 0, 0); border-bottom: thin solid rgb(0, 0, 0); font-size: 10pt;"
 align="right" valign="bottom">5</td>
      <td style="border: thin solid rgb(0, 0, 0); font-size: 10pt;"
 align="right" valign="bottom">5</td>
    </tr>
  </tbody>
</table>
 *
 *
 * <br><br>
 * And here is the table of actions w/ descriptions
<table cellpadding="3" cellspacing="0">
  <tbody>
    <tr>
      <td
 style="border-top: thin solid rgb(0, 0, 0); border-left: thin solid rgb(0, 0, 0); font-size: 10pt;"
 align="left" valign="bottom">Action</td>
      <td
 style="border-top: thin solid rgb(0, 0, 0); border-left: thin solid rgb(0, 0, 0); border-right: thin solid rgb(0, 0, 0); font-size: 10pt;"
 align="left" valign="bottom">Desc</td>
    </tr>
    <tr>
      <td
 style="border-top: thin solid rgb(0, 0, 0); border-left: thin solid rgb(0, 0, 0); font-size: 10pt;"
 align="right" valign="bottom">0</td>
      <td
 style="border-top: thin solid rgb(0, 0, 0); border-left: thin solid rgb(0, 0, 0); border-right: thin solid rgb(0, 0, 0); font-size: 10pt;"
 align="left" valign="bottom">Assert (illegal)</td>
    </tr>
    <tr>
      <td
 style="border-top: thin solid rgb(0, 0, 0); border-left: thin solid rgb(0, 0, 0); font-size: 10pt;"
 align="right" valign="bottom">1</td>
      <td
 style="border-top: thin solid rgb(0, 0, 0); border-left: thin solid rgb(0, 0, 0); border-right: thin solid rgb(0, 0, 0); font-size: 10pt;"
 align="left" valign="bottom">If line_word_count &lt; MAX, no change in
state. Otherwise, change state to s5 ("Look for new line") and reset
line_word_count</td>
    </tr>
    <tr>
      <td
 style="border-top: thin solid rgb(0, 0, 0); border-left: thin solid rgb(0, 0, 0); font-size: 10pt;"
 align="right" valign="bottom">2</td>
      <td
 style="border-top: thin solid rgb(0, 0, 0); border-left: thin solid rgb(0, 0, 0); border-right: thin solid rgb(0, 0, 0); font-size: 10pt;"
 align="left" valign="bottom">Change to s4 ("Saw FETCH")</td>
    </tr>
    <tr>
      <td
 style="border-top: thin solid rgb(0, 0, 0); border-left: thin solid rgb(0, 0, 0); font-size: 10pt;"
 align="right" valign="bottom">3</td>
      <td
 style="border-top: thin solid rgb(0, 0, 0); border-left: thin solid rgb(0, 0, 0); border-right: thin solid rgb(0, 0, 0); font-size: 10pt;"
 align="left" valign="bottom">Push current state, change to "Skipping
Literal" (s1). Increment line_word_count</td>
    </tr>
    <tr>
      <td
 style="border-top: thin solid rgb(0, 0, 0); border-left: thin solid rgb(0, 0, 0); font-size: 10pt;"
 align="right" valign="bottom">4</td>
      <td
 style="border-top: thin solid rgb(0, 0, 0); border-left: thin solid rgb(0, 0, 0); border-right: thin solid rgb(0, 0, 0); font-size: 10pt;"
 align="left" valign="bottom">No change in state</td>
    </tr>
    <tr>
      <td
 style="border-top: thin solid rgb(0, 0, 0); border-left: thin solid rgb(0, 0, 0); font-size: 10pt;"
 align="right" valign="bottom">5</td>
      <td
 style="border-top: thin solid rgb(0, 0, 0); border-left: thin solid rgb(0, 0, 0); border-right: thin solid rgb(0, 0, 0); font-size: 10pt;"
 align="left" valign="bottom">Reset line_word_count, change state to s6
("Saw 'FETCH' (")</td>
    </tr>
    <tr>
      <td
 style="border-top: thin solid rgb(0, 0, 0); border-left: thin solid rgb(0, 0, 0); font-size: 10pt;"
 align="right" valign="bottom">6</td>
      <td
 style="border-top: thin solid rgb(0, 0, 0); border-left: thin solid rgb(0, 0, 0); border-right: thin solid rgb(0, 0, 0); font-size: 10pt;"
 align="left" valign="bottom">Change state to s3 ("new line"). Reset
line_word_count</td>
    </tr>
    <tr>
      <td
 style="border-top: thin solid rgb(0, 0, 0); border-left: thin solid rgb(0, 0, 0); font-size: 10pt;"
 align="right" valign="bottom">7</td>
      <td
 style="border-top: thin solid rgb(0, 0, 0); border-left: thin solid rgb(0, 0, 0); border-right: thin solid rgb(0, 0, 0); font-size: 10pt;"
 align="left" valign="bottom">Change state to s7 ("saw
'FETCH...(...BODY'")</td>
    </tr>
    <tr>
      <td
 style="border-top: thin solid rgb(0, 0, 0); border-left: thin solid rgb(0, 0, 0); font-size: 10pt;"
 align="right" valign="bottom">8</td>
      <td
 style="border-top: thin solid rgb(0, 0, 0); border-left: thin solid rgb(0, 0, 0); border-right: thin solid rgb(0, 0, 0); font-size: 10pt;"
 align="left" valign="bottom">Change state to s8 ("saw
'FETCH...(...RFC822")</td>
    </tr>
    <tr>
      <td
 style="border-top: thin solid rgb(0, 0, 0); border-left: thin solid rgb(0, 0, 0); font-size: 10pt;"
 align="right" valign="bottom">9</td>
      <td
 style="border-top: thin solid rgb(0, 0, 0); border-left: thin solid rgb(0, 0, 0); border-right: thin solid rgb(0, 0, 0); font-size: 10pt;"
 align="left" valign="bottom">Push Current state, change state to s1
("Skipping Literal")</td>
    </tr>
    <tr>
      <td
 style="border-top: thin solid rgb(0, 0, 0); border-left: thin solid rgb(0, 0, 0); font-size: 10pt;"
 align="right" valign="bottom">10</td>
      <td
 style="border-top: thin solid rgb(0, 0, 0); border-left: thin solid rgb(0, 0, 0); border-right: thin solid rgb(0, 0, 0); font-size: 10pt;"
 align="left" valign="bottom">Push s6 state as next, change to s1
(skipping literal).</td>
    </tr>
    <tr>
      <td
 style="border-top: thin solid rgb(0, 0, 0); border-left: thin solid rgb(0, 0, 0); font-size: 10pt;"
 align="right" valign="bottom">11</td>
      <td
 style="border-top: thin solid rgb(0, 0, 0); border-left: thin solid rgb(0, 0, 0); border-right: thin solid rgb(0, 0, 0); font-size: 10pt;"
 align="left" valign="bottom">Change state to s9 ("saw
'FETCH...(...BODY[")</td>
    </tr>
    <tr>
      <td
 style="border-top: thin solid rgb(0, 0, 0); border-left: thin solid rgb(0, 0, 0); font-size: 10pt;"
 align="right" valign="bottom">12</td>
      <td
 style="border-top: thin solid rgb(0, 0, 0); border-left: thin solid rgb(0, 0, 0); border-right: thin solid rgb(0, 0, 0); font-size: 10pt;"
 align="left" valign="bottom">Change state to s11 (If next token is
"PEEK", change state to s7, otherwise assume failed "BODY")</td>
    </tr>
    <tr>
      <td
 style="border-top: thin solid rgb(0, 0, 0); border-left: thin solid rgb(0, 0, 0); font-size: 10pt;"
 align="right" valign="bottom">13</td>
      <td
 style="border-top: thin solid rgb(0, 0, 0); border-left: thin solid rgb(0, 0, 0); border-right: thin solid rgb(0, 0, 0); font-size: 10pt;"
 align="left" valign="bottom">Found Message (change state to s2)</td>
    </tr>
    <tr>
      <td
 style="border-top: thin solid rgb(0, 0, 0); border-left: thin solid rgb(0, 0, 0); font-size: 10pt;"
 align="right" valign="bottom">14</td>
      <td
 style="border-top: thin solid rgb(0, 0, 0); border-left: thin solid rgb(0, 0, 0); border-right: thin solid rgb(0, 0, 0); font-size: 10pt;"
 align="left" valign="bottom">Change state to s12 ("Skip current token,
then change state to s5")</td>
    </tr>
    <tr>
      <td
 style="border-top: thin solid rgb(0, 0, 0); border-left: thin solid rgb(0, 0, 0); border-bottom: thin solid rgb(0, 0, 0); font-size: 10pt;"
 align="right" valign="bottom">15</td>
      <td style="border: thin solid rgb(0, 0, 0); font-size: 10pt;"
 align="left" valign="bottom">Change state to s10 ("saw
'FETCH...(...BODY[]'")</td>
    </tr>
  </tbody>
</table> 
 */
public class ImapServerParser
  extends ImapParser {

  /**
   * State of the parser (*outer* parser)
   */
  private enum ISPState {
    SCANNING,
    DRAINING_HEADERS,
    DRAINING_BODY,
    DRAINING_HOSED,
  };
  
  private final Logger m_logger =
    Logger.getLogger(ImapServerParser.class);

  private final IMAPTokenizer m_tokenizer;
  private IMAPBodyScanner m_msgBoundaryScanner;
  private ISPState m_state = ISPState.SCANNING;
  private MessageGrabber m_msgGrabber;

    

  ImapServerParser(TCPSession session,
    ImapCasing parent) {
    
    super(session, parent, false);
    lineBuffering(false); // XXX line buffering

    m_tokenizer = new IMAPTokenizer();
    m_msgBoundaryScanner = new IMAPBodyScanner();
    
    m_logger.debug("Created");
  }


  public ParseResult parse(ByteBuffer buf) {

    ByteBuffer dup = buf.duplicate();

    //Perform the parse
    ParseResult ret = parseImpl(buf);

    if(ret.getReadBuffer() != null) {
      //Trace what was not pushed-back.  We know that
      //the position of the returned read buffer is equal
      //to the tail of the original buffer not
      //consumed.
      dup.limit(dup.limit() - ret.getReadBuffer().position());
    }
    
    //do tracing stuff
    getImapCasing().traceParse(dup);
    
    return ret;
  }
  
  private ParseResult parseImpl(ByteBuffer buf) {


    //TEMP - so folks don't hit my unfinished code by accident
//    if(System.currentTimeMillis() > 0) {
//      getImapCasing().traceParse(buf);
//      return new ParseResult(new Chunk(buf));
//    }


    //Check for passthru
    if(isPassthru()) {
      return new ParseResult(new Chunk(buf));
    }

    List<Token> toks = new LinkedList<Token>();

    //Get SessionMonitor to inspect bytes.  Its OK that we do not
    //align on token boundaries (as we do with the ClientParser),
    //because the tokenizing for the message body will cause
    //the return (in the read buffer) of any word-fragments.
    if(getImapCasing().getSessionMonitor().bytesFromServer(buf.duplicate())) {
      m_logger.debug("Declare passthru as-per advice of SessionMonitor");
      declarePassthru();
      toks.add(PassThruToken.PASSTHRU);
      toks.add(new Chunk(buf));
      return new ParseResult(toks, null);
    }
  
    ByteBuffer dup = null;
    

    while(buf.hasRemaining()) {
      switch(m_state) {
        //===================================================
        case SCANNING:
          dup = buf.duplicate();
          if(m_msgBoundaryScanner.scanForMsgState(buf)) {
            m_logger.debug("Found message boundary start. Octet count: " +
              m_msgBoundaryScanner.getMessageOctetCount());
            
            //First off, we need to put into the returned token
            //list a Chunk with the bytes which were NOT the
            //message
            dup.limit(buf.position());
            rewindLiteral(dup);
            m_logger.debug("Adding protocol chunk of length " + dup.remaining());
            toks.add(new ImapChunk(dup));

            //Open-up the message grabber
            if(!openMessageGrabber(m_msgBoundaryScanner.getMessageOctetCount())) {
              m_logger.warn("Message will be bypassed because of error opening accumulator");
              changeParserState(ISPState.DRAINING_HOSED);
            }
            else {
              changeParserState(ISPState.DRAINING_HEADERS);
            }
            break;
          }
          else {
            m_logger.debug("We need more data in SCANNING state");
            dup.limit(buf.position());
            m_logger.debug("Adding protocol chunk of length " + dup.remaining());
            toks.add(new ImapChunk(dup));
            return returnWithCompactedBuffer(buf, toks);
          }
          
        //===================================================          
        case DRAINING_HEADERS:
          //Determine how much of this buffer we will consider as
          //message data
          dup = getNextBodyChunkBuffer(buf);

          boolean foundEndOfHeaders =
            m_msgGrabber.scanner.processHeaders(dup, 1024*4);//TODO bscott a real value here

          //Adjust buffers, such that the HeaderBytes are accounted-for
          //in "dup" and the original buffer is advanced past what we
          //accounted-for in "dup"
          int headersEnd = dup.position();
          dup.limit(headersEnd);
          dup.position(buf.position());
          buf.position(headersEnd);

          //Decrement the amount of message "read"
          m_msgGrabber.decrementMsgRemaining(dup.remaining());

          if(m_msgGrabber.scanner.isHeadersBlank()) {
            m_logger.debug("Headers are blank. " +
              m_msgGrabber.getMsgRemaining() + " msg bytes remain");
          }
          else {
            m_logger.debug("About to write the " +
              (foundEndOfHeaders?"last":"next") + " " +
              dup.remaining() + " header bytes to disk. " +
              m_msgGrabber.getMsgRemaining() + " msg bytes remain");
          }

          //Write what we have to disk.
          if(!m_msgGrabber.accumulator.addHeaderBytes(dup, foundEndOfHeaders)) {
            m_logger.error("Unable to write header bytes to disk.  Punt on this message");
            
            //Grab anything trapped thus far in the file (if we can
            recoverTrappedHeaderBytes(toks);
            
            //Add the chunk we could not write to file
            toks.add(new UnparsableMIMEChunk(dup));
            
            if(m_msgGrabber.hasMsgRemaining()) {
              changeParserState(ISPState.DRAINING_HOSED);
            }
            else {
              m_msgGrabber = null;
              changeParserState(ISPState.SCANNING);
            }
            break;
          }

          if(foundEndOfHeaders) {//BEGIN End of Headers
            MIMEMessageHeaders headers = m_msgGrabber.accumulator.parseHeaders();
            if(headers == null) {//BEGIN Header PArse Error
              m_logger.error("Unable to parse headers.  Pass accumulated " +
                "bytes as a normal chunk, and passthru rest of message");
              //Grab anything trapped thus far in the file (if we can
              recoverTrappedHeaderBytes(toks);

              //Figure out next state (in case that was the end of the message as well)
              if(m_msgGrabber.hasMsgRemaining()) {
                changeParserState(ISPState.DRAINING_HOSED);
              }
              else {
                m_msgGrabber = null;
                changeParserState(ISPState.SCANNING);
              }
              break;                             
            }//ENDOF Header PArse Error
            else {//BEGIN Headers parsed
              m_logger.debug("Adding the BeginMIMEToken");
              toks.add(
                new BeginImapMIMEToken(
                  m_msgGrabber.accumulator,
                  createMessageInfo(headers),
                  m_msgGrabber.getTotalMessageLength())
                );
              m_msgGrabber.noLongerAccumulatorMaster();
              changeParserState(ISPState.DRAINING_BODY);
  
              //Check for an empty body
              if(m_msgGrabber.scanner.isEmptyMessage()) {
                m_logger.debug("Message blank.  Complete message tokens.");
                toks.add(new ContinuedMIMEToken(m_msgGrabber.accumulator.createChunk(null, true)));
                changeParserState(ISPState.SCANNING);
                m_msgGrabber = null;
              }
            }//ENDOF Headers parsed
          }//ENDOF End of Headers
          else {
            m_logger.debug("Need more header bytes");
            return returnWithCompactedBuffer(buf, toks);
          }
          break;
          
        //===================================================          
        case DRAINING_HOSED:
          dup = getNextBodyChunkBuffer(buf);
          m_logger.debug("Adding passthru body chunk of length " + dup.remaining());
          toks.add(new UnparsableMIMEChunk(dup));

          //Advance the buf past what we just transferred
          buf.position(buf.position() + dup.remaining());

          m_msgGrabber.decrementMsgRemaining(dup.remaining());

          if(!m_msgGrabber.hasMsgRemaining()) {
            m_logger.debug("Found message end");
            changeParserState(ISPState.SCANNING);
            m_msgGrabber.accumulator.dispose();//Redundant
            m_msgGrabber = null;
          }          
          break;
          
        //===================================================          
        case DRAINING_BODY:

          MIMEAccumulator.MIMEChunk mimeChunk = null;
        
          if(m_msgGrabber.hasMsgRemaining()) {
          
            dup = getNextBodyChunkBuffer(buf);
            m_msgGrabber.decrementMsgRemaining(dup.remaining());
                        
            m_logger.debug("Next body chunk of length " +
              dup.remaining() + ", " + m_msgGrabber.getMsgRemaining() +
              " message bytes remaining");
          
            buf.position(buf.position() + dup.remaining());
            
            if(m_msgGrabber.hasMsgRemaining()) {
              m_logger.debug("Adding continued body chunk of length " + dup.remaining());
              mimeChunk = m_msgGrabber.accumulator.createChunk(dup.slice(), false);              
            }
            else {
              m_logger.debug("Adding final body chunk of length " + dup.remaining());
              mimeChunk = m_msgGrabber.accumulator.createChunk(dup.slice(), true);
              changeParserState(ISPState.SCANNING);
              m_msgGrabber = null;          
            }
          }
          else {
            m_logger.debug("Adding terminal chunk (no data)");
            mimeChunk = m_msgGrabber.accumulator.createChunk(null, true);
            changeParserState(ISPState.SCANNING);
            m_msgGrabber = null;          
          }
          toks.add(new ContinuedMIMEToken(mimeChunk));
          break;          
      }
    }
    //The only way to get here is an empty buffer.  I think
    //this can only happen if the opening of a message is found at the end
    //of a packet (or a complete body was found in one packet.  Eitherway,
    //since the buffer is empty we don't worry about any remaining
    //chunks.
    m_logger.debug("Buffer empty.  Return tokens");
    return new ParseResult(toks);
  }

  @Override
  public TokenStreamer endSession() {
    m_logger.debug("End Session");
    return super.endSession();
  }  

  public ParseResult parseEnd(ByteBuffer buf) {
    Chunk c = new Chunk(buf);

    m_logger.debug(this + " passing chunk of size: " + buf.remaining());
    return new ParseResult(c);
  }



  /**
   * Helper which rewinds the buffer to the position *just before*
   * a literal
   */
  private void rewindLiteral(ByteBuffer buf) {
    for(int i = buf.limit()-1; i>=buf.position(); i--) {
      if(buf.get(i) == OPEN_BRACE_B) {
        buf.limit(i);
        return;
      }    
    }
    throw new RuntimeException("No \"{\" found to rewind-to");
  }

  private void changeParserState(ISPState state) {
    if(m_state != state) {
      m_logger.debug("Change state from \"" +
        m_state + "\" to \"" + state + "\"");
      m_state = state;
    }
    
  }  

  /**
   * Helper method which duplicates buf with as-many body bytes as are appropriate
   * based on the number of bytes remaining and the size of buf.  Does <b>not</b>
   * advance "buf" to account for the transferred bytes.
   */
  private ByteBuffer getNextBodyChunkBuffer(ByteBuffer buf) {
    ByteBuffer dup = buf.duplicate();
    dup.limit(dup.position() +
      (m_msgGrabber.getMsgRemaining() > buf.remaining()?
        buf.remaining():
        m_msgGrabber.getMsgRemaining()));
    return dup;
  }
  
  /**
   * Re-used logic broken-out to simplify "parse" method
   */
  private void recoverTrappedHeaderBytes(List<Token> toks) {
    //Grab anything trapped thus far in the file (if we can
    ByteBuffer trapped = m_msgGrabber.accumulator.drainFileToByteBuffer();
    m_logger.debug("Close accumulator");
    m_msgGrabber.accumulator.dispose();
  
    if(trapped == null) {
      m_logger.debug("Could not recover buffered header bytes");
    }
    else {
      m_logger.debug("Retreived " + trapped.remaining() + " bytes trapped in file");
      toks.add(new UnparsableMIMEChunk(trapped));
    }
  }
  
  /**
   * Helper method removing duplication in parse method
   */
  private ParseResult returnWithCompactedBuffer(ByteBuffer buf,
    List<Token> toks) {
    buf = compactIfNotEmpty(buf, m_tokenizer.getLongestWord());
    m_logger.debug("Returning " + toks.size() + " tokens and a " +
      (buf==null?"null buffer":"buffer at position " + buf.position()));
    return new ParseResult(toks, buf);
  }

  /**
   * Open the MessageGrabber.  False is returned if the underlying
   * MIMEAccumulator cannot be opened (however the message grabber
   * is still created, to track the length of message consumed).
   */
  private boolean openMessageGrabber(int totalMsgLen) {
    try {
      m_msgGrabber = new MessageGrabber(totalMsgLen,
        new MIMEAccumulator(getPipeline()));
      return true;
    }
    catch(IOException ex) {
      m_logger.error("Exception creating MIME Accumulator", ex);
      m_msgGrabber = new MessageGrabber(totalMsgLen,
        null);
      return false;
    }    
  }  

  /**
   * Helper method to break-out the
   * creation of a MessageInfo
   */
  private MessageInfo createMessageInfo(MIMEMessageHeaders headers) {

    if(headers == null) {
      headers = new MIMEMessageHeaders();
    }
  
    MessageInfo ret = MessageInfoFactory.fromMIMEMessage(headers,
      getSession().id(),
      getSession().serverPort());

    
    String username = getImapCasing().getSessionMonitor().getUserName();
    if(username == null) {
      username = "UNKNOWN";
      m_logger.debug("Unable to determine client login name.  Use \"" +
        username + "\" instead");
    }
    ret.addAddress(AddressKind.USER, username, null);

    return ret;
  }

  /**
   * Little class to associate all
   * state with grabbing a message
   */
  private class MessageGrabber {
  
    final MessageBoundaryScanner scanner;
    final MIMEAccumulator accumulator;
    private boolean m_isMasterOfAccumulator = true;
    private final int m_totalMsgLen;
    private int m_msgReadSoFar;

    MessageGrabber(int msgLength,
      MIMEAccumulator accumulator) {
      scanner = new MessageBoundaryScanner();
      this.accumulator = accumulator;
      this.m_totalMsgLen = msgLength;
      m_msgReadSoFar = 0;
      
    }
    int getTotalMessageLength() {
      return m_totalMsgLen;
    }
    boolean isAccumulatorHosed() {
      return accumulator == null;
    }
    boolean hasMsgRemaining() {
      return getMsgRemaining() > 0;
    }
    int getMsgRemaining() {
      return m_totalMsgLen - m_msgReadSoFar;
    }
    void decrementMsgRemaining(int amt) {
      m_msgReadSoFar+=amt;
    }
    boolean isMasterOfAccumulator() {
      return m_isMasterOfAccumulator;
    }
    /**
     * Called when we have passed-along the accumulator
     * (in a "BeginMIMEToken").
     */
    void noLongerAccumulatorMaster() {
      m_isMasterOfAccumulator = false;
    }
  }   



//================================
// Constants for
// IMAPBodyScanner inner
// class (since inner classes
// cannot have static finals)

  private static final int S01= 0;//Skipping Literal
  private static final int S02= 1;//Draining Body
  private static final int S03= 2;//Scanning New Line
  private static final int S04= 3;//Saw "FETCH"
  private static final int S05= 4;//Skipping to end of line
  private static final int S06= 5;//Saw "FETCH .. ("
  private static final int S07= 6;//Saw "FETCH...(...BODY"
  private static final int S08= 7;//Saw "FETCH...(...RFC822"
  private static final int S09= 8;//Saw "FETCH .. (...BODY["
  private static final int S10= 9;//Saw "FETCH .. (...BODY[]"
  private static final int S11=10;//Checking for "BODY.PEEK"
  private static final int S12=11;//Skipping current Token, then going to s5 or s6

  private static final String[] STATE_STRINGS = {
    "Skipping Literal",
    "Draining Body",
    "Scanning New Line",
    "Saw \"FETCH\"",
    "Skipping to end of line",
    "Saw \"FETCH .. (\"",
    "Saw \"FETCH...(...BODY\"",
    "Saw \"FETCH...(...RFC822\"",
    "Saw \"FETCH .. (...BODY[\"",
    "Saw \"FETCH .. (...BODY[]\"",
    "Checking for \"BODY.PEEK\"",
    "Skipping current Token, then going to s6"
  };
    

  private static final int T01 =  0;//EOL
  private static final int T02 =  1;//"FETCH"
  private static final int T03 =  2;//"BODY"
  private static final int T04 =  3;//"RFC822"
  private static final int T05 =  4;//"PEEK"
  private static final int T06 =  5;//(word)
  private static final int T07 =  6;//(qstr)
  private static final int T08 =  7;//(literal)
  private static final int T09 =  8;//OB "["
  private static final int T10 =  9;//CB "]"
  private static final int T11 = 10;//LT "<"
  private static final int T12 = 11;//Paren "("
  private static final int T13 = 12;//Dot "."
  private static final int T14 = 13;//(delim)
  private static final int T15 = 14;//SYNTHETIC TOKEN FOR LOGGING (meaning, previous was a literal)
  private static final int T16 = 15;//SYNTHETIC TOKEN FOR LOGGING (meaning, previous was a msg)
  private static final int T17 = 16;//SYNTHETIC TOKEN FOR LOGGING (meaning, initial state)

  private static final String[] TOKEN_STRINGS = {
    "EOL",
    "\"FETCH\"",
    "\"BODY\"",
    "\"RFC822\"",
    "\"PEEK\"",
    "(word)",
    "(qstr)",
    "(literal)",
    "OB \"[\"",
    "CB \"]\"",
    "LT \"<\"",
    "Paren \"(\"",
    "Dot \".\"",
    "(delim)",
    "previous was a literal",
    "previous was a msg",
    "initial state"
  };

  private static final int A00 =  0;
  private static final int A01 =  1;
  private static final int A02 =  2;
  private static final int A03 =  3;
  private static final int A04 =  4;
  private static final int A05 =  5;
  private static final int A06 =  6;
  private static final int A07 =  7;
  private static final int A08 =  8;
  private static final int A09 =  9;
  private static final int A10 = 10;
  private static final int A11 = 11;
  private static final int A12 = 12;
  private static final int A13 = 13;
  private static final int A14 = 14;
  private static final int A15 = 15;

  private static final int[][] TRAN_TBL = {
    {A00, A00, A00, A00, A00, A00, A00, A00, A00, A00, A00, A00, A00, A00},
    {A00, A00, A00, A00, A00, A00, A00, A00, A00, A00, A00, A00, A00, A00},
    {A04, A02, A01, A01, A01, A01, A01, A03, A04, A04, A04, A04, A04, A04},
    {A06, A04, A04, A04, A04, A04, A04, A09, A04, A04, A04, A05, A04, A04},
    {A06, A04, A04, A04, A04, A04, A04, A09, A04, A04, A04, A04, A04, A04},
    {A06, A04, A07, A08, A04, A04, A04, A09, A04, A04, A04, A04, A04, A04},
    {A06, A05, A04, A08, A05, A05, A05, A10, A11, A05, A05, A05, A12, A05},
    {A06, A05, A07, A04, A05, A05, A05, A13, A05, A05, A05, A05, A14, A05},
    {A06, A14, A14, A14, A14, A14, A14, A10, A05, A15, A05, A05, A14, A05},
    {A06, A06, A07, A08, A05, A05, A05, A13, A05, A05, A05, A05, A05, A05},
    {A06, A05, A05, A05, A07, A05, A05, A10, A05, A05, A05, A05, A05, A05},
    {A06, A05, A05, A05, A05, A05, A05, A05, A05, A05, A05, A05, A05, A05}
  };

  private static final int MAX_TOKENS_BEFORE_FETCH = 8;
  private static final byte[] FETCH_BYTES = "fetch".getBytes();
  private static final byte[] BODY_BYTES = "body".getBytes();
  private static final byte[] RFC822_BYTES = "rfc822".getBytes();
  private static final byte[] PEEK_BYTES = "peek".getBytes();
  
  class IMAPBodyScanner {

    private int m_lineWordCount;
    private int m_toSkipLiteral;    
    private int m_state = S03;
    private int m_msgLength = -1;
    private int m_pushedStateForLiteral = -1;
    private Logger m_logger =
      Logger.getLogger(ImapServerParser.IMAPBodyScanner.class);

    IMAPBodyScanner() {
      changeState(S03, T17);
    }

    private void changeState(int newState,
      int tokenClass) {
      if(newState != m_state) {
        m_logger.debug("Change state from \"" +
          STATE_STRINGS[m_state] +
          "\" to \"" +
          STATE_STRINGS[newState] +
          "\" on token \"" +
          TOKEN_STRINGS[tokenClass] + "\"");
        m_state = newState;
      }
    }

    int getMessageOctetCount() {
      return m_msgLength;
    }

    /**
     * If true is returned, then the caller <b>must</b> make sure
     * to rewind the buffer such that the literal declaration is
     * not sent to the client as part of the stuff we were
     * scanning before we found the message.
     */
    boolean scanForMsgState(ByteBuffer buf) {

      //Reset the message length, as it never caries
      //over
      if(m_state == S02) {
        m_msgLength = -1;
        changeState(S05, T16);
        m_lineWordCount = 0;
      }
      
      while(buf.hasRemaining()) {
        //Before we tokenize into a literal by-accident,
        //handle literal draining first
        if(m_state == S01) {
          //Skipping literal
          int thisSkip = buf.remaining()>m_toSkipLiteral?
            m_toSkipLiteral:buf.remaining();
          m_logger.debug("Continuing to skip next: " + thisSkip + " bytes");
          buf.position(buf.position() + thisSkip);
          m_toSkipLiteral-=thisSkip;
          if(m_toSkipLiteral == 0) {
            if(m_pushedStateForLiteral == -1) {
              throw new RuntimeException("Draining literal without next state");
            }
            changeState(m_pushedStateForLiteral, T15);
            m_pushedStateForLiteral = -1;
          }
          continue;
        }

        //From here, the states "S01" and "S02" are illegal
        
        //Now, get the next result
        switch(m_tokenizer.next(buf)) {
          case EXCEEDED_LONGEST_WORD:
            m_logger.debug("Exceeded Longest Word.  Skip past whole buffer");
            buf.position(buf.limit());
            return false;
          case NEED_MORE_DATA:
            m_logger.debug("Need more data");
            return false;
        }

        //Falling-out of that switch is equivilant
        //to the "HAVE_TOKEN:" case.  Now classify the token
        int tokenClass = -1;
        switch(m_tokenizer.getTokenType()) {
          case WORD:
            if(m_tokenizer.compareWordAgainst(buf, FETCH_BYTES, true)) {
              tokenClass = T02;
            }
            else if(m_tokenizer.compareWordAgainst(buf, BODY_BYTES, true)) {
              tokenClass = T03;
            }
            else if(m_tokenizer.compareWordAgainst(buf, RFC822_BYTES, true)) {
              tokenClass = T04;
            }
            else if(m_tokenizer.compareWordAgainst(buf, PEEK_BYTES, true)) {
              tokenClass = T05;
            }            
            else {
              tokenClass = T06;
            }
            break;
          case QSTRING:
            tokenClass = T07;
            break;
          case LITERAL:
            tokenClass = T08;
            break;
          case CONTROL_CHAR:
            if(buf.get(m_tokenizer.getTokenStart()) == OPEN_BRACKET_B) {
              tokenClass = T09;
            }
            else if(buf.get(m_tokenizer.getTokenStart()) == CLOSE_BRACKET_B) {
              tokenClass = T10;
            }
            else if(buf.get(m_tokenizer.getTokenStart()) == LT_B) {
              tokenClass = T11;
            }
            else if(buf.get(m_tokenizer.getTokenStart()) == OPEN_PAREN_B) {
              tokenClass = T12;
            }
            else if(buf.get(m_tokenizer.getTokenStart()) == PERIOD_B) {
              tokenClass = T13;
            }
            else {
              tokenClass = T14;
            }
            break;
          case NEW_LINE:
            tokenClass = T01;
            break;
          default:
            throw new RuntimeException("Unexpected token type: " + m_tokenizer.getTokenType());
        }


        //Now, index into our function table for what to do based
        //on current state and the token class
        switch(TRAN_TBL[m_state][tokenClass]) {
          //================================        
          case A00:
            //Assert (illegal)
            throw new RuntimeException("Illegal state right now (" + m_state + ")");
          //================================
          case A01:
            //If line_word_count < MAX, no change in state.
            //Otherwise, change state to s5 ("Look for new line") and reset line_word_count
            if(++m_lineWordCount > MAX_TOKENS_BEFORE_FETCH) {
              changeState(S05, tokenClass);
              m_lineWordCount = 0;
            }
            break;
          //================================
          case A02:
            //Change to s4 ("Saw FETCH")
            changeState(S04, tokenClass);
            break;
          //================================
          case A03:
            //Push current state, change to "Skipping Literal" (s1).  Increment line_word_count
            m_pushedStateForLiteral = m_state;
            m_state = S01;
            m_lineWordCount++;
            break;
          //================================
          case A04:
            //No change in state
            break;
          //================================
          case A05:
            //Reset line_word_count, change state to s6 ("Saw 'FETCH' (")
            m_lineWordCount = 0;
            changeState(S06, tokenClass);
            break;
          //================================
          case A06:
            //Change state to s3 ("new line").  Reset line_word_count
            m_lineWordCount = 0;
            changeState(S03, tokenClass);            
            break;
          //================================
          case A07:
            //Change state to s7 ("saw 'FETCH...(...BODY'")
            changeState(S07, tokenClass);
            break;
          //================================
          case A08:
            //Change state to s8 ("saw 'FETCH...(...RFC822")
            changeState(S08, tokenClass);
            break;
          //================================
          case A09:
            //Push Current state, change state to s1 ("Skipping Literal")
            m_pushedStateForLiteral = m_state;
            m_state = S01;
            break;
          //================================
          case A10:
            //Push s6 state as next, change to s1 (skipping literal).
            m_pushedStateForLiteral = S06;
            changeState(S01, tokenClass);
            break;
          //================================
          case A11:
            //Change state to s9 ("saw 'FETCH...(...BODY[")
            changeState(S09, tokenClass);
            break;
          //================================
          case A12:
            //Change state to s11 (If next token is "PEEK",
            //change state to s7, otherwise assume failed "BODY")
            changeState(S11, tokenClass);
            break;
          //================================
          case A13:
            //Found Message (change state to s2)
            m_logger.debug("Found body declaration");
            m_msgLength = m_tokenizer.getLiteralOctetCount();
            changeState(S02, tokenClass);
            return true;            
          //================================
          case A14:
            //Change state to s12 ("Skip current
            //token, then change state to s5 or s6")
            changeState(S12, tokenClass);
            break;
          //================================
          case A15:
            //Change state to s10 ("saw 'FETCH...(...BODY[]'")
            changeState(S10, tokenClass);
            break;
          default:
            throw new RuntimeException("Unknown action");
        }
      }
      return false;
    }
  }
}
