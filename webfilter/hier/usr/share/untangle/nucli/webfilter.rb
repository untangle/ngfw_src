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
require 'filternode'

class Webfilter < UVMFilterNode
    
    UVM_NODE_NAME = "untangle-node-webfilter"
    WEBFILTER_MIB_ROOT = UVM_FILTERNODE_MIB_ROOT + ".1"

    def initialize
        @diag = Diag.new(DEFAULT_DIAG_LEVEL)
	@diag.if_level(3) { puts! "Initializing #{get_node_name()}..." }
        super
	@diag.if_level(3) { puts! "Done initializing #{get_node_name()}..." }
    end

    #
    # Required UVMFilterNode methods.
    #
    def get_uvm_node_name()
        UVM_NODE_NAME
    end
    
    def get_node_name()
        "Web Filter"
    end

    def get_mib_root()
        WEBFILTER_MIB_ROOT
    end

    def get_help_text
        return <<-HELP
- webfilter -- enumerate all web filters running on effective #{BRAND} server.
- webfilter <TID> list blocklist [item-type:cats|urls|mimetypes|filetypes]
    -- Display blocklist of item-type for webfilter TID
- webfilter <TID> add blocklist [item-type:url|mimetype|filetype] item <block:true|false> <log:true|false> <description>
    -- Add item to blocklist by type (or update) with specified block and log settings.
- webfilter <TID> remove blocklist [item-type:url|mime|file] item
    -- Remove item from blocklist by type (or update) with specified block and log settings.
- webfilter <TID> list passlist [item-type:urls|clients]
    -- Display passlist items
- webfilter <TID> add passlist [item-type:url|client] item [pass:true|false] <description>
    -- Add item to passlist with specified settings.
- webfilter <TID> remove passlist [item-type:url|client] item
    -- Remove item from passlist with specified settings.
- webfilter <TID> stats
    -- Display webfilter TID statistics in human readable format
- webfilter snmp
    -- Display webfilter TID statistics in snmp compliant format (getnext)
        HELP
    end

    def cmd_(tid, *args)
        return list_filternodes()
    end

    def cmd_list_blocklist(tid, *args)
        return ERROR_INCOMPLETE_COMMAND
    end
    
    def cmd_list_blocklist_urls(tid, *args)
        return block_list_get_urls(tid)
    end

    def cmd_list_blocklist_cats(tid, *args)
        return block_list_get_categories(tid)
    end

    def cmd_list_blocklist_categories(tid, *args)
        return block_list_get_categories(tid)
    end

    def cmd_list_blocklist_mimetypes(tid, *args)
        return block_list_get_mime_types(tid)
    end

    def cmd_list_blocklist_filetypes(tid, *args)
        return block_list_get_file_types(tid)
    end

    def cmd_add_blocklist_url(tid, *args)
        return block_list_add_url(tid, args[0], args[1], args[2], args[3])
    end

    def cmd_add_blocklist_mimetype(tid, *args)
        return block_list_add_mime_type(tid, args[0], args[1], args[2])
    end
            
    def cmd_add_blocklist_filetype(tid, *args)
        return block_list_add_file_type(tid, args[0], args[1], args[2])
    end

    def cmd_update_blocklist_category(tid, *args)
        return block_list_update_category(tid, args[0], args[1], args[2])
    end
    
    def cmd_block_list_remove_url(tid, *args)
        return block_list_remove_url(tid, args[0])
    end
    
    def cmd_block_list_remove_mimetype(tid, *args)
        return block_list_remove_mime_type(tid, args[0])
    end
    
    def cmd_block_list_remove_filetype(tid, *args)
        return block_list_remove_file_type(tid, args[0])
    end

    def block_list_get_urls(tid)
        node_ctx = @@uvmRemoteContext.nodeManager.nodeContext(tid)
        node = node_ctx.node()
        settings =  node.getSettings()
        blocked_urls_list = settings.getBlockedUrls()
        blocked_urls = "URL,block,log\n"
        blocked_urls_list.each { |url|
            blocked = (url.getString() + "," + url.isLive().to_s + "," + url.getLog().to_s + "," + url.getDescription() + "\n")
            blocked_urls << blocked
        } if blocked_urls_list
        @diag.if_level(3) { puts! blocked_urls }
        return blocked_urls
    end

    def block_list_get_categories(tid)
        node_ctx = @@uvmRemoteContext.nodeManager.nodeContext(tid)
        node = node_ctx.node()
        settings =  node.getSettings()
        blocked_cats_list = settings.getBlacklistCategories()
        blocked_cats = "Category, description, block/log\n"
        blocked_cats_list.each { |cat|
            blocked = ""
            blocked << (cat.getDisplayName() + "," + cat.getDescription())
            # ***TODO: get these block combos to match the GUI
            if cat.getBlockDomains() && cat.getBlockUrls() && cat.getBlockExpressions() && !cat.getLogOnly()
                blocked << ",block and log"
            elsif !cat.getBlockDomains() && !cat.getBlockUrls() && !cat.getBlockExpressions() && !cat.getLogOnly()
                blocked << ",pass"
            elsif cat.getBlockDomains() && cat.getBlockUrls() && cat.getBlockExpressions() && cat.getLogOnly()
                blocked << ",pass and log"
            else
                blocked << ",action unknown - correct via #{BRAND} admin. GUI."
            end
            @diag.if_level(3) { blocked << (" (" + cat.getBlockDomains().to_s + "," + cat.getBlockUrls().to_s + "," + cat.getBlockExpressions().to_s + "," + cat.getLogOnly().to_s + ")") }
            blocked << "\n"
            blocked_cats << blocked
            @diag.if_level(3) { puts! blocked }
        } if blocked_cats_list
        return blocked_cats
    end
    
    def block_list_get_mime_types(tid)
        node_ctx = @@uvmRemoteContext.nodeManager.nodeContext(tid)
        node = node_ctx.node()
        settings =  node.getSettings()
        blocked_mime_types_list = settings.getBlockedMimeTypes()
        @diag.if_level(3) { puts "# blocked mime types = #{blocked_mime_types_list.length}" if blocked_mime_types_list }
        blocked_mime_types = "MIME type,block,name,category,description,log\n"
        blocked_mime_types_list.each { |mime_type_rule|
            mime_type = mime_type_rule.getMimeType                
            blocked = mime_type.getType
            blocked << ("," + mime_type_rule.isLive.to_s)
            blocked << ("," + mime_type_rule.getName)
            blocked << ("," + mime_type_rule.getCategory)
            blocked << ("," + mime_type_rule.getDescription)
            blocked << ("," + (mime_type_rule.getLog ? "logged" : "not logged"))
            blocked << "\n"
            blocked_mime_types << blocked
            @diag.if_level(3) { puts! blocked }
        } if blocked_mime_types_list
        return blocked_mime_types
    end

    def block_list_get_file_types(tid)
        node_ctx = @@uvmRemoteContext.nodeManager.nodeContext(tid)
        node = node_ctx.node()
        settings =  node.getSettings()
        blocked_files_list = settings.getBlockedExtensions()
        blocked_files = "File ext., block, description"
        blocked_files_list.each { |extension|
            blocked_file = ""
            blocked_file << extension.getString() + ","
            blocked_file << extension.isLive().to_s + ","
            blocked_file << extension.getDescription + "\n"
            blocked_files << blocked_file
        } if blocked_files_list
        @diag.if_level(3) { puts! blocked_files }
        return blocked_files
    end

    def block_list_add_url(tid, url, block, log, desc)
        begin
            return ERROR_INCOMPLETE_COMMAND if url.nil?
            node_ctx = @@uvmRemoteContext.nodeManager.nodeContext(tid)
            node = node_ctx.node()
            settings =  node.getSettings()
            blockedUrlsList = settings.getBlockedUrls()   
            url = url.gsub(/^www./, '')
            @diag.if_level(2) { puts! "Attempting to add #{url} to blocked list." }
            newUrlToBlock = com.untangle.uvm.node.StringRule.new(url)
            newUrlToBlock.setLive(block == "false" ? false : true)
            newUrlToBlock.setLog(log == "true")
            newUrlToBlock.setDescription(desc) if desc
            rule_to_update = -1
            blockedUrlsList.each_with_index { |blocked_url, i|
                rule_to_update = i if blocked_url.getString() == newUrlToBlock.getString()
            }
            if rule_to_update == -1
                blockedUrlsList.add(newUrlToBlock)
            else
                blockedUrlsList[rule_to_update] = newUrlToBlock
            end
            settings.setBlockedUrls(blockedUrlsList)
            node.setSettings(settings)
            msg = "URL '#{url}' added to Block list."
            @diag.if_level(3) { puts! msg }
            return msg
        rescue Exception => ex
            @diag.if_level(3) { p ex }
            return "Adding URL to block list failed:\n" + ex
        end
    end
    
    def block_list_remove_url(tid, url)
        begin
            return ERROR_INCOMPLETE_COMMAND if url.nil?
            node_ctx = @@uvmRemoteContext.nodeManager.nodeContext(tid)
            node = node_ctx.node()
            settings =  node.getSettings()
            blockedUrlsList = settings.getBlockedUrls()   
            url = url.gsub(/^www./, '')
            @diag.if_level(2) { puts! "Attempting to remove #{url} from blocked list." }

            rule_to_remove = blockedUrlsList.detect { |blocked_url|
                blocked_url.getString() == url
            }
            if rule_to_remove
                blockedUrlsList.remove(rule_to_remove)
                settings.setBlockedUrls(blockedUrlsList)
                node.setSettings(settings)
                msg = "URL '#{url}' removed from block list."
            else
                msg = "Error: can't remove - URL not found."
            end

            @diag.if_level(3) { puts! msg }
            return msg
        rescue Exception => ex
            @diag.if_level(3) { p ex }
            return "Adding URL to block list failed:\n" + ex
        end
    end
    
    def block_list_add_mime_type(tid, mime_type, block=nil, name=nil)
        begin
            return ERROR_INCOMPLETE_COMMAND if mime_type.nil? || mime_type == ""
            node_ctx = @@uvmRemoteContext.nodeManager.nodeContext(tid)
            node = node_ctx.node()
            settings =  node.getSettings()
            blockedMimesList = settings.getBlockedMimeTypes()
            @diag.if_level(3) { puts! "Attempting to add #{mime_type} to Block list." }
            mimeType = com.untangle.uvm.node.MimeType.new(mime_type)
            block = (block.nil? || (block != "true")) ? false : true
            name ||= "[no name]" 
            mimeTypeRule = com.untangle.uvm.node.MimeTypeRule.new(mimeType, name, "[no category]", "[no description]", block)
            rule_to_update = -1
            blockedMimesList.each_with_index { |blocked_mime,i|
                rule_to_update = i if blocked_mime.getMimeType().getType() == mime_type
            }
            if rule_to_update == -1
                blockedMimesList.add(mimeTypeRule)
            else
                blockedMimesList[rule_to_update] = mimeTypeRule
            end
            settings.setBlockedMimeTypes(blockedMimesList)
            node.setSettings(settings)
            msg = "Mime type '#{mime_type}' added to Block list."
            @diag.if_level(2) { puts! msg }
            return msg
        rescue Exception => ex
            @diag.if_level(3) { p ex }
            return "Adding mime type to block list failed:\n" + ex
        end
    end

    def block_list_remove_mime_type(tid, mime_type)
        begin
            return ERROR_INCOMPLETE_COMMAND if mime_type.nil? || mime_type == ""
            node_ctx = @@uvmRemoteContext.nodeManager.nodeContext(tid)
            node = node_ctx.node()
            settings =  node.getSettings()
            blockedMimesList = settings.getBlockedMimeTypes()
            return "Error: there are no blocked mime types defined." if blockedMimesList.nil? || blockedMimesList.length == 0
            
            @diag.if_level(3) { puts! "Attempting to remove #{mime_type} from block list." }
            rule_to_remove = blockedMimesList.detect{ |blocked_mime|
                blocked_mime.getMimeType().getType() == mime_type
            }
            if rule_to_remove
                blockedMimesList.remove(rule_to_remove)
                settings.setBlockedMimeTypes(blockedMimesList)
                node.setSettings(settings)
                msg = "MIME type '#{mime_type}' removed from block list."
            else
                msg = "Error: can't remove - MIME type not found."
            end
            
            @diag.if_level(2) { puts! msg }
            return msg
        rescue Exception => ex
            msg = "Remove of MIME type from block list failed:\n" + ex
            @diag.if_level(3) { puts! msg ; p ex }
            return msg
        end
    end

    def block_list_add_file_type(tid, file_ext, block="true", description=nil)
        begin
            return ERROR_INCOMPLETE_COMMAND if file_ext.nil? || file_ext==""
            node_ctx = @@uvmRemoteContext.nodeManager.nodeContext(tid)
            node = node_ctx.node()
            settings =  node.getSettings()
            blockedExtensionsList = settings.getBlockedExtensions()   
            @diag.if_level(2) { puts! "Attempting to add #{file_ext} to Block list." }
            block = (block.nil? || (block != "true")) ? false : true
            stringRule = com.untangle.uvm.node.StringRule.new(file_ext, "[no name]", "[no category]", description, block)
            rule_to_update = -1
            blockedExtensionsList.each_with_index { |blocked_ext,i|
                blocked_ext.getString() == stringRule.getString()
            }
            if rule_to_update == -1
                blockedExtensionsList.add(stringRule)
            else
                blockedExtensionsList[rule_to_update] = stringRule
            end
            settings.setBlockedExtensions(blockedExtensionsList)
            node.setSettings(settings)
            msg = "File extension '#{file_ext}' added to Block list."
            @diag.if_level(3) { puts! msg }
            return msg
        rescue Exception => ex
            @diag.if_level(3) { p ex }
            return "Adding extension '#{file_ext}' to block list failed:\n" + ex
        end
    end

    def block_list_remove_file_type(tid, file_ext)
        begin
            return ERROR_INCOMPLETE_COMMAND if file_ext.nil? || file_ext==""
            node_ctx = @@uvmRemoteContext.nodeManager.nodeContext(tid)
            node = node_ctx.node()
            settings = node.getSettings()
            blockedExtensionsList = settings.getBlockedExtensions()   
            @diag.if_level(2) { puts! "Attempting to remove #{file_ext} from block list." }

            rule_to_remove = blockedExtensionsList.detect{ |blocked_file|
                blocked_file.getString() == file_ext
            }
            if rule_to_remove
                blockedExtensionsList.remove(rule_to_remove)
                settings.setBlockedExtensions(blockedExtensionsList)
                node.setSettings(settings)
                msg = "File type '#{file_ext}' removed from block list."
            else
                msg = "Error: can't remove - File type not found."
            end
            
            @diag.if_level(2) { puts! msg }
            return msg

        rescue Exception => ex
            @diag.if_level(3) { p ex }
            return "Remove file type '#{file_ext}' from block list failed:\n" + ex
        end
    end

    def block_list_update_category(tid, cat_to_block, block="true", log="true")
        return ERROR_INCOMPLETE_COMMAND if cat_to_block.nil? || cat_to_block==""
        node_ctx = @@uvmRemoteContext.nodeManager.nodeContext(tid)
        node = node_ctx.node()
        settings = node.getSettings()
        blocked_cats_list = settings.getBlacklistCategories()
        cat_to_block = cat_to_block.downcase
        cat_idx = 0
        blocked_cat = blocked_cats_list.find { |cat|
            found = cat.getDisplayName().downcase == cat_to_block
            cat_idx += 1 unless found
            found
        }
        if blocked_cat
            block = (block == "true")
            pass = !block
            log = (log == "true")
            new_cat = blocked_cats_list[cat_idx]
            if block
                new_cat.setBlockDomains(true)
                new_cat.setBlockUrls(true)
                new_cat.setBlockExpressions(true)
                new_cat.setLogOnly(false)
            elsif pass && log
                new_cat.setBlockDomains(false)
                new_cat.setBlockUrls(false)
                new_cat.setBlockExpressions(false)
                new_cat.setLogOnly(true)
            elsif pass
                new_cat.setBlockDomains(false)
                new_cat.setBlockUrls(false)
                new_cat.setBlockExpressions(false)
                new_cat.setLogOnly(false)
            end
            blocked_cats_list[cat_idx] = new_cat
            settings.setBlacklistCategories(blocked_cats_list)
            node.setSettings(settings)
            msg = "Category '#{cat_to_block}' action updated."
            @diag.if_level(2) { puts! msg }
            return msg
        end
    end
    
    #
    #   Pass list related methods
    #
    def cmd_passlist(tid, *args)
        return ERROR_INCOMPLETE_COMMAND
    end
    
    def cmd_list_passlist(tid, *args)
        return ERROR_INCOMPLETE_COMMAND
    end

    def cmd_list_passlist_urls(tid, *args)
        return list_passlist_urls(tid)
    end
    
    def cmd_list_passlist_clients(tid, *args)
        return list_passlist_clients(tid)
    end
    
    def cmd_add_passlist(tid, *args)
        return ERROR_INCOMPLETE_COMMAND
    end
    
    def cmd_add_passlist_url(tid, *args)
        return add_passlist_url(tid, args[0], args[1], args[2])
    end
    
    def cmd_add_passlist_client(tid, *args)
        return add_passlist_client(tid, args[0], args[1], args[2])
    end
    
    def cmd_remove_passlist(tid, *args)
        return ERROR_INCOMPLETE_COMMAND
    end
    
    def cmd_remove_passlist_url(tid, *args)
        return remove_passlist_url(tid, args[0])
    end
    
    def cmd_remove_passlist_client(tid, *args)
        return remove_passlist_client(tid, args[0])
    end

    def list_passlist_urls(tid)
        node_ctx = @@uvmRemoteContext.nodeManager.nodeContext(tid)
        node = node_ctx.node()
        settings =  node.getSettings()
        passed_urls_list = settings.getPassedUrls()
        passed_urls = "URL,pass,description\n"
        passed_urls_list.each { |url|
            passed = ""
            passed << (url.getString() + "," + url.isLive().to_s + "," + url.getDescription() + "\n")
            passed_urls << passed
            @diag.if_level(3) { puts! passed }
        } if passed_urls_list
        return passed_urls
    end

    def list_passlist_clients(tid)
        node_ctx = @@uvmRemoteContext.nodeManager.nodeContext(tid)
        node = node_ctx.node()
        settings =  node.getSettings()
        passed_clients_list = settings.getPassedClients()
        passed_clients = "Client IP,pass,description\n"
        passed_clients_list.each { |client|
            passed = ""
            passed << (client.getIpMaddr().getAddr() + "," + client.isLive().to_s + "," + client.getDescription() + "\n")
            passed_clients << passed
            @diag.if_level(3) { puts! passed }
        } if passed_clients_list
        return passed_clients
    end
    
    def add_passlist_url(tid, url, block="true", desc=nil)
        begin
            return ERROR_INCOMPLETE_COMMAND if url.nil? || url==""
            node_ctx = @@uvmRemoteContext.nodeManager.nodeContext(tid)
            node = node_ctx.node()
            settings =  node.getSettings()
            passedUrlsList = settings.getPassedUrls()   
            url = url.gsub(/^www./, '')
            @diag.if_level(2) { puts! "Attempting to add #{url} to passed list." }
            newUrlToPass = com.untangle.uvm.node.StringRule.new(url)
            newUrlToPass.setLive(block == "true")
            newUrlToPass.setDescription(desc) if desc
            rule_to_update = -1
            passedUrlsList.each_with_index { |passed_url,i|
                rule_to_update = i if passed_url.getString() == newUrlToPass.getString()
            }
            if rule_to_update == -1
                passedUrlsList.add(newUrlToPass)
            else
                passedUrlsList[rule_to_update] = newUrlToPass
            end
            settings.setPassedUrls(passedUrlsList)
            node.setSettings(settings)
            msg = (rule_to_update == -1) ? "URL '#{url}' added to Pass List." : "Pass list URL '#{url}' updated.'"
            @diag.if_level(3) { puts! msg }
            return msg
        rescue Exception => ex
            msg = "Adding URL to block list failed: " + ex
            @diag.if_level(3) { puts! msg ; p ex }
            return msg
        end
    end

    def remove_passlist_url(tid, url)
        begin
            return ERROR_INCOMPLETE_COMMAND if url.nil? || url==""
            node_ctx = @@uvmRemoteContext.nodeManager.nodeContext(tid)
            node = node_ctx.node()
            settings =  node.getSettings()
            passedUrlsList = settings.getPassedUrls()   
            url = url.gsub(/^www./, '')
            @diag.if_level(2) { puts! "Attempting to add #{url} to passed list." }

            rule_to_remove = passedUrlsList.detect { |passed_url|
                passed_url.getString() == url
            }
            if rule_to_remove
                passedUrlsList.remove(rule_to_remove)
                settings.setPassedUrls(passedUrlsList)
                node.setSettings(settings)
                msg = "URL '#{url}' removed from pass List."
            else
                msg = "Error: can't remove - URL not found."
            end
            @diag.if_level(3) { puts! msg }
            return msg
        rescue Exception => ex
            @diag.if_level(3) { p ex }
            return "Adding URL to block list failed:\n" + ex
        end
    end

    def add_passlist_client(tid, client, block="true", desc=nil)
        begin
            return ERROR_INCOMPLETE_COMMAND if client.nil? || client==""
            node_ctx = @@uvmRemoteContext.nodeManager.nodeContext(tid)
            node = node_ctx.node()
            settings =  node.getSettings()
            passedClientsList = settings.getPassedClients()
            @diag.if_level(2) { puts! "Attempting to add #{client} to passed list." }
            newClientIpMaddr = com.untangle.uvm.node.IPMaddr.new(client)
            newClientToPass = com.untangle.uvm.node.IPMaddrRule.new()
            newClientToPass.setIpMaddr(newClientIpMaddr)
            newClientToPass.setLive(block == "true")
            newClientToPass.setDescription(desc) if desc
            rule_to_update = -1
            passedClientsList.each_with_index { |passed_client,i|
                rule_to_update = i if passed_client.getIpMaddr().getAddr() == newClientToPass.getIpMaddr().getAddr()
            }
            if rule_to_update == -1
                passedClientsList.add(newClientToPass)
            else
                passedClientsList[rule_to_update] = newClientToPass
            end
            settings.setPassedClients(passedClientsList)
            node.setSettings(settings)
            msg = "Client '#{client}' added to Pass List."
            @diag.if_level(3) { puts! msg }
            return msg
        rescue Exception => ex
            @diag.if_level(3) { p ex }
            return "Adding client to pass list failed:\n" + ex
        end
    end

    def remove_passlist_client(tid, client, block="true", desc=nil)
        begin
            return ERROR_INCOMPLETE_COMMAND if client.nil? || client==""
            node_ctx = @@uvmRemoteContext.nodeManager.nodeContext(tid)
            node = node_ctx.node()
            settings =  node.getSettings()
            passedClientsList = settings.getPassedClients()   
            @diag.if_level(2) { puts! "Attempting to remove #{client} from pass list." }
            
            rule_to_remove = passedClientsList.detect { |passed_client|
                passed_client.getIpMaddr().getAddr() == client
            }
            if rule_to_remove
                passedClientsList.remove(rule_to_remove)
                settings.setPassedClients(passedClientsList)
                node.setSettings(settings)
                msg = "Client '#{client}' removed from pass List."
            else
                msg = "Error: can't remove - client address not found."
            end
            @diag.if_level(3) { puts! msg }
            return msg
        rescue Exception => ex
            @diag.if_level(3) { p ex }
            return "Adding URL to block list failed:\n" + ex
        end
    end

end # WebFilter
