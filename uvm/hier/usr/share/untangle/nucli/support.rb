#
# $HeadURL:$
# Copyright (c) 2003-2007 Untangle, Inc. 
#
# This program is free software; you can redistribute it and/or modify
# it under the terms of the GNU General Public License, version 2,
# as published by the Free Software Foundation.
#
# This program is distributed in the hope that it will be useful, but
# AS-IS and WITHOUT ANY WARRANTY; without even the implied warranty of
# MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE, TITLE, or
# NONINFRINGEMENT.  See the GNU General Public License for more details.
#
# You should have received a copy of the GNU General Public License
# along with this program; if not, write to the Free Software
# Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
#
require 'configelem'

class Support < UVMConfigElement

  def initialize
    @diag = Diag.new(DEFAULT_DIAG_LEVEL)
    @diag.if_level(3) { puts! "Initializing #{get_node_name()}..." }
    super
    @diag.if_level(3) { puts! "Done initializing #{get_node_name()}..." }
  end
  
  def get_node_name()
    "Support"
  end
  
  def get_help_text()
      return <<-HELP
- support allow access [true|false]
    -- Query the value of Support Access Restrictions "Allow" setting - change value to [true|false], if provided.
- support allow send [true|false]
    -- Query the value of Support Access Restrictions "Send" setting - change value to [true|false], if provided.
- support protocol weboverride [true|false]
    -- Query the value of Support: Manual Protocol Override: Web Override setting - change value to [true|false], if provided.
- support protocol longuris [true|false] [max-uri-length]
    -- Query the value of Support: Manual Protocol Override: Long Urls setting - change value to [true|false] and set [max-uri-length], if provided.
- support protocol longheaders [true|false] [max-header-length]
    -- Query the value of Support: Manual Protocol Override: Long Headers setting - change value to [true|false] and set [max-header-length], if provided.
- support protocol nonhttp [true|false]
    -- Query the value of Support: Manual Protocol Override: Non-HTTP Blocking setting - change value to [true|false], if provided.
- support protocol smtp [true|false]
    -- Query the value of Support: Manual Protocol Override: Mail Settings: SMTP - change value to [true|false], if provided.
- support protocol pop [true|false]
    -- Query the value of Support: Manual Protocol Override: Mail Settings: POP - change value to [true|false], if provided.
- support protocol imap [true|false]
    -- Query the value of Support: Manual Protocol Override: Mail Settings: IMAP - change value to [true|false], if provided.
- support protocol smtp timeout [seconds]
    -- Query the value of Support: Manual Protocol Override: Mail Settings: SMTP timeout - change value to [seconds], if provided.
- support protocol pop timeout [seconds]
    -- Query the value of Support: Manual Protocol Override: Mail Settings: POP timeout - change value to [seconds], if provided.
- support protocol imap timeout [seconds]
    -- Query the value of Support: Manual Protocol Override: Mail Settings: IMAP timeout - change value to [seconds], if provided.
    HELP
  end

  def cmd_allow_access(*args)
    allow_access(*args)
  end

  def cmd_allow_send(*args)
    allow_send(*args)
  end

  def cmd_protocol_weboverride(*args)
    protocol_weboverride(*args)
  end

  def cmd_protocol_longuris(*args)
    protocol_longuris(*args)
  end

  def cmd_protocol_longheaders(*args)
    protocol_longheaders(*args)
  end

  def cmd_protocol_nonhttp(*args)
    protocol_nonhttp(*args)
  end

  def cmd_protocol_smtp(*args)
    protocol_smtp(*args)
  end

  def cmd_protocol_pop(*args)
    protocol_pop(*args)
  end

  def cmd_protocol_imap(*args)
    protocol_imap(*args)
  end

  def cmd_protocol_smtp_timeout(*args)
    protocol_smtp_timeout(*args)
  end

  def cmd_protocol_pop_timeout(*args)
    protocol_pop_timeout(*args)
  end

  def cmd_protocol_imap_timeout(*args)
    protocol_imap_timeout(*args)
  end

  def cmd_protocol_ftp(*args)
    protocol_ftp(*args)
  end

  def allow_access(*args)
    settings = @@uvmRemoteContext.networkManager().getAccessSettings()
    if args.length > 0
      begin
        settings.setIsSupportEnabled(validate_bool(args[0], "access"))
        @@uvmRemoteContext.networkManager().setAccessSettings(settings)
      rescue Exception => ex
        msg = "Error: unable to set support access to '#{args[0]}'"
        @diag.if_level(3) { puts! msg; puts! ex; puts! ex.backtrace }          
        return msg + ": #{ex}"
      end
    end
    msg = "Support access is #{ settings.getIsSupportEnabled() ? 'allowed.' : 'disallowed.'}"
    @diag.if_level(3) { puts! msg }          
    return msg
  end

  def allow_send(*args)
    settings = @@uvmRemoteContext.networkManager().getMiscSettings()
    if args.length > 0
      begin
        settings.setIsExceptionReportingEnabled(validate_bool(args[0], "send"))
        @@uvmRemoteContext.networkManager().setMiscSettings(settings)
      rescue Exception => ex
        msg = "Error: unable to set #{BRAND} reporting to '#{args[0]}'"
        @diag.if_level(3) { puts! msg; puts! ex; puts! ex.backtrace }          
        return msg + ": #{ex}"
      end
    end
    msg = "Sending of #{BRAND} Server data to #{BRAND} is #{ settings.getIsExceptionReportingEnabled() ? 'allowed.' : 'disallowed.'}"
    @diag.if_level(3) { puts! msg }          
    return msg
  end

  def get_http_casing
    tids = @@uvmRemoteContext.nodeManager.nodeInstances("untangle-casing-http")
    if tids.empty?
      msg = "Error: there is no HTTP casing running on the effective UVM server!  This is unexpected; contact techical support."
      @diag.if_level(3) { puts! msg }
      return msg
    end
    node_ctx = @@uvmRemoteContext.nodeManager.nodeContext(tids[0])
    node_ctx.node()
  end
  
  def protocol_weboverride(*args)
    node = get_http_casing()
    if args.length > 0
      begin
        settings = node.getSettings()
        settings.setEnabled(validate_bool(args[0], "weboverride"))
        node.setSettings(settings)
      rescue Exception => ex
        msg = "Error: unable to set web override to '#{args[0]}'"
        @diag.if_level(3) { puts! msg; puts! ex; puts! ex.backtrace }          
        return msg + ": #{ex}"
      end
    end
    settings = node.getSettings()
    msg = "Processing of web traffic is #{ settings.isEnabled() ? 'enabled.' : 'disabled.'}"
    @diag.if_level(3) { puts! msg }          
    return msg
  end
  
  def protocol_longuris(*args)
    node = get_http_casing()
    if args.length > 0
      begin
        settings = node.getSettings()
        settings.setBlockLongUris(!validate_bool(args[0], "longuris"))
        if args.length > 1
          begin
            max_uri = Integer(args[1])
            settings.setMaxUriLength(max_uri)
          rescue Exception => ex
            msg = "Error: invalid maximum URI length '#{args[1]}'"
            @diag.if_level(3) { puts! msg; puts! ex; puts! ex.backtrace }          
            return msg + ": #{ex}"
          end
        end
        node.setSettings(settings)
      rescue Exception => ex
        msg = "Error: unable to set long URIs to '#{args[0]}'"
        @diag.if_level(3) { puts! msg; puts! ex; puts! ex.backtrace }          
        return msg + ": #{ex}"
      end
    end
    settings = node.getSettings()
    msg = "Processing of long URIs is #{ settings.getBlockLongUris() ? 'diabled' : 'enabled'} (#{settings.getMaxUriLength()})" 
    @diag.if_level(3) { puts! msg }          
    return msg
  end

  def protocol_longheaders(*args)
    node = get_http_casing()
    if args.length > 0
      begin
        settings = node.getSettings()
        settings.setBlockLongHeaders(!validate_bool(args[0], "longheaders"))
        if args.length > 1
          begin
            max_hdr = Integer(args[1])
            settings.setMaxHeaderLength(max_hdr)
          rescue Exception => ex
            msg = "Error: invalid maximum header length '#{args[1]}'"
            @diag.if_level(3) { puts! msg; puts! ex; puts! ex.backtrace }          
            return msg + ": #{ex}"
          end
        end
        node.setSettings(settings)
      rescue Exception => ex
        msg = "Error: unable to set long headers to '#{args[0]}'"
        @diag.if_level(3) { puts! msg; puts! ex; puts! ex.backtrace }          
        return msg + ": #{ex}"
      end
    end
    settings = node.getSettings()
    msg = "Processing of long headers is #{ settings.getBlockLongHeaders() ? 'diabled' : 'enabled'} (#{settings.getMaxHeaderLength()})" 
    @diag.if_level(3) { puts! msg }          
    return msg
  end
  
  def protocol_nonhttp(*args)
    node = get_http_casing()
    if args.length > 0
      begin
        settings = node.getSettings()
        settings.setNonHttpBlocked(!validate_bool(args[0], "nonhttp"))
        node.setSettings(settings)
      rescue Exception => ex
        msg = "Error: unable to set non-http blocking to '#{args[0]}'"
        @diag.if_level(3) { puts! msg; puts! ex; puts! ex.backtrace }          
        return msg + ": #{ex}"
      end
    end
    settings = node.getSettings()
    msg = "Processing of non-http traffic is #{ settings.isNonHttpBlocked() ? 'diabled.' : 'enabled.'}" 
    @diag.if_level(3) { puts! msg }          
    return msg
  end

  def get_mail_casing
    tids = @@uvmRemoteContext.nodeManager.nodeInstances("untangle-casing-mail")
    if tids.empty?
      msg = "Error: there is no mail casing running on the effective UVM server! This is unexpected; contact techical support."
      @diag.if_level(3) { puts! msg }
      return msg
    end
    node_ctx = @@uvmRemoteContext.nodeManager.nodeContext(tids[0])
    node_ctx.node()
  end
  
  def protocol_smtp(*args)
    node = get_mail_casing()
    if args.length > 0
      begin
        settings = node.getSettings()
        settings.setSmtpEnabled(validate_bool(args[0], "smtp"))
        node.setSettings(settings)
      rescue Exception => ex
        msg = "Error: unable to set SMPT processing to '#{args[0]}'"
        @diag.if_level(3) { puts! msg; puts! ex; puts! ex.backtrace }          
        return msg + ": #{ex}"
      end
    end
    settings = node.getSettings()
    msg = "Processing of SMTP email is #{ settings.isSmtpEnabled() ? 'enabled.' : 'disabled.'}" 
    @diag.if_level(3) { puts! msg }          
    return msg
  end
  
  def protocol_pop(*args)
    node = get_mail_casing()
    if args.length > 0
      begin
        settings = node.getSettings()
        settings.setPopEnabled(validate_bool(args[0], "pop"))
        node.setSettings(settings)
      rescue Exception => ex
        msg = "Error: unable to set POP processing to '#{args[0]}'"
        @diag.if_level(3) { puts! msg; puts! ex; puts! ex.backtrace }          
        return msg + ": #{ex}"
      end
    end
    settings = node.getSettings()
    msg = "Processing of POP email is #{ settings.isPopEnabled() ? 'enabled.' : 'disabled.'}" 
    @diag.if_level(3) { puts! msg }          
    return msg
  end

  def protocol_imap(*args)
    node = get_mail_casing()
    if args.length > 0
      begin
        settings = node.getSettings()
        settings.setImapEnabled(validate_bool(args[0], "imap"))
        node.setSettings(settings)
      rescue Exception => ex
        msg = "Error: unable to set IMAP processing to '#{args[0]}'"
        @diag.if_level(3) { puts! msg; puts! ex; puts! ex.backtrace }          
        return msg + ": #{ex}"
      end
    end
    settings = node.getSettings()
    msg = "Processing of IMAP email is #{ settings.isPopEnabled() ? 'enabled.' : 'disabled.'}" 
    @diag.if_level(3) { puts! msg }          
    return msg
  end

  def protocol_smtp_timeout(*args)
    node = get_mail_casing()
    if args.length > 0
      begin
        settings = node.getSettings()
        settings.setSmtpTimeout(Integer(args[0])*1000)
        node.setSettings(settings)
      rescue Exception => ex
        msg = "Error: unable to set SMPT processing to '#{args[0]}'"
        @diag.if_level(3) { puts! msg; puts! ex; puts! ex.backtrace }          
        return msg + ": #{ex}"
      end
    end
    settings = node.getSettings()
    msg = "SMTP timeout is #{settings.getSmtpTimeout()/1000} seconds." 
    @diag.if_level(3) { puts! msg }          
    return msg
  end

  def protocol_pop_timeout(*args)
    node = get_mail_casing()
    if args.length > 0
      begin
        settings = node.getSettings()
        settings.setPopTimeout(Integer(args[0])*1000)
        node.setSettings(settings)
      rescue Exception => ex
        msg = "Error: unable to set POP processing to '#{args[0]}'"
        @diag.if_level(3) { puts! msg; puts! ex; puts! ex.backtrace }          
        return msg + ": #{ex}"
      end
    end
    settings = node.getSettings()
    msg = "POP timeout is #{settings.getPopTimeout()/1000} seconds." 
    @diag.if_level(3) { puts! msg }          
    return msg
  end

  def protocol_imap_timeout(*args)
    node = get_mail_casing()
    if args.length > 0
      begin
        settings = node.getSettings()
        settings.setImapTimeout(Integer(args[0])*1000)
        node.setSettings(settings)
      rescue Exception => ex
        msg = "Error: unable to set IMAP processing to '#{args[0]}'"
        @diag.if_level(3) { puts! msg; puts! ex; puts! ex.backtrace }          
        return msg + ": #{ex}"
      end
    end
    settings = node.getSettings()
    msg = "IMAP timeout is #{settings.getPopTimeout()/1000} seconds." 
    @diag.if_level(3) { puts! msg }          
    return msg
  end

  def get_ftp_casing
    tids = @@uvmRemoteContext.nodeManager.nodeInstances("untangle-casing-ftp")
    if tids.empty?
      msg = "Error: there is no FTP casing running on the effective UVM server! This is unexpected; contact techical support."
      @diag.if_level(3) { puts! msg }
      return msg
    end
    node_ctx = @@uvmRemoteContext.nodeManager.nodeContext(tids[0])
    node_ctx.node()
  end
  
  def protocol_ftp(*args)
    node = get_ftp_casing()
    if args.length > 0
      begin
        settings = node.getSettings()
        settings.setEnabled(validate_bool(args[0], "ftp"))
        node.setSettings(settings)
      rescue Exception => ex
        msg = "Error: unable to set FTP processing to '#{args[0]}'"
        @diag.if_level(3) { puts! msg; puts! ex; puts! ex.backtrace }          
        return msg + ": #{ex}"
      end
    end
    settings = node.getSettings()
    msg = "Processing of FTP traffic is #{ settings.isEnabled() ? 'enabled.' : 'disabled.'}" 
    @diag.if_level(3) { puts! msg }          
    return msg
  end

end

