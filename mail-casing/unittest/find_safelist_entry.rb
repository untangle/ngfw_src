#
# simple rush script

first_flag = ARGV[0]
if ( first_flag  == "--help" || first_flag == "'h" )
  puts <<EOF
USAGE: rush find_safelist_entry.rb [<address>]+
\t<address> : is a regular expression for the address to search for.
\tThis will print every entry in the safelist that matches as well as the user that has
\tthe entry in there safelist.
EOF
  exit
end

## Convert all of the arguments into regular expressions
matches = ARGV.map { |regex| /#{regex}/ }

nm = Untangle::RemoteUvmContext.nodeManager()
tid = nm.nodeInstances( 'untangle-casing-mail' ).first
mail = nm.nodeContext( tid ).node

safelist_end_user_view = mail.getSafelistEndUserView()

mail_settings = mail.getMailNodeSettings()

mail_settings["safelistSettings"]["list"].each do |safelist|
  recipient = safelist["recipient"]["addr"]

  safelist_end_user_view.getSafelistContents( recipient ).each do |safelisted_address|
    if ( matches.length > 0 ) 
      print_entry = false
      matches.each { |regex| break print_entry = true if regex.match( safelisted_address ) }

      next unless print_entry
    end

    puts "(user,safelisted_address): '#{recipient}' -> '#{safelisted_address}'"
  end
end
