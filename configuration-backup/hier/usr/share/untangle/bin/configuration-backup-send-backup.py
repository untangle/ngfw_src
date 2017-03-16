#!@PREFIX@/usr/share/untangle/bin/ut-pycli -f 
import sys

appManager = uvm.appManager()

app = appManager.app( "configuration-backup" )

if app == None:
    print "configuration-backup app not installed."
    sys.exit(0)

app.sendBackup()
sys.exit(0)
