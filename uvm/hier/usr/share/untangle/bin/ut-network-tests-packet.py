#! /usr/bin/env python
import os
import getopt
from multiprocessing import Process, Queue
import shlex
import signal
import subprocess
import sys
import time

UNTANGLE_DIR = '%s/usr/lib/python%d.%d' % ( "@PREFIX@", sys.version_info[0], sys.version_info[1] )
if ( "@PREFIX@" != ''):
    sys.path.insert(0, UNTANGLE_DIR)

def signal_handler( signal, frame ):
    dump_writer.terminate()
    sys.exit(0)

signal.signal(signal.SIGINT, signal_handler)
signal.signal(signal.SIGTERM, signal_handler)
signal.signal(signal.SIGUSR1, signal_handler)
signal.signal(signal.SIGUSR2, signal_handler)
signal.signal(signal.SIGQUIT, signal_handler)

class DumpWriter:	
    def start( self, arguments, timeout, file_name, error_file_name ):
        args = shlex.split( arguments )
        args.insert( 0, tcpdump )
        args.append( "-w" )
        args.append( file_name )
 
        self.process = subprocess.Popen( args, stderr=open( error_file_name, 'wb'), stdout=open(os.devnull, 'wb') )
        while timeout > 0:
            timeout = timeout - 1
            time.sleep(1)
        self.terminate()

    def terminate(self):
        self.process.terminate()
        
class DumpReader:
    def __init__( self, file_name, error_file_name ):
        self.dump_file_name = file_name
        self.error_file_name = error_file_name
		
        self.printed_header = False
        self.last_line_count = 0
		
    def header( self ):
        if self.printed_header == False and os.path.isfile( self.error_file_name ) == True:
            h = open( self.error_file_name )
            count = 0
            for line in h:
                count = count + 1
                print line
                if count == 1:
                    break;
            h.close()
            if count > 0:
                self.printed_header = True
				
    def read( self ):
        if self.printed_header == False:
            return
		
        if self.last_line_count == 0:
            show_lines = 1
        else:
            show_lines = 0
			
        if os.path.isfile( self.dump_file_name ) == False:
            return

        args = [
            tcpdump,
            '-n',
			'-r', self.dump_file_name
        ]
        self.process = subprocess.Popen( args, stdout=subprocess.PIPE, stderr=open(os.devnull, 'wb') )
        line_count = 0
        for line in self.process.stdout:
            line_count = line_count + 1
            if show_lines == 0 and line_count == self.last_line_count:
                show_lines = 1
                continue
			
            if show_lines == 1:
                print line.strip()
        self.last_line_count = line_count

def usage():
    print "usage"
    print "--help\tShow usage"
    print "--timeout <sec>\tTime to run in seconds"
    print "--filename <filename>\tFile to write"
    print "--arguments <list>\tcpdump arguments"
	
def main(argv):
    global _debug
    global dump_reader
    global dump_writer
    global tcpdump
    _debug = False
    tcpdump = "/usr/sbin/tcpdump"
	
    timeout = 5
    filename = "/tmp/network-tests"
    arguments = ""

    try:
        opts, args = getopt.getopt( argv, "htfa:d", [ "help", "timeout=", "filename=", "arguments=", "debug" ] )
    except getopt.GetoptError:
        usage()
        sys.exit(2)

    for opt, arg in opts:
        if opt in ( "-h", "--help" ):
            usage()
            sys.exit()
        elif opt in ( "-d", "--debug" ):
            _debug = True
        elif opt in ( "-t", "--timeout" ):
            timeout = int(arg)
        elif opt in ( "-f", "--filename" ):
            filename = arg
        elif opt in ( "-t", "--arguments" ):
            arguments = arg

    if _debug == True:
        print "timeout=" + str(timeout)
        print "filename=" + filename
        print "arguments=" + arguments

    path = "/".join( filename.split( "/" )[0:-1] )
    if os.path.isdir( path ) == False:
        os.mkdirs( path )

    tcpdump_stderr_filename = filename + ".stderr"
	
    dump_reader = DumpReader( filename, tcpdump_stderr_filename )
    dump_writer = DumpWriter()
    dump_writer.start( arguments, timeout, filename, tcpdump_stderr_filename )
	
    while timeout > 0:
        dump_reader.header()
        dump_reader.read()

        timeout = timeout -1
        time.sleep(1) 

    dump_reader.read()
	
if __name__ == "__main__":
    main( sys.argv[1:] )
    
