#! /usr/bin/ruby

# Sebastien Delafond <seb@untangle.com>

require 'net/smtp'

# constants
REP = "/var/www/untangle"
DISTS = "#{REP}/dists"
INCOMING = "#{REP}/incoming"
PROCESSED = "#{REP}/processed"
FAILED = "#{PROCESSED}/failed"

DISTRIBUTIONS = Dir.entries(DISTS).delete_if { |f| f =~ /\./ }
DEFAULT_DISTRIBUTION = "mustang"
DEFAULT_COMPONENT = "upstream"
DEFAULT_MAIL_RECIPIENTS = [ "rbscott@untangle.com", "seb@untangle.com" ]
DEFAULT_SECTION = "utils"
DEFAULT_PRIORITY = "normal"

# global functions
def email(recipients, subject, body)
  recipients.delete_if { |r| not r =~ /@untangle\.com/ }
  recipients.map! { |r|
    r.gsub(/.*?<(.*)>/, '\1')
  }
  recipientsString = recipients.join(',')
  myMessage = <<EOM
From: Incoming Queue Daemon <seb@untangle.com>
To: #{recipientsString}
Subject: #{subject}

#{body}
EOM

  Net::SMTP.start('localhost', 25, 'localhost.localdomain') { |smtp|
    smtp.send_message(myMessage,"seb@untangle.com",*recipients)
  }
end

# Custom exceptions
class UploadFailure < Exception
end
class UploadFailureByPolicy < UploadFailure
end
class UploadFailureNoSection < UploadFailure
end
class UploadFailureNoPriority < UploadFailure
end
class UploadFailureAlreadyUploaded < UploadFailure
end

class DebianUpload # Main base class

  @@doEmailSuccess = true
  @@doEmailFailure = true

  attr_reader :files, :distribution, :uploader, :version

  def initialize(file, move = true)
    @file = file
    @move = move
    @files = [ @file ]
  end

  def to_s
    s = "#{@file}\n"
    s += "  distribution = #{@distribution}\n"
    s += "  component = #{@component}\n"
    s += "  maintainer = #{@maintainer}\n"
    s += "  uploader = #{@uploader}\n"
    s += "  version = #{@version}\n"
    s += "  files =\n"
    @files.each { |file|
      s += "    #{file}\n"
    }
    return s
  end

  def listFiles
    # list all files involved in the upload, one basename per line
    return @files.inject("") { |result, e|
      result += e.gsub(/#{INCOMING}\//, "") + "\n"
    }
  end

  def handleFailure(e)
    # dumps error message on stdout, and possibly by email too
    subject = "Upload of #{@name} failed (#{e.class})"
    body = e.message
    body += "\n" + e.backtrace.join("\n") if not e.is_a?(UploadFailure)
    puts "#{subject}\n#{body}"
    email(@emailRecipientsFailure,
          subject,
          body) if @@doEmailFailure
  end

  def addToRepository
    destination = FAILED

    begin
      # first do a few policy checks
      if @distribution == "testing" and not @uploader =~ /(seb|rbscott)/i
        output = "#{@name} was intended for testing, but you can't upload there."
        raise UploadFailureByPolicy.new(output)
      end

      if @uploader =~ /root/i
        output = "#{@name} was built by root, not processing"
        raise UploadFailureByPolicy.new(output)
      end

      if @distribution == "daily-dogfood" and not @uploader =~ /buildbot/i
        output = "#{@name} was intended for daily-dogfood, but was not built by buildbot: not processing."
        raise UploadFailureByPolicy.new(output)
      end

      # then try to actually add the package
      output = `#{@command} 2>&1`
      puts output
      if $? != 0
        if output =~ /No section was given for '#{@name}', skipping/ then
          raise UploadFailureNoSection.new(output)
        elsif output =~ /No priority was given for '#{@name}', skipping/ then
          raise UploadFailureNoPriority.new(output)
        elsif output =~ /is already registered with other md5sum/ then
          raise UploadFailureAlreadyUploaded.new(output)
        else
          raise UploadFailure.new("Something went wrong when adding #{@name}\n\n" + output)
        end
      end

      destination = PROCESSED

      email(@emailRecipientsSuccess,
            "Upload of #{@name} succeeded",
            to_s) if @@doEmailSuccess

    rescue UploadFailureAlreadyUploaded
      puts "there: #{DISTRIBUTIONS}"
      DISTRIBUTIONS.each { |d|
        puts d
        @files.each { |f|
          if f =~ /.+\/(.+?)_.+\.deb$/ then
            packageName = $1
            puts "  #{packageName}"
            removeCommand = "reprepro -V -b #{REP} remove #{d} #{packageName}"
            puts "  #{removeCommand}"
            output = `#{removeCommand} 2>&1`
            puts output
          end
        }
      }
      retry
    rescue UploadFailureNoSection
      @command = @command.gsub!(/\-V/, "-V --section #{DEFAULT_SECTION}")
      retry
    rescue UploadFailureNoPriority
      @command = @command.gsub!(/\-V/, "-V --priority #{DEFAULT_PRIORITY}")
      retry
    rescue Exception => e
      handleFailure(e)
    ensure
      if @move
        @files.each { |file|
          File.rename(file, "#{destination}/#{File.basename(file)}")
        }
      end
    end
  end
end

class PackageUpload < DebianUpload
  def initialize(file, move)
    super(file, move)
    @name = File.basename(@file).gsub(/_.*/, "")
    @distribution = DEFAULT_DISTRIBUTION
    @component = DEFAULT_COMPONENT
    @command = "reprepro -V -b #{REP} --component #{@component} includedeb #{@distribution} #{@file}"
    @emailRecipientsSuccess = DEFAULT_MAIL_RECIPIENTS
    @emailRecipientsFailure = DEFAULT_MAIL_RECIPIENTS
  end
end

class ChangeFileUpload < DebianUpload
  def initialize(file, move)
    super(file, move)
    filesSection = false
    File.open(file).each { |line|
      line.strip!
      # FIXME: use a hash of /regex/ => :attribute
      case line
      when /^Source: / then
        @name = line.sub(/^Source: /, "")
      when /^Distribution: / then
        @distribution = line.sub(/^Distribution: /, "")
      when /^Maintainer: / then
        @maintainer = line.sub(/^Maintainer: /, "")
      when /^Changed-By: / then
        @uploader = line.sub(/^Changed-By: /, "")
      when /^Version: / then
        @version = line.sub(/^Version: /, "")
      when/^Files:/ then
        filesSection = true
        next
      when /^-----BEGIN PGP SIGNATURE-----/
        break # stop processing
      end

      if filesSection
        parts = line.split
        @files << INCOMING + "/" + parts[-1]
        @component = parts[2].split(/\//)[0] if not @component
      end
    }
    @command = "reprepro -Vb #{REP} include #{@distribution} #{@file}"
    @emailRecipientsSuccess = [ @uploader, @maintainer ].uniq
    @emailRecipientsFailure = @emailRecipientsSuccess + DEFAULT_MAIL_RECIPIENTS
    @emailRecipientsFailure.uniq!
  end
end

# if we operate on another directory, don't move files
if ARGV.length == 1
  INCOMING = ARGV[0]
  move = false
else
  move = true
end

if File.directory?(INCOMING)
  Dir["#{INCOMING}/*.changes"].each { |file|
    cfu = ChangeFileUpload.new(file, move)
    cfu.addToRepository
  }

  Dir["#{INCOMING}/*.deb"].each { |file|
    pu = PackageUpload.new(file, move)
    pu.addToRepository
  }
else
  if INCOMING =~ /\.changes$/
    cfu = ChangeFileUpload.new(INCOMING, move)
    cfu.addToRepository
  else
    pu = PackageUpload.new(INCOMING, move)
    pu.addToRepository
  end
end
