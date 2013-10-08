require 'gettext/rgettext'
require 'yaml'
require "dbi"


# Database parser for gettext library for extracting msgid and create a po-file.
# usage: rgettext -rdbparser config1.yml config2.yml ... -o keys.pot
module DbParser
  module_function
  def target?(file)
    File.extname(file) == ".yml"  # This parser targets configuration yaml files only.
  end
  # Parse a file and return the array of [msgid, file1, file2, ...].
  # ary includes the result of other parsers. The new results should be
  # added to this ary.
  # And this method is required to keep a msgid as unique. Check the
  # msgid becomes unique before adding the msgid to ary. 
  #
  # Return value format is:
  #          [["msgid1", "file1:line1", "file2:line2",...],
  #           ["msgid2", "file3:line3",...]]
  # 
  def parse(file, ary)
    config = YAML.load(File.open(file))
    
#    CSV.open(file, "r") do |row|
#      row.each { |elem| ary << [elem.strip, file + ":csv data"] if !elem.nil?}
#    end    
    begin
      # connect to the PostgreSQL server
      dbh = connect_to_postgres(config['database'])
      
      # get data and return it formatted as specified above
      config['table'].each do |table|
        table['column'].each do |column|
           sth = dbh.execute("SELECT DISTINCT #{column} FROM #{table['name']}")
           sth.fetch do |row|
#             ary << [row[0].strip, "database data:#{table['name']}.#{column}"] if !row[0].nil?
             ary << [row[0], "database data: #{table['name']}.#{column}"]
           end
           sth.finish
        end
      end
      
    rescue DBI::DatabaseError => e
      puts "An error occurred"
      puts "Error code: #{e.err}"
      puts "Error message: #{e.errstr}"
    ensure
      # disconnect from server
      dbh.disconnect if dbh
    end
    
    ary
  end
  
  def connect_to_postgres(dbconfig)
    puts "\nConnecting to Postgres..."
    return DBI.connect("DBI:Pg:#{dbconfig['name']}", dbconfig['username'], dbconfig['password'])
  end  
end
# Add this parser to GetText::RGetText
GetText::RGetText.add_parser(DbParser)