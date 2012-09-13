#! /usr/bin/ruby1.8

require 'gpgme'
require 'optparse'

# The format of a UID is:
#   xxxx-xxxx-xxxx-xxPV-xxxxxxx-xxxxxxx-xxxxPVx
#   P = platform bit
#   V = version bit
#   x = uuid bit
# see VERSIONS and PLATFORMS below for the actual meaning of those
# bits

#############################
# constants

# bits
PLATFORMS = { :sarge    => 0,
              :etch     => 1,
              :sid      => 2,
              :feisty   => 3,
              :gutsy    => 4,
              :hardy    => 5,
              :intrepid => 6,
              :lenny    => 7,
              :squeeze  => 8 }

VERSIONS = { :hardware => 1,
             :iso      => 2,
             :pkgs     => 3,
             :win      => 4,
             :mac      => 5,
             :lin      => 6,
             :ubuntu   => 9 }

# workaround for <1.0 gpgme
OLDGPGME = !defined?(GPGME::Ctx)

# files
UNTANGLE = "@PREFIX@/usr/share/untangle"
UNTANGLE_GPG_HOME = File.join(UNTANGLE, "gpg")
UID_FILE = File.join(UNTANGLE, "conf/uid")
ENTROPY_AVAILABLE_FILE = "/proc/sys/kernel/random/entropy_avail"

# minimum entropy needed to generate our key
MINIMUM_ENTROPY = 3000

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

def hasEnoughEntropy
  return (File.new(ENTROPY_AVAILABLE_FILE).read.to_i > MINIMUM_ENTROPY)
end

def createGpgKeyring(config)
  if hasEnoughEntropy && !OLDGPGME then 
    puts "GPGME"
    createGpgKeyringWithGPGME(config)
  else
    puts "GPG"
    createGpgKeyringWithGPG(config)
  end
end

def createGpgKeyringWithGPGME(config)
  ctx = GPGME::Ctx.new
  ctx.genkey(config, nil, nil)
end

def createGpgKeyringWithGPG(config)
  IO.popen("gpg --batch --quick-random --gen-key 2> /dev/null", 'w+') { |f|
    f.puts(config.gsub!(/^<.*/, '')) # gpg doesn't want the XML-style config
  }
  # exceptions occur for lots of reasons, like running the first time.
  # raise Exception.new if $? != 0
end

def getPlatform
  case File.open("/etc/issue").read
  when %r|lenny/sid|
    :sid
  when /4.0/
    :etch
  when /3\.1/
    :sarge
  when /5\.0/
    :lenny
  when /6\.0/
    :squeeze
  when /(7\.04|feisty)/
    :feisty
  when /(7\.10|gutsy)/
    :gutsy
  when /(8\.04|hardy)/
    :hardy
  when /(8\.10|intrepid)/
    :intrepid
  else
    raise "Couldn't get your platform."
  end
end

# ACTIVATION_FILE = File.join(UNTANGLE, "activation.key")

# def readExistingActivationKey
#   existingLicenseKey = nil
#   if File.file?(ACTIVATION_FILE) then
#     existingLicenseKey = File.open(ACTIVATION_FILE).read.strip.gsub(/-/, "")
#     existingLicenseKey = nil if existingLicenseKey =~ /^[0\-]+$/
#   end
#   return existingLicenseKey
# end

def getPackageVersion(name)
  line = `dpkg-query --showformat='${Version}' -W #{name} 2> /dev/null`
  version = line.empty? ? nil : line
  return version
end

def getVersion
  if !getPackageVersion('untangle-windows-installer').nil? 
    :win
  elsif !getPackageVersion('untangle-mac-installer').nil? 
    :mac
  elsif !getPackageVersion('untangle-linux-installer').nil? 
    :lin
  elsif !getPackageVersion('untangle').nil? 
    :ubuntu
  elsif getPackageVersion('untangle-gateway').nil?
    :pkgs
  elsif getPackageVersion('untangle-hardware-support').nil?
    :iso
  else
    :hardware
  end
end

def getBits(typeNibble)
  typeNibble = VERSIONS[getVersion] if typeNibble.nil?
  return "#{PLATFORMS[getPlatform]}#{typeNibble}"
end

def createUID(fingerprint, bits, existingKey)
  # insert meaningful bit in the left part
  if existingKey.nil? then
    uid = fingerprint.insert(LEFT_LENGTH-2, bits)
  else # FIXME
    uid = existingKey + fingerprint[LEFT_LENGTH-2,fingerprint.length-LEFT_LENGTH+2]
  end

  # insert meaningful bit in the right part
  uid = uid.insert(-2, bits).downcase

  # group and join
  one = uid[0,LEFT_LENGTH].scan(/.{4}/)
  two = uid[LEFT_LENGTH,uid.length-LEFT_LENGTH].scan(/.{7}/)
  return [one, one+two].map{ |list| list.join("-") }
end

def writeToFiles(shortUID, uid)
#  File.open(ACTIVATION_FILE, 'w') { |f| f.puts(shortUID) }
  File.open(UID_FILE, 'w') { |f| f.puts(shortUID) }
end

#######################
# main

options = parseCommandLineArgs(ARGV)

# GPG env.
ENV['GPG_AGENT_INFO'] = nil
ENV['GNUPGHOME'] = UNTANGLE_GPG_HOME

# create the keyring only if no keys are available
firsttime = true
begin
  fingerprint = nil
  if (OLDGPGME)
    IO.popen("gpg --with-colons --fingerprint 2> /dev/null") { |f|
      f.each do |input|
        if (input.index('fpr') == 0)
          arr = input.split(':');
          if (arr.length > 1)
            fingerprint=arr[arr.length - 2];
            break
          end
        end
      end
    }
    raise NoMethodError if fingerprint == nil
  else
    key = GPGME.list_keys[0].subkeys[0]
    fingerprint = key.fingerprint
  end
rescue NoMethodError
  if (firsttime)
    firsttime = false
    createGpgKeyring(GPG_KEY_CONFIG) # create the keyring
    retry
  end
  puts "unable to get fingerprint"
  exit 1
end

bits = getBits(options[:typeNibble]) # what we'll embed in the uid

# existingLicenseKey = readExistingActivationKey # re-use that if it's there

# shortUID, uid = createUID(fingerprint, bits, existingLicenseKey)
shortUID, uid = createUID(fingerprint, bits, nil)

puts shortUID, uid
writeToFiles(shortUID, uid)
