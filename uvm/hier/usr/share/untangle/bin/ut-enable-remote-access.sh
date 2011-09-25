#!@PREFIX@/usr/share/untangle/bin/ut-pycli -f 

nm = uvm.networkManager()

access_settings =  nm.getAccessSettings()
is_enabled = access_settings["isOutsideAdministrationEnabled"]
print('remote administration is currently: %s' % is_enabled)

access_settings["isOutsideAdministrationEnabled"] = True;
nm.setAccessSettings( access_settings )

access_settings =  nm.getAccessSettings()
is_enabled = access_settings["isOutsideAdministrationEnabled"]
print('remote administration is now      : %s' % is_enabled)
