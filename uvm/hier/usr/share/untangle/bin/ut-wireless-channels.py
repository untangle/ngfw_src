#!/usr/bin/python -u
import uvm

uvm = uvm.Uvm().getUvmContext()
print(uvm.networkManager().getWirelessChannels( "wlan0" ))


