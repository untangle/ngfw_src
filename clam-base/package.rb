# -*-ruby-*-

deps = []

%w(untangle-casing-smtp untangle-casing-ftp untangle-casing-http untangle-base-virus-blocker).each do |c|
  deps << BuildEnv::SRC[c]['src']
end

AppBuilder.makeBase(BuildEnv::SRC, 'clam-base', 'clam-base', deps)
