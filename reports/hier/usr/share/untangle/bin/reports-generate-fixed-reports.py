#!@PREFIX@/usr/share/untangle/bin/ut-pycli -f
import uvm
import sys
import traceback

uvm = uvm.Uvm().getUvmContext(timeout=600)

reportsApp = uvm.appManager().app("reports");
if reportsApp == None:
    print("Reports not installed")
    sys.exit(1)

if reportsApp.getRunState() != 'RUNNING':
    print("Reports not running")
    sys.exit(1)

try:
    reportsApp.runFixedReports()
except Exception as e:
    # can timeout - just ignore
    print(e)
    pass
