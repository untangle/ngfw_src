am = Untangle::RemoteUvmContext.adminManager()

settings = am.getAdminSettings()

users = settings["users"]["set"].values

ARGV.each do |login_name|
  user = users.find { |v| v["login"] == login_name  }
  
  next puts "Unable to find the login '#{login_name}'" if user.nil?
  
  puts "Found the user '#{login_name}', (#{user["id"]}) setting to read only."
  user["readOnly"] = true
end

am.setAdminSettings( settings )

