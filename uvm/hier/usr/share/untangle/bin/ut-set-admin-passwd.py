#!/usr/bin/python -u
import uvm
import sys

if len(sys.argv) < 2:
    print("Usage: %s password" % sys.argv[0])
    sys.exit(1)

uvm = uvm.Uvm().getUvmContext()

admin_manager = uvm.adminManager()
admin_settings =  admin_manager.getSettings()

users = admin_settings.get("users").get("list")
if users == None:
    print("No admin user found in settings!")
    sys.exit(1)
    
for user in users:
    if user.get('username') == 'admin':
        user['password'] = sys.argv[1]
        admin_manager.setSettings(admin_settings)
        sys.exit(0)

print("No admin user found in settings!")
sys.exit(1)
        
