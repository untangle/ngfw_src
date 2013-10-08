require 'gettext/rgettext'
require 'csv'

# CSV parser for gettext library for extracting msgid and create a po-file.
# usage: rgettext -rcsvparser file1.csv file2.cvs ... -o keys.pot
module CSVParser
  module_function
  def target?(file)
    File.extname(file) == ".csv"  # This parser targets csv files only.
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
    CSV.open(file, "r") do |row|
      row.each { |elem| ary << [elem.strip, "csv data: #{file}"] if !elem.nil?}
    end

    ary
  end
end
# Add this parser to GetText::RGetText
GetText::RGetText.add_parser(CSVParser)