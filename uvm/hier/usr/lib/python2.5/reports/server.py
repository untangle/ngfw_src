#!/usr/bin/python

import getopt
import logging
import mx
import socket
import sys
import time

from threading import Thread

LOGFILE = "/var/log/uvm/report.log"
sys.stdout = sys.stderr = open(LOGFILE, 'a+')

locale = 'en'

def usage():
     print """\
usage: %s [options]
Options:
  -h | --help                 help
""" % sys.argv[0]

try:
     opts, args = getopt.getopt(sys.argv[1:], "h",
                                ['help'])
except getopt.GetoptError, err:
     print str(err)
     usage()
     sys.exit(2)

PREFIX = '@PREFIX@'
REPORTS_PYTHON_DIR = '%s/usr/lib/python2.5' % PREFIX
REPORTS_OUTPUT_BASE = '%s/usr/share/untangle/web/reports' % PREFIX
NODE_MODULE_DIR = '%s/reports/node' % REPORTS_PYTHON_DIR

if (PREFIX != ''):
     sys.path.insert(0, REPORTS_PYTHON_DIR)

import reports.i18n_helper
import reports.engine

class Worker(Thread):
    def __init__(self, socket):
        Thread.__init__(self)

        self.__socket = socket
        self.__incomplete_line = ''

    def run(self):
        while 1:
            buf = self.__socket.recv(4096)
            print time.asctime()
            print "got '%s'" % buf
            sys.stdout.flush()

            if not buf:
                break
            elif buf == '':
                continue
            else:
                self.__incomplete_line += buf
                s = self.__incomplete_line.split()

                if self.__incomplete_line.endswith("\n"):
                    lines = s
                    self.__incomplete_line = ''
                else:
                    lines +=  s[0:len(s) - 1]
                    self.__incomplete_line = s[-1]

                for l in lines:
                    response = self.__process_line(l)
                    self.__socket.send('%s\n' % response)
                    if response == 'PONG': # FIXME: maybe for BAD_CMD too ?
                         self.__socket.shutdown(socket.SHUT_RDWR)
                         self.__socket.close()
                         return

        self.__socket.shutdown(socket.SHUT_RDWR)
        self.__socket.close()

    def __process_line(self, line):
        try:
            (cmd, node_name, end_date, host, user, email) = line.split(',')
        except:
            if line == 'PING':
                return "PONG"
            else:
                return 'BAD_CMD: %s' % line

        end_date = mx.DateTime.DateFrom(end_date)

        if host == '':
            host = None
        if user == '':
            user = None
        if email == '':
            email = None

        if cmd == 'generate_sub_report':
            reports.engine.generate_sub_report(REPORTS_OUTPUT_BASE, node_name,
                                               end_date, host, user, email)
            return 'DONE'
        else:
            return 'BAD_CMD: %s' % cmd

reports.engine.init_engine(NODE_MODULE_DIR)

serversocket = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
serversocket.bind(('localhost', 55204))
serversocket.listen(5)

while 1:
    (clientsocket, address) = serversocket.accept()
    ct = Worker(clientsocket)
    ct.start()
