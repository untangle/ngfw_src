<%@ page language="java" import="com.untangle.mvvm.*, com.untangle.mvvm.client.*, com.untangle.mvvm.security.Tid, com.untangle.mvvm.tran.*, com.untangle.mvvm.tapi.*, com.untangle.mvvm.util.SessionUtil, org.apache.log4j.helpers.AbsoluteTimeDateFormat, java.util.Properties, java.net.URL, java.io.*, java.util.Vector, java.util.Collections, java.util.Comparator, java.util.Calendar, java.util.GregorianCalendar, java.util.StringTokenizer, javax.naming.*" %>

<%
    BrandingSettings bs = MvvmContextFactory.context().brandingManager().getBrandingSettings();
    String company = bs.getCompanyName();

    // ENUMERATE THE ARCHIVE REPORT DIRECTORIES
    // note that these directory names are in form of 'YYYY-MM-DD'
    String path = System.getProperty("bunnicula.home") + "/web/reports";
    File mvvmReportsFile = new File(path);
    Vector<File> directories = new Vector<File>();
    File[] allFiles = mvvmReportsFile.listFiles();
    String filenameTemplate = "YYYY-MM-DD";
    String filename;
    for( File file : allFiles ) {
        if(true == file.isDirectory()) {
            filename = file.getName();
            if (filename.length() == filenameTemplate.length() &&
                filename.charAt(4) == filenameTemplate.charAt(4) &&
                filename.charAt(7) == filenameTemplate.charAt(7))
                directories.add(file);
        }
    }

    // ORDER THE ARCHIVE REPORT DIRECTORIES
    Comparator<File> directoryComparator = new Comparator<File>() {
        public int compare(File f1, File f2) {
           String n1 = f1.getName();
           String n2 = f2.getName();
           try{
               int y1 = Integer.parseInt(n1.substring(0, 4));
               int y2 = Integer.parseInt(n2.substring(0, 4));
               if( y1 > y2 )
                   return -1;
               else if( y1 < y2 )
                   return 1;
               int m1 = Integer.parseInt(n1.substring(5, 7));
               int m2 = Integer.parseInt(n2.substring(5, 7));
               if( m1 > m2 )
                   return -1;
               else if( m1 < m2 )
                   return 1;
               int d1 = Integer.parseInt(n1.substring(8, 10));
               int d2 = Integer.parseInt(n2.substring(8, 10));
               if( d1 > d2 )
                   return -1;
               else if( d1 < d2 )
                   return 1;
               else
                   return 0;
           } catch(Exception e){ return 0; }
        }
        public boolean equals(Object o){ return true; }
    };

    Collections.sort(directories, directoryComparator);
%>


<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Transitional//EN" "http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd">
<html xmlns="http://www.w3.org/1999/xhtml">
<head>
<title><%=company%> Reports</title>
<meta http-equiv="Content-Type" content="text/html; charset=iso-8859-1" />
<style type="text/css">
/* <![CDATA[ */
@import url(/images/base.css);
/* ]]> */
</style>
</head>
<body>
<div id="main">    
 <!-- Box Start -->
 <div class="main-top-left"></div><div class="main-top-right"></div><div class="main-mid-left"><div class="main-mid-right"><div class="main-mid">
 <!-- Content Start -->
	
	
	<div class="page_head">
		<a href="http://<%companyUrl%>"><img src="/images/BrandingLogo.gif" alt="<%=company%> Logo" /></a> <div><%=company%> Reports</div>
	</div>
	
	
	
    <hr />
	
	
	
	<center>
	<div style="padding: 10px 0; margin: 0 auto; width: 440px; text-align: left;">

<span class="page_header_alternate"><b>Archives</b> &gt; <a href="./current">View Current Report</a></span>
<br />
 
<table width="100%" style="padding: 15px 0px 10px 0px"><tbody>
        <%

        Calendar cal = new GregorianCalendar();
        String[] strCals = new String[3];

        StringTokenizer sTokenizer;
        String strDOW;
        int idx;
        int yearVal;
        int monVal;
        int dayVal;

        for(File file : directories) {
            filename = file.getName();
            // based on requirement that filenameTemplate = "YYYY-MM-DD";
            sTokenizer = new StringTokenizer(filename, "-");
            idx = 0;
            while (true == sTokenizer.hasMoreTokens()) {
                strCals[idx] = sTokenizer.nextToken();
                idx++;
            }
            yearVal = Integer.parseInt(strCals[0]);
            monVal = Integer.parseInt(strCals[1]) - 1; // 0 offset
            dayVal = Integer.parseInt(strCals[2]);
            cal.set(yearVal, monVal, dayVal);
            switch( cal.get(Calendar.DAY_OF_WEEK) ) {
                case Calendar.SUNDAY:
                    strDOW = "Sunday";
                    break;
                case Calendar.MONDAY:
                    strDOW = "Monday";
                    break;
                case Calendar.TUESDAY:
                    strDOW = "Tuesday";
                    break;
                case Calendar.WEDNESDAY:
                    strDOW = "Wednesday";
                    break;
                case Calendar.THURSDAY:
                    strDOW = "Thursday";
                    break;
                case Calendar.FRIDAY:
                    strDOW = "Friday";
                    break;
                default:
                case Calendar.SATURDAY:
                    strDOW = "Saturday";
                    break;
            }

            out.write("<tr><td align=\"center\">");
            out.write("> <a href=\"./" + filename + "\">" + filename + " (" + strDOW + ")</a><br/>");
            out.write("</td></tr>");
        }

     %>
</tbody></table>
   
		
	</div>
	</center>

	
	
	<hr />

	
 <!-- Content End -->
 </div></div></div><div class="main-bot-left"></div><div class="main-bot-right"></div>
 <!-- Box End -->
</div>	

</body>
</html>


