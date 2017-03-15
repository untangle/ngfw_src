#!@PREFIX@/usr/share/untangle/bin/ut-pycli -f 
import sys

appManager = uvm.appManager()

node = appManager.app( "smtp" )

if node == None:
    print "SMTP Casing not installed."
    sys.exit(0)

node.sendQuarantineDigests()
sys.exit(0)

