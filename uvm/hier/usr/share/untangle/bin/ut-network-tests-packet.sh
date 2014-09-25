#!/bin/bash
##
## Tcpdump wrapper to allow creation of pcap file as well as display that
## file to stdout.
##
timeout=$1
filename=$2
tcpdumpArguments=$3

##
## Read contents of current pcp file and count last read location.
## On subsequent function calls, continue from that point.
##
last_line=;
tcpdumpread(){
	new_last_line=;
	show_lines=0;
	if [ "" == "$last_line" ] ; then
		show_lines=1;
	fi;
	if [ ! -f $filename -o \
         ! -s $filename ] ;then
		return
	fi
	while read -t 1 line; do
		new_last_line=$line;
		if [ $show_lines -eq 0 \
			-a "" != "$last_line" -a \
			"$line" == "$last_line" ] ; then
			show_lines=1;
			continue;
		fi;
		if [ $show_lines -eq 1 ] ; then
			echo $line;
		fi;
	done < <(tcpdump -r $filename 2> /dev/null);
	last_line=$new_last_line;
}; 

if [ ! -d /tmp/network-tests ] ; then 
	mkdir -p /tmp/network-tests; 
fi;

tcpdumpstderr=$filename.stderr

##
## Launch tcpdump writer process then loop for timeout seconds, dumping
## the contents of the currently written pcap file.
##
tcpdump $tcpdumpArguments -w $filename > $tcpdumpstderr 2>&1 & echo -n "";
trace_pid=$!;
for t in `seq 1 $timeout`; do 
    sleep 1;
    if [ $t -eq 1 -a \
         -f $tcpdumpstderr ] ; then
        ##
        ## First line of tcpdump
        ##
        head -1 $tcpdumpstderr
    fi
	if [ ! -d /proc/$trace_pid ]; then 
        ##
        ## tcpdump process already stopped
        ##
		break; 
	fi;
	tcpdumpread;
done;
kill -INT $trace_pid > /dev/null 2>&1
tcpdumpread

##
## tcpdump in -w mode provides some useful and unuseful information:
## Useful: header, # of packets read.
## Unuseful: The "Got NUM" message.  
## We collect this and filter out those "Got" messages at the end.
##
cat $tcpdumpstderr | sed 's/Got .*//g' | sed -n '1!p'
rm -f $tcpdumpstderr
