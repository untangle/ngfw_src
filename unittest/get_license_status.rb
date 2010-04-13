## A rush shell for testing the new quarantine API methods.

## (Wherever the rush shell is)
## To run use: sudo ./dist/usr/bin/rush ./license/unittest/get_license_status.rb 

identifiers = [
               "untangle-dual-virus-blocker-kav",
               "untangle-remote-access-portal",
               "untangle-configuration-backup", 
               "untangle-user-directory-integration", 
               "untangle-user-directory-management", 
               "untangle-policy-manager", 
               "untangle-branding-manager", 
               "untangle-pcremote",
              ]

license_manager = Untangle::RemoteUvmContext.licenseManager()

identifiers.each do |identifier|
  status = license_manager.getLicenseStatus( identifier )
  puts "#{identifier}: type #{status["type"]}, #{status["timeRemaining"]}"
end


