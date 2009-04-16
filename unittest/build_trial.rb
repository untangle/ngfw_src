#!/usr/bin/env ruby

require "md5"

module LicenseBuilder
  Trial = "14 Day Trial"
  Subscription = "Subscription"

  SelfSignedKeyVersion = 31

  ## Licenses of various types must be a certain duration, the final + are designed just so that
  ## start and end don't end in the same final three digits.
  DurationMap = {
    Trial => ( 60 * 60 * 24 * 15 ) + 0.278,
    Subscription => ( 60 * 60 * 24 * 365 * 2 ) + 0.344
  }
  
  class License
    def initialize( identifier, mackage, startTime = nil, licenseType = Trial )
      startTime = Time.new if startTime.nil?
      @identifier = identifier
      @mackage = mackage      
      @version = SelfSignedKeyVersion
      @licenseType = licenseType

      ## Update the start time
      self.startTime = startTime
    end

    def startTime=(newValue)
      @startTime = newValue
      # calculate the new end time
      @endTime = @startTime + duration
    end

    def endTime=(newValue)
      @endTime = newValue
      # calculate the new start time
      @startTime = @endTime - duration
    end

    def duration
      DurationMap[@licenseType]
    end

    def firstKey
      digest( "#{@identifier}-license-#{0xDEC0DED}-#{@licenseType}" )
    end

    def key
      license = <<THE_LICENSE_TERMINATOR
#{firstKey}
untangle: 234363866
version: #{@version}
type: #{@licenseType}
product: #{@identifier}
start: #{startTimeMillis}
end: #{endTimeMillis}
THE_LICENSE_TERMINATOR
   ##
      digest( license )
   end

## Get a license
    def license
      ## Generate a new license key
      license = <<THE_LICENSE_TERMINATOR
com.untangle.uvm.license.identifier=#{@identifier}
com.untangle.uvm.license.type=#{@licenseType}
com.untangle.uvm.license.mackage=#{@mackage}
com.untangle.uvm.license.start=#{startTimeMillis}
com.untangle.uvm.license.end=#{endTimeMillis}
com.untangle.uvm.license.key=#{key}
com.untangle.uvm.license.keyVersion=#{@version}
THE_LICENSE_TERMINATOR

      license
    end
    
    def startTimeMillis
      toMillis( @startTime )
    end

    def endTimeMillis
      toMillis( @endTime )
    end

    def toMillis( time )
      ( time.to_f * 1000 ).to_i
    end

    def digest(value)
      MD5.digest( value ).unpack( "C*" ).map { |b| sprintf( "%02x", b ) }.join
    end
    
    attr_reader :identifer, :mackage, :startTime, :endTime, :version
  end
end

identifier = ARGV[0]
mackage = ARGV[1]

daysPassed = 0
daysPassed = ARGV[2].to_i unless ARGV[2].nil?
startTime = Time.new - ( daysPassed * 60 * 60 * 24 ) 


licenseType = LicenseBuilder::Trial
licenseType = ARGV[3] unless ARGV[3].nil?


l = LicenseBuilder::License.new( identifier, mackage, startTime, licenseType )

puts l.license
