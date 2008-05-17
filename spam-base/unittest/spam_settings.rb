## A rush shell for testing adding and removing rules and loading settings

## (Wherever the rush shell is)
## To run use: ./dist/usr/bin/rush spam_settings.rb 

require 'java'
nm = RUSH.uvm.nodeManager()
tid = nm.nodeInstances( 'untangle-node-spamassassin' ).first
spam = nm.nodeContext( tid ).node
base_settings = spam.getBaseSettings
smtp_config = base_settings.getSmtpConfig
notify_sender = com.untangle.node.spam.SpamSMTPNotifyAction.getInstance( "notify sender" )
do_not_notify = com.untangle.node.spam.SpamSMTPNotifyAction.getInstance( "do not notify" )
action = smtp_config.getNotifyAction
puts "Current Notify action: #{smtp_config.getNotifyAction}"

smtp_config.setNotifyAction(( action.getKey == do_not_notify.getKey ) ? notify_sender : do_not_notify )

puts "New Notify action: #{smtp_config.getNotifyAction}"

spam.setBaseSettings( base_settings )

rblList = spam.getSpamRBLList( 0, 100, java.lang.String[0].new )

rblList.each { |rbl|  puts "#{rbl.getHostname} / #{rbl.getDescription}" }

list_size = rblList.size

added = java.util.LinkedList.new
deleted = java.util.LinkedList.new
modified = java.util.LinkedList.new

deleted.add( rblList[0].getId )

rbl = com.untangle.node.spam.SpamRBL.new
rbl.setHostname( "foo.untangle.com" )
rbl.setActive( false )
rbl.setDescription( "I am the new fooness, now here me roar. #{Time.now}" )
added.add( rbl )

spam.updateSpamRBLList( added, deleted, modified );

# l = java.util.List[3].new
# l[0] = added
# l[1] = deleted
# l[2] = modified
# spam.updateAll( nil, l );
  
rblList = spam.getSpamRBLList( 0, 100, java.lang.String[0].new )

rblList.each { |rbl|  puts "#{rbl.getHostname} / #{rbl.getDescription}" }
