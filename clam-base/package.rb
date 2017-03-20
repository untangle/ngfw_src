# -*-ruby-*-

deps = []

%w(untangle-app-smtp untangle-app-ftp untangle-app-http untangle-base-virus-blocker).each do |c|
  deps << BuildEnv::SRC[c]['src']
end

AppBuilder.makeBase(BuildEnv::SRC, 'clam-base', 'clam-base', deps)
