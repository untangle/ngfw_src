#!@PREFIX@/usr/share/untangle/bin/ut-pycli -f
import uvm
import sys

uvm = uvm.Uvm().getUvmContext()
print(uvm.getServerUID())

