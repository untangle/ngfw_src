#!@PREFIX@/usr/share/untangle/bin/ut-pycli -f 
import sys

nodeManager = uvm.nodeManager()

node = nodeManager.node( "configuration-backup" )

if node == None:
    print "configuration-backup node not installed."
    sys.exit(0)

node.sendBackup()
sys.exit(0)
