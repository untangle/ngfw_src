#!@PREFIX@/usr/share/untangle/bin/ut-pycli -f 

networkManager = uvm.networkManager()

network_settings =  networkManager.getNetworkSettings()
is_enabled = network_settings["outsideHttpsEnabled"]
print('remote administration is currently: %s' % is_enabled)

network_settings["outsideHttpsEnabled"] = True;
networkManager.setNetworkSettings( network_settings )

network_settings =  networkManager.getNetworkSettings()
is_enabled = network_settings["outsideHttpsEnabled"]
print('remote administration is now      : %s' % is_enabled)
