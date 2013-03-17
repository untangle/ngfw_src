# -*-ruby-*-

deps = []

%w(untangle-casing-smtp untangle-casing-ftp untangle-casing-http).each do |c|
  deps << BuildEnv::SRC[c]['api']
end

NodeBuilder.makeBase(BuildEnv::SRC, 'untangle-base-clam', 'clam-base', deps)
