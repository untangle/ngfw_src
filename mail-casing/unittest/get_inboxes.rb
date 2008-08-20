## A rush shell for testing the new quarantine API methods.

## (Wherever the rush shell is)
## To run use: ./dist/usr/bin/rush ./mail-casing/unittest/get_inboxes.rb 

nm = Untangle::RemoteUvmContext.nodeManager()
tid = nm.nodeInstances( 'untangle-casing-mail' ).first
mail = nm.nodeContext( tid ).node

puts "Retrieving the first 10 inboxes"
quarantine = mail.getQuarantineMaintenenceView()
puts "total size: #{quarantine.getInboxesTotalSize()}"

inboxArray = quarantine.getInboxArray( 0, 10, "", true )

puts "array: #{inboxArray["inboxes"].size()} #{inboxArray["totalRecords"]}"

inboxArray["inboxes"].each do |inbox|
  address = inbox["address"]
  puts "inbox: #{address}"

  inboxRecordArray = quarantine.getInboxRecordArray( address, 0, 3, "", true )

  inboxRecordArray["inboxRecords"].each do |record|
    puts " record: #{record["mailID"]}, (#{record["recipients"]})"
  end
end




