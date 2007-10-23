#!/usr/bin/ruby

require 'logger'
require 'fileutils'
require 'getoptlong'

# this is needed now !
export DEBIAN_FRONTEND=noninteractive

INST_OPTS = " -o DPkg::Options::=--force-confnew --yes --force-yes --fix-broken --purge "
UPGD_OPTS = " -o DPkg::Options::=--force-confnew --yes --force-yes --fix-broken --purge "
UPDT_OPTS = " --yes --force-yes --purge "
REMO_OPTS = " --yes --force-yes --fix-broken --purge "

PREFIX = "@PREFIX@"

STDERR.reopen('@UVM_LOG@/apt.log', 'a')

def printusage()
  puts <<STR
usage:
  mkg installed
  mkg available
  mkg install <mackage>
  mkg update
  mkg upgrade
  mkg remove
STR
end

class DevMkg
  @pkg_list_dir = "#{PREFIX}/tmp"

  def initialize(stamp)
    @stamp = stamp
  end

  def installed()
    each_pkg_list_line { |l| puts "#{l.split[1]} 10.0" if /^Package: / =~ l }
  end

  def available()
    each_pkg_list_line do |l|
      puts l unless /^XB-/ =~ l
      puts "Version: 10.0" if /^Package: / =~ l
    end
  end

  def install(name)
        aptlog "start $stamp"
        echo "END PACKAGE LIST" >>$APT_LOG
        aptlog "done $stamp"
  end

  def update()
  end

  def upgrade()
        aptlog "start $stamp"
        echo "END PACKAGE LIST" >>$APT_LOG
        aptlog "done $stamp"
  end

  def remove(pkg)
  end

  private

  def each_pkg_list_line()
    Dir.new(@pkg_list_dir).select { |d| /^pkg-list/ =~ d }.each do |p|
      File.open(p, 'r') do |io|
        io.each_line { |l| yield l }
      end
    end
  end
end

class RealMkg
  def initialize(stamp)
    @stamp = stamp
  end

  def installed()
    IO.popen("dpkg --get-selections 'untangle*'", 'r') do |input|
      IO.popen("xargs dpkg-query -W", 'w') do |output|
        input.each_line do |l|
          s = l.split
          output.puts(s[0]) if 'install' == s[1]
        end
      end
    end
  end

  def available()
    IO.popen("apt-cache search '^untangle-'") do |input|
      IO.popen("xargs apt-cache show --no-all-versions", 'w') do |output|
        input.each_line do |l|
          output.puts(l.split[0])
        end
      end
    end
  end

  def install(pkg)
    STDERR.puts "start #{@stamp}"
    STDERR.puts "downloading: \"#{pkg}\""
    pkg_list = get_pkg_list("apt-get install #{INST_OPTS} --print-uris #{pkg}")
    download(pkg, pkg_list)
    STDERR.puts "installing: \"#{pkg}\""

    IO.popen("apt-get install #{INST_OPTS} #{pkg}", 'r') do |input|
      input.each_line { |l| STDERR.puts l }
    end
    r = $?

    STDERR.puts "done #{@stamp}"

    r
  end

  def update()
    STDERR.puts "start #{@stamp}"
    STDERR.puts "apt-get update #{UPDT_OPTS}"
    IO.popen("apt-get update #{UPDT_OPTS}") do |io|
      io.each_line { |l| STDERR.puts l }
    end

    r = $?

    STDERR.puts "done #{@stamp}"
  end

  def upgrade()
    STDERR.puts "start #{@stamp}"
    STDERR.puts "apt-get upgrade #{UPGD_OPTS}"

    pkg_list = get_pkg_list("apt-get dist-upgrade #{UPGD_OPTS} --print-uris")
    download(pkg_list)
    r = cmd_to_stderr("apt-get dist-upgrade #{UPGD_OPTS}")

    STDERR.puts "done #{@stamp}"
  end

  def remove(pkg)
    STDERR.puts "start #{@stamp}"
    STDERR.puts "apt-get remove #{REMO_OPTS} \"$2\""
    r = cmd_to_stderr("apt-get remove #{REMO_OPTS}")
    cmd_to_stderr("apt-get clean")
    STDERR.puts "done #{@stamp}"
  end

  private

  def cmd_to_stderr(cmd)
    IO.popen(cmd) do |io|
      io.each_line { |l| STDERR.puts l }
    end
    $?
  end

  def get_tempdir()
    tmp = Tempfile.new('mkg')
    path = tmp.path
    tmp.delete
    Dir.mkdir(path)
    Dir.new(path)
  end

  def get_pkg_list(apt_cmd)
    pkg_list = []

    IO.popen(apt_cmd, 'r') do |io|
      io.each_line do |l|
        pkg_list << l if /^'http:\/\// =~ l
      end
    end

    pkg_list
  end

  def download(pkg, pkg_list)
    pkg_list.each { |p| STDERR.puts p }
    STDERR.puts "END PACKAGE LIST"

    tmp_dir = get_tempdir()
    begin
      pkg_list.each do |p|
        STDERR.puts "downloading: #{p}"

        cmd_to_stderr("wget --progress=dot -P #{tmp_dir.path} #{p}")

        if $?.success?
          tmp_dir.each do |f|
            FileUtils.mv("#{tmp_dir.path}/#{f}", "/var/cache/apt/archives")
          end
          STDERR.puts "DOWNLOAD SUCCEEDED: #{pkg}"
        else
          STDERR.puts "DOWNLOAD FAILED: #{pkg}"
        end

      end
    ensure
      Dir.delete(tmp_dir.path)
    end
  end

end

stamp = nil

opts = GetoptLong.new([ '-k', GetoptLong::REQUIRED_ARGUMENT ])
opts.each do |opt, arg|
  if '-k' == opt
    stamp = arg
  end
end

if ARGV.length < 1
  printusage()
  exit(1)
end

ret = 0

mkg = PREFIX.empty? ? RealMkg.new(stamp) : DevMkg(stamp)

case ARGV.shift
when 'installed'
  mkg.installed()
when 'available'
  mkg.available()
when 'install'
  if ARGV.empty?
    printusage()
  else
    mkg.install(ARGV.shift)
  end
when 'update'
  mkg.update()
when 'upgrade'
  mkg.upgrade()
when 'remove'
  if ARGV.empty?
    printusage()
  else
    mkg.remove(ARGV.shift)
  end
else

exit $RET
