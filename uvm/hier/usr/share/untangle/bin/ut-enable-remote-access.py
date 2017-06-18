#!/usr/bin/python -u
import uvm
import sys

uvm = uvm.Uvm().getUvmContext()

# Looks for a rule named "Allow HTTPS on WANs" and enables it if it finds it.
networkManager = uvm.networkManager()
network_settings =  networkManager.getNetworkSettings()

found = False
access_rules = network_settings["accessRules"]["list"]
if access_rules != None:
    for rule in access_rules:
        if rule.get("description") == "Allow HTTPS on WANs":
            if rule.get("enabled") != None:
                print('remote administration is currently : %s' % rule.get("enabled"))
            rule["enabled"] = True
            found = True

if not found:
    print('Found no "Allow HTTPS on WANs" rule')
    sys.exit(0)
                
networkManager.setNetworkSettings( network_settings )

network_settings =  networkManager.getNetworkSettings()

access_rules = network_settings["accessRules"]["list"]
if access_rules != None:
    for rule in access_rules:
        if rule.get("description") == "Allow HTTPS on WANs":
            if rule.get("enabled") != None:
                print('remote administration is now       : %s' % rule.get("enabled"))

