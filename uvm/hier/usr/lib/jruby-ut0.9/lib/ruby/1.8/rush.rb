require 'java'
require 'optparse'

class Rush
  attr_reader :uvm

  JAR_DIR = '@PREFIX@/usr/share/untangle/web/webstart'

  def initialize(options, scripts)
    @options = options
    @scripts = [scripts].flatten
  end

  def start
    login()

    if 0 < @scripts.length
      @scripts.each do |s|
        Kernel.load(s)
      end
    elsif @options[:command]
      Kernel.eval(@options[:command])
    elsif @options[:tty]
      require 'irb'
      IRB.start()
    else
      Kernel.eval(STDIN.readlines.join("\n"))
    end
  end

  private

  def login()
    begin
      @uvm = com.untangle.uvm.client.RemoteUvmContextFactory.factory.systemLogin(0)
    rescue
      puts "Could not log in: #{$!.message}"
      exit 1
    end
  end
end

if __FILE__ == $0
  options = {}

  OptionParser.new do |opts|
    opts.banner = "usage: rush [-c command] script.rb"

    opts.on("-t", "--tty", "is a tty") do |v|
      options[:tty] = v
    end

    opts.on("-c", "--command [COMMAND]", "command string") do |v|
      options[:command] = v
    end
  end.parse!


  RUSH = Rush.new(options, ARGV)

  RUSH.start()
end
