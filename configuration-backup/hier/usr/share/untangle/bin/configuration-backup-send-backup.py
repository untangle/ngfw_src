#!/usr/bin/python3 -u
import uvm
import sys

uvm = uvm.Uvm().getUvmContext()
appManager = uvm.appManager()

app = appManager.app( "configuration-backup" )

if app == None:
    print("configuration-backup app not installed.")
    sys.exit(0)

app.sendBackup()
sys.exit(0)
