#!/usr/bin/python

# $HeadURL: svn://chef/work/src/buildtools/rake-util.rb $
# Copyright (c) 2003-2009 Untangle, Inc.
#
# This program is free software; you can redistribute it and/or modify
# it under the terms of the GNU General Public License, version 2,
# as published by the Free Software Foundation.
#
# This program is distributed in the hope that it will be useful, but
# AS-IS and WITHOUT ANY WARRANTY; without even the implied warranty of
# MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE, TITLE, or
# NONINFRINGEMENT.  See the GNU General Public License for more details.
#
# You should have received a copy of the GNU General Public License
# along with this program; if not, write to the Free Software
# Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
#
# Aaron Read <amread@untangle.com>

import getopt
import logging
import mx
import socket
import sys
import time

from threading import Thread

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

from reports.log import *
logger = getLogger(__name__)

class Worker(Thread):
    def __init__(self, socket):
        Thread.__init__(self)

        self.__socket = socket
        self.__incomplete_line = ''

    def run(self):
        while 1:
            buf = self.__socket.recv(4096)

            if not buf:
                break
            elif buf == '':
                continue
            else:
                self.__incomplete_line += buf
                s = self.__incomplete_line.split('\n')

                if self.__incomplete_line.endswith("\n"):
                    lines = s[:-1]
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
            logger.info('Got line: %s' % (line,))
            (cmd, node_name, end_date, report_days, host, user,
             email) = line.split(',')
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
             try:
                  reports.engine.generate_sub_report(REPORTS_OUTPUT_BASE,
                                                     node_name, end_date,
                                                     int(report_days),
                                                     host, user, email)
             except:
                  logger.error("could not process request line: %s" % line,
                                exc_info=True);
                  return 'ERROR: see server log'
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
