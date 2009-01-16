require "singleton"

class MessageFormatter
  include Singleton

  def format( message )
    java_class = message["javaClass"]
    method_name = "format_#{java_class.gsub( /[^0-9a-zA-Z]/, "_" )}" 
    return "\tunknown type '#{method_name}'" unless self.class.method_defined?( method_name )
    
    self.send( method_name, message )      
  end

  def format_com_untangle_uvm_license_LicenseUpdateMessage( message )
    message["mackageMap"]["map"].map do |mackage_name, license_status|
      "\t mackage : #{mackage_name}, '#{license_status["type"]}', #{license_status["timeRemaining"]}"
    end.join( "\n" )
  end

  def format_com_untangle_uvm_message_Stats_FixedCounts( message )
    "\tcount: #{message["count"]}\tcount-since-midnight: #{message["countSinceMidnight"]}"
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

message_queue = rmm.getMessageQueue( messageKey, nil )

message_queue["messages"]["list"].each do |message|
  puts <<EOF
#{message.keys.join( " , " )}
Received the message: #{message["javaClass"]}
#{MessageFormatter.instance.format( message )}
EOF
end

node_manager = Untangle::RemoteUvmContext.nodeManager()

message_queue["stats"]["map"].each do |tid,stats|
  puts "TID : #{tid}"
  
  stats["metrics"]["map"].each do |name,counter_stats|
    puts( "%10s : %s" % [ name, MessageFormatter.instance.format( counter_stats )])
  end
end

