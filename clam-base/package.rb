# -*-ruby-*-

deps = []

%w(untangle-casing-smtp untangle-casing-ftp untangle-casing-http).each do |c|
  deps << BuildEnv::SRC[c]['src']
end

NodeBuilder.makeBase(BuildEnv::SRC, 'untangle-base-clam', 'clam-base', deps)
