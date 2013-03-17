# -*-ruby-*-

implDeps = []

%w(untangle-casing-smtp untangle-casing-ftp untangle-casing-http).each do |c|
  implDeps << BuildEnv::SRC[c]['api']
end

NodeBuilder.makeBase(BuildEnv::SRC, 'untangle-base-clam', 'clam-base', implDeps)
