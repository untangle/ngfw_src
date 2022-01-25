#!/usr/bin/python3 -u
import uvm

uvm = uvm.Uvm().getUvmContext()
print(uvm.networkManager().getWirelessChannels( "wlan0" ))


