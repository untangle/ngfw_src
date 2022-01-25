#!/usr/bin/python3 -u
import uvm
import sys

uvm = uvm.Uvm().getUvmContext()

# Looks for a rule named "Allow HTTPS on WANs" and enables it if it finds it.
networkManager = uvm.networkManager()
network_settings =  networkManager.getNetworkSettings()

found = False
already_enabled = True
access_rules = network_settings["accessRules"]["list"]
if access_rules != None:
    for rule in access_rules:
        if rule.get("description") == "Allow HTTPS on WANs":
            if rule.get("enabled") != None:
                print('Allow HTTPS is currently : %s' % rule.get("enabled"))
                if not rule.get("enabled"):
                    already_enabled = False
            rule["enabled"] = True
            found = True
        if rule.get("description") == "Allow SSH":
            if rule.get("enabled") != None:
                print('Allow SSH   is currently : %s' % rule.get("enabled"))
                if not rule.get("enabled"):
                    already_enabled = False
            rule["enabled"] = True
            found = True
            
if not found:
    print('Found no access rules')
    sys.exit(0)

if already_enabled:
    print('Remote access already enabled')
    sys.exit(0)

networkManager.setNetworkSettings( network_settings )
network_settings =  networkManager.getNetworkSettings()

access_rules = network_settings["accessRules"]["list"]
if access_rules != None:
    for rule in access_rules:
        if rule.get("description") == "Allow HTTPS on WANs":
            if rule.get("enabled") != None:
                print('Allow HTTPS is now       : %s' % rule.get("enabled"))
        if rule.get("description") == "Allow SSH":
            if rule.get("enabled") != None:
                print('Allow SSH   is now       : %s' % rule.get("enabled"))

