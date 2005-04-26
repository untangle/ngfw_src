/*
 * Copyright (c) 2004 Metavize Inc.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Metavize Inc. ("Confidential Information").  You shall
 * not disclose such Confidential Information.
 *
 * $Id$
 */
package com.metavize.tran.email;

import java.io.*;
import java.nio.*;
import java.nio.charset.*;
import java.util.*;
import java.util.regex.*;
import org.apache.log4j.Logger;

import com.metavize.mvvm.tapi.*;
import com.metavize.mvvm.tran.*;
import com.metavize.tran.util.*;

public class XMailScannerCache
{
    class ScannerOptions
    {
        private int iOptions; /* signed, 32-bit value */

        public ScannerOptions()
        {
            iOptions = 0;
        }

        public void set(int iType)
        {
            iOptions |= iType;
            return;
        }

        public void set(int iType, boolean bVal)
        {
            if (true == bVal)
            {
                iOptions |= iType;
            }
            return;
        }

        public boolean get(int iType)
        {
            return (iType == (iOptions & iType)) ? true : false;
        }
    }

    /* constants */
    private final static Logger zLog = Logger.getLogger(XMailScannerCache.class.getName());

    /* pattern to action mapping options (mutually exclusive)
     * FIRST2FIRST = 1st true pattern maps to action of 1st true pattern
     * FIRST2LAST = 1st true pattern maps to action of last true pattern
     * LAST2FIRST = last true pattern maps to action of 1st true pattern
     * LAST2LAST = last true pattern maps to action of last true pattern
     */
    public final static int FIRST2FIRST = PatternMap.FIRST2FIRST;
    public final static int FIRST2LAST = PatternMap.FIRST2LAST;
    public final static int LAST2FIRST = PatternMap.LAST2FIRST;
    public final static int LAST2LAST = PatternMap.LAST2LAST;

    /* default options (mutually exclusive)
     * if no pattern matches to true, then:
     * DEFNONE = no default action
     * DEFFIRST = default to 1st action
     * DEFLAST = default to last action
     */
    public final static int DEFNONE = PatternMap.DEFNONE;
    public final static int DEFFIRST = PatternMap.DEFFIRST;
    public final static int DEFLAST = PatternMap.DEFLAST;

    public final static Integer NOTRUEACTION = new Integer(PatternMap.NO_TRUE_ACTION);

    private final static byte POP3PMBA[] = { 'P', 'O', 'P', '3', ' ', 'P', 'o', 's', 't', 'm', 'a', 's', 't', 'e', 'r', ' ', '<', 'p', 'o', 's', 't', 'm', 'a', 's', 't', 'e', 'r', '@', 'l', 'o', 'c', 'a', 'l', 'h', 'o', 's', 't', '.', 'c', 'o', 'm', '>' };
    private final static byte IMAP4PMBA[] = { 'I', 'M', 'A', 'P', '4', ' ', 'P', 'o', 's', 't', 'm', 'a', 's', 't', 'e', 'r', ' ', '<', 'p', 'o', 's', 't', 'm', 'a', 's', 't', 'e', 'r', '@', 'l', 'o', 'c', 'a', 'l', 'h', 'o', 's', 't', '.', 'c', 'o', 'm', '>' };

    /* class variables */

    /* instance variables */
    private PatternMap zPatternMap;
    private Hashtable zHashtable;
    private ByteBuffer zPOP3Postmaster;
    private ByteBuffer zIMAP4Postmaster;
    private ByteBuffer zVirusRemoved;
    private ScannerOptions zSpamInboundOptions, zSpamOutboundOptions;
    private ScannerOptions zVirusInboundOptions, zVirusOutboundOptions;
    private static int iSpamScanner;
    private static int iVirusScanner;
    private int iMsgSzRelay;
    private int iSpamMsgSzLimit;
    private int iVirusMsgSzLimit;
    private boolean bCopyOnException;
    private boolean bReturnErrOnSMTPBlock;
    private boolean bReturnErrOnPOP3Block;
    private boolean bReturnErrOnIMAP4Block;

    /* constructors */
    private XMailScannerCache()
    {
        zSpamInboundOptions = new ScannerOptions();
        zSpamOutboundOptions = new ScannerOptions();
        zVirusInboundOptions = new ScannerOptions();
        zVirusOutboundOptions = new ScannerOptions();
    }

    /* public methods */
    public static XMailScannerCache build(EmailTransform transform)
    {
        EmailSettings zSettings = transform.getEmailSettings();

        List zFilterList = zSettings.getFilters();
        if(null == zFilterList)
        {
            zLog.error("Unable to create email; custom rule list is null (not valid)");
            return null;
        }

        CharsetEncoder zEncoder = Constants.CHARSET.newEncoder();
        SubCache zSubCache = build(zEncoder, zFilterList);
        PatternMap zPatternMap = zSubCache.getPatternMap();

        /*  by ian
        if (0 == zPatternMap.count())
        {
            zLog.error("Unable to create email; all filter patterns are invalid");
            zPatternMap.emptyMap(); // ensure that everything is deleted
            return null;
        }
         **/

        if (false == Constants.setAll())
        {
            return null;
        }

        XMailScannerCache zXMCache = new XMailScannerCache();
        zXMCache.zPatternMap = zPatternMap;
        zXMCache.zHashtable = zSubCache.getHashtable();

        CTLDefinition zCtl = zSettings.getControl();
        if (null == zCtl)
        {
            // How could this ever happen? XXX
            // zCtl = (CTLDefinition) NodeType.type(CTLDefinition.class).instantiate();
            zXMCache.zPOP3Postmaster = ByteBuffer.wrap(POP3PMBA, POP3PMBA.length, 0);
            zXMCache.zIMAP4Postmaster = ByteBuffer.wrap(IMAP4PMBA, IMAP4PMBA.length, 0);
        }
        else
        {
            try
            {
                zEncoder.reset();
                zXMCache.zPOP3Postmaster = zEncoder.encode(CharBuffer.wrap(zCtl.getPop3Postmaster()));
                zXMCache.zPOP3Postmaster.position(zXMCache.zPOP3Postmaster.limit());
                zEncoder.reset();
                zXMCache.zIMAP4Postmaster = zEncoder.encode(CharBuffer.wrap(zCtl.getImap4Postmaster()));
                zXMCache.zIMAP4Postmaster.position(zXMCache.zIMAP4Postmaster.limit());
            }
            catch (CharacterCodingException e)
            {
                zLog.error("Unable to encode default strings from preferences");
                return null;
            }
        }

        zXMCache.zVirusRemoved = ByteBuffer.wrap(Constants.VIRUSREMOVEDBA, Constants.VIRUSREMOVEDBA.length, 0);

        zXMCache.iMsgSzRelay = zCtl.getMsgSzLimit();
        zXMCache.iSpamMsgSzLimit = zCtl.getSpamMsgSzLimit();
        zXMCache.iVirusMsgSzLimit = zCtl.getVirusMsgSzLimit();

        zXMCache.bCopyOnException = zCtl.isCopyOnException();
        zXMCache.bReturnErrOnSMTPBlock = zCtl.isReturnErrOnSMTPBlock();
        zXMCache.bReturnErrOnPOP3Block = zCtl.isReturnErrOnPOP3Block();
        zXMCache.bReturnErrOnIMAP4Block = zCtl.isReturnErrOnIMAP4Block();

        SScanner zSScanner;
        //zSScanner = zCtl.getSpamScanner();
        zXMCache.setASScanner(Constants.SPAMAS_ID);

        SSCTLDefinition zSpamInboundCtl = zSettings.getSpamInboundCtl();
        if (null == zSpamInboundCtl)
        {
            // How could this ever happen? XXX
            // zSpamInboundCtl = (SSCTLDefinition) NodeType.type(SSCTLDefinition.class).instantiate();
        }
        ScannerOptions zOptions = zXMCache.zSpamInboundOptions;
        zOptions.set(Constants.SCAN_TYPE, zSpamInboundCtl.isScan());
        zOptions.set(Constants.convertSSType(zSpamInboundCtl.getActionOnDetect().toString(), zSpamInboundCtl.isCopyOnBlock()));

        SSCTLDefinition zSpamOutboundCtl = zSettings.getSpamOutboundCtl();
        if (null == zSpamOutboundCtl)
        {
            // How could this ever happen? XXX
            // zSpamOutboundCtl = (SSCTLDefinition) NodeType.type(SSCTLDefinition.class).instantiate();
        }
        zOptions = zXMCache.zSpamOutboundOptions;
        zOptions.set(Constants.SCAN_TYPE, zSpamOutboundCtl.isScan());
        zOptions.set(Constants.convertSSType(zSpamOutboundCtl.getActionOnDetect().toString(), zSpamOutboundCtl.isCopyOnBlock()));

        VScanner zVScanner = zCtl.getVirusScanner();
        if (true == zVScanner.equals(VScanner.CLAMAV))
        {
            zXMCache.setAVScanner(Constants.CLAMAV_ID);
        }
        else if (true == zVScanner.equals(VScanner.SOPHOSAV))
        {
            zXMCache.setAVScanner(Constants.SOPHOSAV_ID);
        }
        else if (true == zVScanner.equals(VScanner.HAURIAV))
        {
            zXMCache.setAVScanner(Constants.HAURIAV_ID);
        }
        else if (true == zVScanner.equals(VScanner.FPROTAV))
        {
            zXMCache.setAVScanner(Constants.FPROTAV_ID);
        }
        else /* default */
        {
            zXMCache.setAVScanner(Constants.NOAV_ID);
        }

        VSCTLDefinition zVirusInboundCtl = zSettings.getVirusInboundCtl();
        if (null == zVirusInboundCtl)
        {
            // How could this ever happen? XXX
            // zVirusInboundCtl = (VSCTLDefinition) NodeType.type(VSCTLDefinition.class).instantiate();
        }
        zOptions = zXMCache.zVirusInboundOptions;
        zOptions.set(Constants.SCAN_TYPE, zVirusInboundCtl.isScan());
        zOptions.set(Constants.convertVSType(zVirusInboundCtl.getActionOnDetect().toString(), zVirusInboundCtl.isCopyOnBlock()));

        VSCTLDefinition zVirusOutboundCtl = zSettings.getVirusOutboundCtl();
        if (null == zVirusOutboundCtl)
        {
            // How could this ever happen? XXX
            // zVirusOutboundCtl = (VSCTLDefinition) NodeType.type(VSCTLDefinition.class).instantiate();
        }
        zOptions = zXMCache.zVirusOutboundOptions;
        zOptions.set(Constants.SCAN_TYPE, zVirusOutboundCtl.isScan());
        zOptions.set(Constants.convertVSType(zVirusOutboundCtl.getActionOnDetect().toString(), zVirusOutboundCtl.isCopyOnBlock()));

        dump(zXMCache, zCtl, zSpamInboundCtl, zSpamOutboundCtl, zVirusInboundCtl, zVirusOutboundCtl);

        return zXMCache;
    }

    /* get action of matching true pattern (see constants above) */
    public MatchAction getAction(MLMessagePTO zMsgPTO, int iMapOption, int iDefOption)
    {
        try
        {
            MatchAction zTmp = zPatternMap.matchMap(zMsgPTO, iMapOption, iDefOption);
            if (null == zTmp)
            {
                return MatchAction.NOTRUEACTION;
            }
            return zTmp;
        }
        catch (MatchException e)
        {
            zLog.error("Unable to match message with email pattern: " + e);
            return MatchAction.NOTRUEACTION;
        }
    }

    public void setASScanner(int iSpamScanner)
    {
        this.iSpamScanner = iSpamScanner;
        return;
    }

    public void setAVScanner(int iVirusScanner)
    {
        this.iVirusScanner = iVirusScanner;
        return;
    }

    /* get exchange value of exchange key */
    public Object getExchValue(String zExchKey)
    {
        return zHashtable.get(zExchKey);
    }

    public ByteBuffer getPOP3Postmaster()
    {
        return zPOP3Postmaster;
    }

    public ByteBuffer getIMAP4Postmaster()
    {
        return zIMAP4Postmaster;
    }

    public ByteBuffer getVirusRemoved()
    {
        return zVirusRemoved;
    }

    public int getMsgSzRelay()
    {
        return iMsgSzRelay;
    }

    public int getSpamMsgSzLimit()
    {
        return iSpamMsgSzLimit;
    }

    public int getVirusMsgSzLimit()
    {
        return iVirusMsgSzLimit;
    }

    public boolean getCopyOnException()
    {
        return bCopyOnException;
    }

    public boolean getReturnErrOnSMTPBlock()
    {
        return bReturnErrOnSMTPBlock;
    }

    public boolean getReturnErrOnPOP3Block()
    {
        return bReturnErrOnPOP3Block;
    }

    public boolean getReturnErrOnIMAP4Block()
    {
        return bReturnErrOnIMAP4Block;
    }

    public int getASScanner()
    {
        return iSpamScanner;
    }

    public boolean getSpamInboundOption(int iType)
    {
        return getOption(zSpamInboundOptions, iType);
    }

    public boolean getSpamOutboundOption(int iType)
    {
        return getOption(zSpamOutboundOptions, iType);
    }

    public int getAVScanner()
    {
        return iVirusScanner;
    }

    public boolean getVirusInboundOption(int iType)
    {
        return getOption(zVirusInboundOptions, iType);
    }

    public boolean getVirusOutboundOption(int iType)
    {
        return getOption(zVirusOutboundOptions, iType);
    }

    /* private methods */
    /* build custom rules */
    private static SubCache build(CharsetEncoder zEncoder, List zList)
    {
        Integer zPassAction = new Integer(Constants.PASS_ACTION);
        Integer zBlockAction = new Integer(Constants.BLOCK_ACTION);
        Integer zCopyOnBlockAction = new Integer(Constants.COPYONBLOCK_ACTION);
        Integer zExchangeAction = new Integer(Constants.EXCHANGE_ACTION);

        PatternMap zPatternMap = PatternMap.initMap();
        Hashtable zHashtable = new Hashtable();
        String zName = null;
        int iMapIdx = 0;

        try
        {
            ArrayList zData;
            Pattern zPattern;
            Matcher zMatcher;
            String zExchValue;
            String zFileName;
            Iterator zIter;
            Integer zType;
            Integer zAction;
            MLDefinition zMLField;

            /* get definitions and
             * create patterns and actions from definitions
             */
            for (zIter = zList.iterator(); true == zIter.hasNext(); iMapIdx++)
            {
                zMLField = (MLDefinition) zIter.next();

                zType = Constants.convertType(zMLField.getField());
                if (true == Constants.NONE_INT.equals(zType))
                {
                    zLog.error("Unknown mail field type " + zMLField.getField() + " [" + zType + "]");
                    continue;
                }

                zPattern = Pattern.compile(zMLField.getValue(), Pattern.CASE_INSENSITIVE);

                zName = zMLField.getAction().toString();
                if (true == zName.equals(Constants.BLOCK))
                {
                    if (false == zMLField.isCopyOnBlock())
                    {
                        zAction = zBlockAction;
                    }
                    else
                    {
                        zAction = zCopyOnBlockAction;
                    }
                    zExchValue = null;
                }
                else if (true == zName.equals(Constants.EXCHANGE))
                {
                    zAction = zExchangeAction;
                    zExchValue = zMLField.getExchValue();

                    zMatcher = Constants.FILEPREFIXP.matcher(zExchValue);
                    if (true == zMatcher.find())
                    {
                        zFileName = zExchValue.substring(zMatcher.end());
                        zData = readFile(zEncoder, zFileName);
                        if (null != zData)
                        {
                            try
                            {
                                /* if exchange value is file name value,
                                 * then exchange key is exchange value and
                                 * exchange value is file data (ByteBuffer list)
                                 */
                                zHashtable.put(zExchValue, zData);
                                /* fall through */
                            }
                            catch (NullPointerException e)
                            {
                                zLog.error("Unable to cache file data, " + zName + " pattern[" + iMapIdx + "]: " + e);
                                continue;
                            }
                        }
                        else
                        {
                            zLog.error("Unable to read file data, " + zName + " pattern[" + iMapIdx + "]");
                            continue;
                        }
                        /* fall through */
                    }
                    else
                    {
                        zMatcher = Constants.TEXTPREFIXP.matcher(zExchValue);
                        if (true == zMatcher.find())
                        {
                            String zText = zExchValue.substring(zMatcher.end()) + Constants.PCRLF;
                            ByteBuffer zLine;
                            try
                            {
                                zEncoder.reset();
                                zLine = zEncoder.encode(CharBuffer.wrap(zText));
                            }
                            catch (CharacterCodingException e)
                            {
                                zLog.error("Unable to encode text: " + zText);
                                continue;
                            }
                            zLine.position(zLine.limit());
                            CBufferWrapper zCLine = new CBufferWrapper(zLine);
                            zData = new ArrayList();
                            zData.add(zCLine);

                            try
                            {
                                /* if exchange value is text value,
                                 * then exchange key is exchange value and
                                 * exchange value is text (ByteBuffer list)
                                 */
                                zHashtable.put(zExchValue, zData);
                                /* fall through */
                            }
                            catch (NullPointerException e)
                            {
                                zLog.error("Unable to cache file data, " + zName + " pattern[" + iMapIdx + "]: " + e);
                                continue;
                            }
                        }
                        else
                        {
                            /* else exchange value is literal value,
                             * then exchange key is exchange value and
                             * exchange value is exchange value
                             */
                            zHashtable.put(zExchValue, zExchValue);
                            /* fall through */
                        }
                    }
                }
                else /* default = Constants.PASS */
                {
                    zAction = zPassAction;
                    zExchValue = null;
                }

                zPatternMap.appendToMap(new PatternType(zType, zPattern), new PatternAction(zAction, zExchValue));
            }
        }
        catch (BuildException e)
        {
            zLog.error("Unable to build " + zName + " pattern[" + iMapIdx + "]: " + e);
            /* continue */
        }

        return new SubCache(zPatternMap, zHashtable);
    }

    /* opens filename,
     * reads file data into ByteBuffers,
     * adds ByteBuffers to list, and
     * returns list
     * - hopefully, file is small in size
     */
    private static ArrayList readFile(CharsetEncoder zEncoder, String zFileName)
    {
        FileReader zFReader = null;

        try
        {
            zFReader = new FileReader(zFileName);
        }
        catch (FileNotFoundException e)
        {
            zLog.error("Unable to find file: " + e);
            return null;
        }

        BufferedReader zBReader = new BufferedReader(zFReader);

        ArrayList zList = new ArrayList();

        try
        {
            String zReadLine;
            ByteBuffer zLine;
            CBufferWrapper zCLine;

            /* read file data as separate lines of Strings and
             * using default charset,
             * encode each String into ByteBuffer
             */
            while (true == zBReader.ready())
            {
                zReadLine = zBReader.readLine() + Constants.PCRLF;
                zEncoder.reset();
                zLine = zEncoder.encode(CharBuffer.wrap(zReadLine));
                zLine.position(zLine.limit());
                zCLine = new CBufferWrapper(zLine);
                zList.add(zCLine);
            }
            zBReader.close();
        }
        catch (IOException e)
        {
            zLog.error("Unable to process (read/close) file: " + e);
            return null;
        }

        return zList;
    }

    private boolean getOption(ScannerOptions zOptions, int iType)
    {
        return zOptions.get(iType);
    }

    /* for debugging */
    public static void dump(XMailScannerCache zXMCache, CTLDefinition zCtl,
                            SSCTLDefinition zSpamInboundCtl, SSCTLDefinition zSpamOutboundCtl,
                            VSCTLDefinition zVirusInboundCtl, VSCTLDefinition zVirusOutboundCtl)
    {
        CBufferWrapper zCDummy = new CBufferWrapper(null);

        zLog.debug("POP3 postmaster: \"" + zCDummy.renew(zXMCache.zPOP3Postmaster) + "\"");
        zLog.debug("IMAP4 postmaster: \"" + zCDummy.renew(zXMCache.zIMAP4Postmaster) + "\"");
        zLog.debug("copy message when an exception occurs: " + zXMCache.bCopyOnException);
        zLog.debug("report SMTP error when blocking message: " + zXMCache.bReturnErrOnSMTPBlock);
        zLog.debug("report POP3 error when blocking message: " + zXMCache.bReturnErrOnPOP3Block);
        zLog.debug("report IMAP4 error when blocking message: " + zXMCache.bReturnErrOnIMAP4Block);

        zLog.debug("message relay size: " + zXMCache.iMsgSzRelay);

        SScanner zSScanner = zCtl.getSpamScanner();
        zLog.debug("use anti-spam scanner: " + zSScanner + " (" + iSpamScanner + ")");

        zLog.debug("spam message size limit: " + zXMCache.iSpamMsgSzLimit);

        ScannerOptions zScOpt = zXMCache.zSpamInboundOptions;
        zLog.debug("always scan uploaded/outbound message for spam: " + zScOpt.get(Constants.SCAN_TYPE));
        zLog.debug("block uploaded/outbound message that contains spam: " + zScOpt.get(Constants.BLOCK_TYPE));
        zLog.debug("copy blocked uploaded/outbound message that contains spam: " + zScOpt.get(Constants.CPONBLOCK_TYPE));
        zLog.debug("forward and warn sender if uploaded/outbound message contains spam: " + zScOpt.get(Constants.NTFYSENDR_TYPE));
        zLog.debug("forward and warn receiver if uploaded/outbound message contains spam: " + zScOpt.get(Constants.NTFYRECVR_TYPE));

        zScOpt = zXMCache.zSpamOutboundOptions;
        zLog.debug("always scan downloaded/inbound message for spam: " + zScOpt.get(Constants.SCAN_TYPE));
        zLog.debug("block downloaded/inbound message that contains spam: " + zScOpt.get(Constants.BLOCK_TYPE));
        zLog.debug("copy blocked downloaded/inbound message that contains spam: " + zScOpt.get(Constants.CPONBLOCK_TYPE));
        zLog.debug("forward and warn sender if downloaded/inbound message contains spam: " + zScOpt.get(Constants.NTFYSENDR_TYPE));
        zLog.debug("forward and warn receiver if downloaded/inbound message contains spam: " + zScOpt.get(Constants.NTFYRECVR_TYPE));

        VScanner zVScanner = zCtl.getVirusScanner();
        zLog.debug("use anti-virus scanner: " + zVScanner + " (" + iVirusScanner + ")");

        zLog.debug("virus message size limit: " + zXMCache.iVirusMsgSzLimit);

        zScOpt = zXMCache.zVirusInboundOptions;
        zLog.debug("always scan uploaded/outbound message for virus: " + zScOpt.get(Constants.SCAN_TYPE));
        zLog.debug("block uploaded/outbound message that contains virus: " + zScOpt.get(Constants.BLOCK_TYPE));
        zLog.debug("copy blocked uploaded/outbound message that contains virus: " + zScOpt.get(Constants.CPONBLOCK_TYPE));
        zLog.debug("replace virus attachment in uploaded/outbound message that contains virus: " + zScOpt.get(Constants.REPLACE_TYPE));
        zLog.debug("forward and warn sender if uploaded/outbound message contains virus: " + zScOpt.get(Constants.NTFYSENDR_TYPE));
        zLog.debug("forward and warn receiver if uploaded/outbound message contains virus: " + zScOpt.get(Constants.NTFYRECVR_TYPE));

        zScOpt = zXMCache.zVirusOutboundOptions;
        zLog.debug("always scan downloaded/inbound message for virus: " + zScOpt.get(Constants.SCAN_TYPE));
        zLog.debug("block downloaded/inbound message that contains virus: " + zScOpt.get(Constants.BLOCK_TYPE));
        zLog.debug("copy blocked downloaded/inbound message that contains virus: " + zScOpt.get(Constants.CPONBLOCK_TYPE));
        zLog.debug("replace virus attachment in downloaded/inbound message that contains virus: " + zScOpt.get(Constants.REPLACE_TYPE));
        zLog.debug("forward and warn sender if downloaded/inbound message contains virus: " + zScOpt.get(Constants.NTFYSENDR_TYPE));
        zLog.debug("forward and warn receiver if downloaded/inbound message contains virus: " + zScOpt.get(Constants.NTFYRECVR_TYPE));

        zLog.debug("pattern map:");
        zXMCache.zPatternMap.dump(); /* dump PatternMap */

        return;
    }
}
