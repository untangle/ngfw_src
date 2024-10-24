#!/usr/bin/python3 -u
import uvm
import sys

uvm = uvm.Uvm().getUvmContext()
appManager = uvm.appManager()

app = appManager.app( "reports" )

if app == None:
    print("reports app not installed.")
    sys.exit(0)

app.logReportsData()
sys.exit(0)
