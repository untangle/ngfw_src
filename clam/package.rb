# -*-ruby-*-

deps = []

%w(untangle-casing-smtp untangle-casing-ftp untangle-casing-http untangle-base-virus).each do |c|
  deps << BuildEnv::SRC[c]['src']
end

NodeBuilder.makeNode(BuildEnv::SRC, 'untangle-node-clam', 'clam', deps, { 'clam-base' => BuildEnv::SRC['untangle-base-clam'] })
