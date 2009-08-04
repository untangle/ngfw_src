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

import commands, os, shutil, sys, xml.dom.minidom
from untangle.ats.uvm_setup import UvmSetup

PREFIX = ""

def setup_module(module):
      shutil.rmtree("%s/usr/share/untangle/web/reports/data" % PREFIX)
      os.system('dropdb -U postgres uvm')
      os.system('createdb -U postgres uvm')
      os.system('bzcat %s/usr/share/untangle/tests/untangle-node-reporting/dogfood.sql.bz2 | psql -U postgres uvm' % PREFIX)
      os.system('%s/usr/lib/python2.5/reports/process.py --date=2009-6-3' % PREFIX)

class TestQueries:

  def test_queries(self):
    for path, dirs, files in os.walk("%s/usr/share/untangle/web/reports/data" % PREFIX):
      for f in files:
        if f == "report.xml":
          f = os.path.join(path, f)
          md = xml.dom.minidom.parse(f)

          try:
            sql = md.getElementsByTagName("sql")[0].childNodes[0].data
          except:
            continue

          status, output = commands.getstatusoutput("echo \"%s\" | psql -v ON_ERROR_STOP=1 -U postgres uvm" % (sql,))

          if not status == 0
            print "=" * 72
            print "Error in report: %s" % (f,)
            print output

          assert status == 0
