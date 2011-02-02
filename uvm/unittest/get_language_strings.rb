
lm = Untangle::RemoteUvmContext.languageManager()

language_strings = lm.getTranslations(ARGV[0])

language_strings["map"].each do |k,v|
  puts "#{k} => #{v}"
end
