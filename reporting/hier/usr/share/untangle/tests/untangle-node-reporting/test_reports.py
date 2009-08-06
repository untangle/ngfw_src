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

import os
import sets
import shutil

from untangle.ats.uvm_setup import UvmSetup

PREFIX = "@PREFIX@"

def setup_module(module):
    try:
        shutil.rmtree("%s/usr/share/untangle/web/reports/data" % PREFIX)
    except:
        pass
    os.system('dropdb -U postgres uvm')
    os.system('createdb -U postgres uvm')
    os.system('cat %s/usr/share/untangle/tests/untangle-node-reporting/emptydb.sql | psql -U postgres uvm' % PREFIX)
    os.system('%s/usr/lib/python2.5/reports/process.py --date=2009-6-3' % PREFIX)

class TestReports(UvmSetup):

    def test_cvs_to_png(self):
        csv = sets.Set()
        png = sets.Set()

        arg = { 'csv': csv, 'png': png }

        os.path.walk("%s/usr/share/untangle/web/reports/data" % PREFIX,
                     self.__walk_fn, arg)

        diff = csv - png

        print "missing pngs:"
        for p in png:
            print "  %s" % p

        assert len(diff) == 0

    def __walk_fn(self, arg, dirname, fnames):
        for f in fnames:
            if f.endswith('.csv'):
                arg['csv'].add('%s/%s' % (dirname, os.path.splitext(f)[0]))
            elif f.endswith('.png'):
                arg['png'].add('%s/%s' % (dirname, os.path.splitext(f)[0]))

