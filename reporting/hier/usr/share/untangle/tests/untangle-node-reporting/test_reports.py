import os
import sets
import shutil

from untangle.ats.uvm_setup import UvmSetup

PREFIX = "@PREFIX@"

def setup_module(module):
    shutil.rmtree("%s/usr/share/untangle/web/reports/data" % PREFIX)
    os.system('dropdb -U postgres uvm')
    os.system('createdb -U postgres uvm')
    os.system('bzcat %s/usr/share/untangle/tests/untangle-node-reporting/dogfood.sql.bz2 | psql -U postgres uvm' % PREFIX)
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

