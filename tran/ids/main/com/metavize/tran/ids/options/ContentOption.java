package com.metavize.tran.ids.options;

import java.util.regex.*;
//import java.util.Vector;

import org.apache.log4j.Logger;
import java.nio.ByteBuffer;
import com.metavize.mvvm.tapi.event.*;
import com.metavize.tran.ids.IDSDetectionEngine;
import com.metavize.tran.ids.IDSRuleSignature;
import com.metavize.tran.ids.IDSSessionInfo;

/**
 * This class matches the content option found in snort based rule signatures.
 *
 * @Author Nick Childers
 */

///XXX - ADD ERROR HANDELING OMG!

public class ContentOption extends IDSOption {

    private static final Logger logger = Logger.getLogger(ContentOption.class);

    private ContentOption previousContentOption = null;
	
    //private int indexOfMatch = 0;
    private int start = 0;
    private int end = 0;
    private int distance = 0;
    private int within = 0;

    private Pattern contentPattern;
	
    private byte[] bytePattern;	
    private ByteBuffer binaryBuffer = ByteBuffer.allocate(2048);
	
    private boolean hasBinaryData = false;
    private boolean withinFlag = false;
    private boolean distanceFlag = false;
	
    public ContentOption(IDSRuleSignature signature, String params) {
        super(signature, params);
        int index = params.indexOf('|');
        if(index < 0) {
            contentPattern = Pattern.compile(params, Pattern.LITERAL);
        }
        else {
            buildBytePattern(params, index);
            bytePattern = new byte[binaryBuffer.position()];
            binaryBuffer.flip();
            binaryBuffer.get(bytePattern);
            String pattern = new String(bytePattern);
            contentPattern = Pattern.compile(pattern, Pattern.LITERAL);
        }	
    }
	
    //public int getIndexOfLastMatch() {
      //  return indexOfMatch;
    //}
	
    public void setNoCase() {
        contentPattern = Pattern.compile(contentPattern.pattern(), contentPattern.flags() | Pattern.CASE_INSENSITIVE);
    }

    public void setOffset(int val) {
		start = val;
        //setStartAndEndPoints(val,end);
    }

    public void setDepth(int val) {
        end = start+val;
        if(start == end)
			end = 0;
    }

    public void setDistance(int val) {
        previousContentOption = getPreviousContentOption();
		
        if(previousContentOption == null) {
            setOffset(val);
            return;
        }
		
        distance = val;
        distanceFlag = true;
    }

    public void setWithin(int val) {
        previousContentOption = getPreviousContentOption();
		
        if(previousContentOption == null) {
            setDepth(val+contentPattern.pattern().length());
            return;
        }
		
        within = val+contentPattern.pattern().length();
        withinFlag = true;
    }

    private ContentOption getPreviousContentOption() {
        IDSOption option = signature.getOption("ContentOption",this);
        if(option != null)
            return (ContentOption) option;
        return null; //error checking OMGWTFBBQ
    }
						 
	
    private void setStartAndEndPoints(int offset, int depth, IDSSessionInfo sessionInfo) {
        sessionInfo.start = offset;
        sessionInfo.end = offset+depth;
        if(sessionInfo.start == sessionInfo.end)
            sessionInfo.end = 0;
    }
	
    public boolean runnable() {
        return true;
    }
	
    public boolean run(IDSSessionInfo sessionInfo) {
        ByteBuffer eventData = sessionInfo.getEvent().data();
        String data = new String(eventData.array());
		
		sessionInfo.start = start;
		sessionInfo.end = end;

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
        Matcher matcher = contentPattern.matcher(data.substring(sessionInfo.start,sessionInfo.end));
        //return super.negationFlag() ^ contentPattern.matcher(data.substring(start,end)).find();
        if(matcher.find()) {
            sessionInfo.indexOfLastMatch = matcher.end();
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
	
    private void buildBytePattern(String params, int index) {
        if(index == 0) {
            index = params.indexOf('|', 1);
            String bytes = params.substring(1,index);
            bytes = bytes.replaceAll(" ", "");
            parseBinaryPattern(bytes);
            String substring = params.substring(index+1);
            index = substring.indexOf('|') ;
			
            if(substring.length() > 0)
                buildBytePattern(substring, index);
        }
        else {
            if(index < 0)
                index = params.length();
            parseASCIIPattern(params.substring(0,index));
            String substring = params.substring(index);
            index = 0;
			
            if(substring.length() > 0) 
                buildBytePattern(substring, index);
        }	
    }

    private void parseASCIIPattern(String params) {
        if(params.length() > binaryBuffer.remaining()) {
            logger.warn("Very large ASCII pattern");
            return;
        }
        binaryBuffer.put(params.getBytes());
    }

    private void parseBinaryPattern(String bytes) {
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
}
