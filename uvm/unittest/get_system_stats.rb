mm = Untangle::RemoteUvmContext.messageManager()

system_stats = mm.getSystemStats()

system_stats["map"].each do |k,v| 
  puts "#{k} => #{v}"
end
