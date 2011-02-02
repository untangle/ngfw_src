#
# simple rush script

im = Untangle::RemoteUvmContext.intfManager()

enumeration = im.getIntfMatcherEnumeration()

enumeration.each do |intf_matcher|
  puts "Interface Matcher: #{intf_matcher} #{intf_matcher.class}"
end

