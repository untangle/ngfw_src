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

package com.untangle.node.ips.options;

import java.nio.ByteBuffer;
import java.text.CharacterIterator;

import org.apache.log4j.Logger;

import org.apache.xerces.impl.xpath.regex.BMPattern;
import com.untangle.node.ips.IpsSessionInfo;
import com.untangle.node.util.AsciiCharBuffer;
import com.untangle.node.util.AsciiCharBufferCharacterIterator;

/**
 * This class matches the content option found in snort based rule signatures.
 *
 * @Author Nick Childers
 */
///XXX - ADD ERROR HANDELING OMG!
class ContentOption extends IpsOption
{
    private ContentOption previousContentOption = null;

    private int start = 0;
    private int end = 0;
    private int distance = 0;
    private int within = 0;

    private final String stringPattern;
    private boolean withinFlag = false;
    private boolean distanceFlag = false;
    private boolean nocase = false;

    private BMPattern contentPattern;
    private final Logger logger = Logger.getLogger(getClass());

    public ContentOption(OptionArg arg)
    {
        super(arg);

        String params = arg.getParams();

        int index = params.indexOf('|');
        if(index < 0) {
            stringPattern = params;
        }
        else {
            ByteBuffer binaryBuffer = ByteBuffer.allocate(2048);
            buildBytePattern(binaryBuffer, params, index);
            byte[] bytePattern = new byte[binaryBuffer.position()];
            binaryBuffer.flip();
            binaryBuffer.get(bytePattern);
            stringPattern = new String(bytePattern);
        }
        contentPattern = new BMPattern(stringPattern, nocase);
    }

    public void setNoCase()
    {
        nocase = true;
        contentPattern = new BMPattern(stringPattern, nocase);
    }

    public void setOffset(int val)
    {
        start = val;
    }

    public void setDepth(int val)
    {
        end = start+val;
        if(start == end)
            end = 0;
    }

    public void setDistance(int val)
    {
        previousContentOption = getPreviousContentOption();

        if(previousContentOption == null) {
            setOffset(val);
            return;
        }

        distance = val;
        distanceFlag = true;
    }

    public void setWithin(int val)
    {
        previousContentOption = getPreviousContentOption();

        if(previousContentOption == null) {
            setDepth(val+stringPattern.length());
            return;
        }

        within = val+stringPattern.length();
        withinFlag = true;
    }

    private ContentOption getPreviousContentOption()
    {
        IpsOption option = signature.getOption("ContentOption",this);
        if(option != null)
            return (ContentOption) option;
        return null; //error checking OMGWTFBBQ
    }

    private void setStartAndEndPoints(int offset, int depth, IpsSessionInfo sessionInfo)
    {
        sessionInfo.start = offset;
        sessionInfo.end = offset+depth;
        if(sessionInfo.start == sessionInfo.end)
            sessionInfo.end = 0;
    }

    public boolean runnable()
    {
        return true;
    }

    public boolean run(IpsSessionInfo sessionInfo)
    {
        ByteBuffer eventData = sessionInfo.getEvent().data();
        AsciiCharBuffer data = AsciiCharBuffer.wrap(eventData);
        CharacterIterator iter = new AsciiCharBufferCharacterIterator(data);

        sessionInfo.start = start;
        sessionInfo.end = end;

        //logger.debug("needle: " + stringPattern);
        //logger.debug("haystack: " + data);
        
        if(distanceFlag)
            sessionInfo.start = sessionInfo.indexOfLastMatch + distance;
        //start = previousContentOption.getIndexOfLastMatch()+distance;

        if(withinFlag) {
            if(distanceFlag)
                setStartAndEndPoints(sessionInfo.start,within,sessionInfo);
            else
                setStartAndEndPoints(sessionInfo.indexOfLastMatch, within, sessionInfo);
            // setStartAndEndPoints(previousContentOption.getIndexOfLastMatch(),within,sessionInfo);
        }

        if(sessionInfo.start > data.length() || sessionInfo.start < 0)
            return false;

        if(sessionInfo.end <= 0 || sessionInfo.end > data.length())
            sessionInfo.end = data.length();

        int result = contentPattern.matches(iter, sessionInfo.start, sessionInfo.end);
        //return super.negationFlag() ^ contentPattern.matcher(data.substring(start,end)).find();
        if (result >= 0) {
            if (logger.isDebugEnabled())
                logger.debug("content matched for rule " + signature.getSid() + ": " + stringPattern);
            sessionInfo.indexOfLastMatch = result + stringPattern.length();
            // Was: sessionInfo.indexOfLastMatch = matcher.end();, is my version wrong?
            return true;
        }
        //sessionInfo.indexOfLastMatch = -1;
        return false;
    }

    /**
     * This must parse strings in the form of "|byte byte byte| string | byte | string" etc...
     * This class will recursivly call itself to iterate over all the | byte | sections, calling
     * a sub function to put those bytes in the objects binaryBuffer. It will parse out the
     * string sections using a second sub function. That second sub function will also put all
     * the bytes of the string onto the byte buffer.
     */
    private void buildBytePattern(ByteBuffer binaryBuffer, String params, int index)
    {
        if(index == 0) {
            index = params.indexOf('|', 1);
            String bytes = params.substring(1,index);
            bytes = bytes.replaceAll(" ", "");
            parseBinaryPattern(binaryBuffer, bytes);
            String substring = params.substring(index+1);
            index = substring.indexOf('|') ;

            if(substring.length() > 0)
                buildBytePattern(binaryBuffer, substring, index);
        }
        else {
            if(index < 0)
                index = params.length();
            parseASCIIPattern(binaryBuffer, params.substring(0,index));
            String substring = params.substring(index);
            index = 0;

            if(substring.length() > 0)
                buildBytePattern(binaryBuffer, substring, index);
        }
    }

    private void parseASCIIPattern(ByteBuffer binaryBuffer, String params)
    {
        if(params.length() > binaryBuffer.remaining()) {
            logger.warn("Very large ASCII pattern");
            return;
        }
        binaryBuffer.put(params.getBytes());
    }

    private void parseBinaryPattern(ByteBuffer binaryBuffer, String bytes)
    {
        if(bytes.length()%2 != 0 || bytes.length() > binaryBuffer.remaining()) {
            logger.warn("Very large binary pattern"); //throw error XXX
            return;
        }
        for(int i = 0; i < bytes.length()/2; i++) {
            byte head =  Byte.parseByte(""+bytes.charAt(2*i),16);
            byte tail = Byte.parseByte(""+bytes.charAt(2*i+1),16);
            byte tmpByte = head;
            tmpByte  = (byte) (tmpByte << 4);
            tmpByte = (byte) (tmpByte | tail);
            binaryBuffer.put(tmpByte);
        }
    }

    public boolean optEquals(Object o)
    {
        if (!(o instanceof ContentOption)) {
            return false;
        }

        ContentOption co = (ContentOption)o;

        if (!super.optEquals(co)) {
            return false;
        }

        if (null == previousContentOption || null == co.previousContentOption) {
            if (previousContentOption != co.previousContentOption) {
                return false;
            }
        } else {
            if (!previousContentOption.optEquals(co.previousContentOption)) {
                return false;
            }
        }

        return start == co.start
            && end == co.end
            && distance == co.distance
            && within == co.within
            && stringPattern.equals(co.stringPattern)
            && withinFlag == co.withinFlag
            && distanceFlag == co.distanceFlag
            && nocase == co.nocase;
    }

    public int optHashCode()
    {
        int result = 17;
        result = result * 37 + super.optHashCode();
        result = result * 37 + (null == previousContentOption ? 0 : previousContentOption.optHashCode());
        result = result * 37 + start;
        result = result * 37 + end;
        result = result * 37 + distance;
        result = result * 37 + within;
        result = result * 37 + stringPattern.hashCode();
        result = result * 37 + (withinFlag ? 1 : 0);
        result = result * 37 + (distanceFlag ? 1 : 0);
        result = result * 37 + (nocase ? 1 : 0);
        return result;
    }
}
