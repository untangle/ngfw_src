#!/usr/bin/python -u
import uvm
import sys

uvm = uvm.Uvm().getUvmContext()

system_manager = uvm.systemManager()
system_settings = system_manager.getSettings()

if system_settings.get('cloudEnabled') and system_settings.get('supportEnabled'):
    print("Support access alread enabled")
    sys.exit(0)

system_settings['cloudEnabled'] = True
system_settings['supportEnabled'] = True
system_manager.setSettings(system_settings)
sys.exit(0)



