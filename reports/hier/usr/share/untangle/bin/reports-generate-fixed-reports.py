#!/usr/share/untangle/bin/ut-pycli -f 

import sys

reportsApp = uvm.nodeManager().node("untangle-node-reports");
reportsApp.runFixedReport()
