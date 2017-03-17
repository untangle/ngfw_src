#!@PREFIX@/usr/share/untangle/bin/ut-pycli -f 
import sys

appManager = uvm.appManager()

app = appManager.app( "smtp" )

if app == None:
    print "SMTP Casing not installed."
    sys.exit(0)

app.sendQuarantineDigests()
sys.exit(0)

