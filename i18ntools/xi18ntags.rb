#!/usr/bin/env ruby
#
# @author: ken@untangle.com
#
# The only weakness to this script is that an i18n tag body cannot
# contain a '<' character and the string passed to an i18n function
# cannot contain any kind of quote.  If either of these become a
# requirement then this script will have to be revised or replaced
# with one that processed the file as XML.
#

if (ARGV.length < 1)
    puts "Usage: ruby xi18ntags.rb <file-to-extract-text-from>.jsp[x] [dup]"
    exit 1
end

File.open(ARGV[0]) do |f|
    check_dup = (ARGV.length >= 2 && ARGV[1] == "dup")
    line_num = 1
    h = Hash.new if check_dup
    while line = f.gets
        # process i18n tags, e.g., <uvm:i18n>Help</uvm:i18n>
        while line =~ /(<uvm:i18n([^<]*)>)([^<]*)(<\/uvm:i18n>)/
            if check_dup then
                if h.has_key?($3) then
                    h[$3]=h[$3]+"#: #{ARGV[0]}:#{line_num}\n"  
                else
                    h[$3] = "#: #{ARGV[0]}:#{line_num}\n" 
                end
            else
                puts "#: #{ARGV[0]}:#{line_num}\nmsgid \"#{$3}\"\nmsgstr \"\"\n\n"
            end
            line.sub!(/(<uvm:i18n([^<]*)>)([^<]*)(<\/uvm:i18n>)/, '')
        end
        # process i18n embedded functions, e.g., ${uvm:i18n(pageContext,'Cancel')}
        while line =~ /(\$\{uvm:i18n[^'"]*['"])([^'"]*)(['"]\)\})/
            if check_dup then
                if h.has_key?($2) then
                    h[$2]=h[$3]+"#: #{ARGV[0]}:#{line_num}\n"  
                else
                    h[$2] = "#: #{ARGV[0]}:#{line_num}\n" 
                end
            else
                puts "#: #{ARGV[0]}:#{line_num}\nmsgid \"#{$2}\"\nmsgstr \"\"\n\n"
            end
            line.sub!(/(\$\{uvm:i18n[^'"]*['"])([^'"]*)(['"]\)\})/, '')
        end
        line_num += 1;
    end    
    h.each {|key, value| puts "#{value}msgid \"#{key}\"\nmsgstr \"\"\n\n" } if check_dup
end
