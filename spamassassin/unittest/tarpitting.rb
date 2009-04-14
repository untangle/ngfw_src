
##  Modify the RequestHandler class a little bit
class Untangle::RequestHandler
  attr_reader :opener
end

def disable_tarpitting( host, username, password )
  ## Build a new remote context
  handler = Untangle::RequestHandler.new

  handler.opener.post( "https://#{host}/auth/login?url=/webui/&realm=Administrator", { "username" => username, "password" => password } )  
  
  remote_uvm_context = Untangle::ServiceProxy.new( "https://#{host}/webui/JSON-RPC", "RemoteUvmContext", handler )

  nm = remote_uvm_context.nodeManager()
  nm.nodeInstances( "untangle-node-spamassassin" ).each do |tid|
    spamassassin = nm.nodeContext( tid ).node
    settings = spamassassin.getBaseSettings()
    settings["smtpConfig"]["throttle"] = false
    spamassassin.setBaseSettings( settings )
  end
end

STDIN.readlines.each do |line|
  host, username, password = line.strip.split
  begin
    disable_tarpitting( host, username, password )
    puts "Sucessfully disabled tarpitting on #{host}"
  rescue
    puts "Unable to disable tarpitting on #{host}"
  end
end



    
