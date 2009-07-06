import os
import sets

from untangle.ats.uvm_setup import UvmSetup

PREFIX = "@PREFIX@"

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

