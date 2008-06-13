## A rush shell for testing adding and removing rules and loading settings

## (Wherever the rush shell is)
## To run use:  QUARANTINE_TOKEN="key" ./dist/usr/bin/rush ./mail-casing/unittest/get_quarantine.rb 

require 'java'
nm = RUSH.uvm.nodeManager()
tid = nm.nodeInstances( 'untangle-casing-mail' ).first
mail = nm.nodeContext( tid ).node

token = ENV["QUARANTINE_TOKEN"]
puts "Retrieving all of the messages from the quarantine"
mail.getInboxRecords( token, -1, 60, "subject", false ).each do |r|
  puts "#{r.getMailID}, #{r.getMailSummary}"
end

