#!@PREFIX@/usr/share/untangle/bin/ut-pycli -f 

systemManager = uvm.systemManager()

system_settings =  systemManager.getSettings()
is_enabled = system_settings["outsideHttpsEnabled"]
print('remote administration is currently: %s' % is_enabled)

system_settings["outsideHttpsEnabled"] = True;
systemManager.setSettings( system_settings )

system_settings =  systemManager.getSettings()
is_enabled = system_settings["outsideHttpsEnabled"]
print('remote administration is now      : %s' % is_enabled)
