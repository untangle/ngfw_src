## A rush script for getting the openvpn client link

## (Wherever the rush shell is)
## To run use: ./dist/usr/bin/rush ./spyware/unittest/validator.rb 
def test_validator( validator, value, expected )
  result = validator.validate( { "list" => [ value ], "javaClass" => "java.util.ArrayList" } )
  if ( result["valid"] != expected ) 
    puts "The value '#{value}' did not validate properly #{expected}, #{result["valid"]}"
  else 
    ## puts "The value '#{value}' is #{result ? "valid" : "invalid"}"
  end
end

nm = Untangle::RemoteUvmContext.nodeManager()
tid = nm.nodeInstances( 'untangle-node-spyware' ).first
spyware = nm.nodeContext( tid ).node

puts "This should only print if there is an error."

v = spyware.getValidator()

test_validator( v, "1.2.3.3", true )
test_validator( v, " 1.2.3.3 ", true )
test_validator( v, "1.2.3.3 ", true )


(0..32).each do |n|
  test_validator( v, "1.2.3.3/#{n}", true )
  test_validator( v, " 1.2.3.3/#{n}", true )
  test_validator( v, "1.2.3.3 / #{n}", true )
  test_validator( v, " 1.2.3.3 / #{n}", true )
  test_validator( v, "1.2.3.3 / #{n} ", true )
end

test_validator( v, " 1.2.3.3 mask 0xFF000000", true )
test_validator( v, " 1.2.3.3 mask 0xFF000000 ", true )
