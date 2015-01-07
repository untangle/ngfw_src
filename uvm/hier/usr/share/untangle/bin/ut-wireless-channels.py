#!@PREFIX@/usr/share/untangle/bin/ut-pycli -f 

networkManager = uvm.networkManager()

channels = networkManager.getWirelessChannels( "wlan0" )
print channels


