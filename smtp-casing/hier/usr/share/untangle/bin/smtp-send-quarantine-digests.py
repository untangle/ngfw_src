#!/usr/bin/python -u
import uvm
import sys

uvm = uvm.Uvm().getUvmContext(timeout=600)
app = uvm.appManager().app( "smtp" )

if app == None:
    print("SMTP Casing not installed.")
    sys.exit(0)

app.sendQuarantineDigests()
sys.exit(0)

