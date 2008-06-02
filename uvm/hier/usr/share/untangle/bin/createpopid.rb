#! /usr/bin/ruby1.8

require 'gpgme'
require 'optparse'

# The format of a popid is:
#   xxxx-xxxx-xxxx-xPVx-xxxxxxx-xxxxxxx-xxxxPVx
#   P = platform bit
#   V = version bit
# see VERSIONS and PLATFORMS below for the actual meaning of those
# bits

#############################
# constants

# bits
VERSIONS = { :old      => 0,
             :hardware => 1,
             :cd       => 2,
             :lite     => 3,
             :ubuntu   => 9 }

PLATFORMS = { :sarge    => 0,
              :etch     => 1,
              :sid      => 2,
              :feisty   => 3,
              :gutsy    => 4,
              :hardy    => 5 }

# files
UNTANGLE = "@PREFIX@/usr/share/untangle"
UNTANGLE_GPG_HOME = File.join(UNTANGLE, "gpg")
ACTIVATION_FILE = File.join(UNTANGLE, "activation.key")
POPID_FILE = File.join(UNTANGLE, "popid")

# length of the left part
LEFT_LENGTH = 16

# default GPG key config
GPG_KEY_CONFIG = <<EOF
<GnupgKeyParms format="internal">
Key-Type: RSA
Key-Length: 2048
Name-Real: Untangle
Name-Email: untangle@untangle.com
Expire-Date: 0
</GnupgKeyParms>
EOF

#############################
# functions

def parseCommandLineArgs(args)
  options = { :typeNibble => nil }
  
  opts = OptionParser.new { |opts|
    opts.on("--type-nibble=TYPE", VERSIONS.keys, "force the type nibble #{VERSIONS.keys.map {|k| k.to_s}.inspect}") { |t|
      options[:typeNibble] = VERSIONS[t]
    }
  }

  begin
    opts.parse!(args)
  rescue Exception => e
    puts e, "", opts
    exit
  end
  
  return options
end

def createGpgKeyring(config)
  ctx = GPGME::Ctx.new
  ctx.genkey(config, nil, nil)
end

def getPlatform
  case File.open("/etc/issue").read
  when %r|lenny/sid|
    :sid
  when /4.0/
    :etch
  when /3\.1/
    :sarge
  when /(7\.04|feisty)/
    :feisty
  when /(7\.10|gutsy)/
    :gutsy
  when /(8\.04|hardy)/
    :hardy
  else
    raise "Couldn't get your platform."
  end
end

def readExistingActivationKey
  existingLicenseKey = nil
  if File.file?(ACTIVATION_FILE) then
    existingLicenseKey = File.open(ACTIVATION_FILE).read.strip.gsub(/-/, "")
    existingLicenseKey = nil if existingLicenseKey =~ /^[0\-]+$/
  end
  return existingLicenseKey
end

def getPackageVersion(name)
  lines = `dpkg -l #{name} 2> /dev/null`.split(/\n/)
  line = lines.grep(/^ii/)[0]
  return line.nil? ? nil : line.split[2]
end

def getVersion
  if getPackageVersion('untangle-vm') =~ /^5\.3/
    if getPackageVersion('untangle-gateway-light').nil? then
      :lite
    elsif getPackageVersion('untangle-system-config').nil?
      :cd
    else
      :hardware
    end
  else
    :old
  end
end

def getBits(typeNibble)
  typeNibble = VERSIONS[getVersion] if typeNibble.nil?
  return "#{PLATFORMS[getPlatform]}#{typeNibble}"
end

def createPopId(fingerprint, bits, existingKey)
  # insert meaningful bit in the left part
  if existingKey.nil? then
    popId = fingerprint.insert(LEFT_LENGTH-2, bits)
  else # FIXME
    popId = existingKey + fingerprint[LEFT_LENGTH-2,fingerprint.length-LEFT_LENGTH+2]
  end

  # insert meaningful bit in the right part
  popId = popId.insert(-2, bits).downcase

  # group and join
  one = popId[0,LEFT_LENGTH].scan(/.{4}/)
  two = popId[LEFT_LENGTH,popId.length-LEFT_LENGTH].scan(/.{7}/)
  return [one, one+two].map{ |list| list.join("-") }
end

def writeToFiles(activationKey, popId)
  File.open(ACTIVATION_FILE, 'w').puts(activationKey)
  File.open(POPID_FILE, 'w').puts(popId)
end

#######################
# main

options = parseCommandLineArgs(ARGV)

# GPG env.
ENV['GPG_AGENT_INFO'] = nil
ENV['GNUPGHOME'] = UNTANGLE_GPG_HOME

# create the keyring only if no keys are available
begin
  key = GPGME.list_keys[0].subkeys[0]
rescue NoMethodError
  createGpgKeyring(GPG_KEY_CONFIG) # create the keyring
  retry
end

fingerprint = key.fingerprint

bits = getBits(options[:typeNibble]) # what we'll embed in the popid

existingLicenseKey = readExistingActivationKey # re-use that if it's there

activationKey, popId = createPopId(fingerprint, bits, existingLicenseKey)

puts activationKey, popId
writeToFiles(activationKey, popId)
