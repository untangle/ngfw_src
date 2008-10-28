require "singleton"

class MessageFormatter
  include Singleton

  def format( message )
    java_class = message["javaClass"]
    method_name = "format_#{java_class.gsub( ".", "_" )}" 
    return "\tunknown type" unless self.class.method_defined?( method_name )
    
    self.send( method_name, message )      
  end

  def format_com_untangle_uvm_license_LicenseUpdateMessage( message )
    message["mackageMap"]["map"].map do |mackage_name, license_status|
      "\t mackage : #{mackage_name}, '#{license_status["type"]}', #{license_status["timeRemaining"]}"
    end.join( "\n" )
  end
end

rmm = Untangle::RemoteUvmContext.messageManager()

messageKey = ARGV[0]

if messageKey.nil?
  messageKey = rmm.getMessageKey() 
else
  messageKey = messageKey.to_i
end

puts "Using the message key #{messageKey}"

message_queue = rmm.getMessageQueue( messageKey )

message_queue["messages"]["list"].each do |message|
  puts <<EOF
#{message.keys.join( " , " )}
Received the message: #{message["javaClass"]}
#{MessageFormatter.instance.format( message )}
EOF
end

  





