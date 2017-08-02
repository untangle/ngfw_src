#!@PREFIX@/usr/share/untangle/bin/ut-pycli -f
import uvm
import sys
import traceback

uvm = uvm.Uvm().getUvmContext(timeout=600)

reportsApp = uvm.appManager().app("reports");
if reportsApp == None:
    print "Reports not installed"
    sys.exit(1)

try:
    reportsApp.runFixedReport()
except:
    # can timeout - just ignore
    pass
