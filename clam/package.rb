# -*-ruby-*-

deps = []

%w(untangle-casing-smtp untangle-casing-ftp untangle-casing-http).each do |c|
  deps << BuildEnv::SRC[c]['api']
end

NodeBuilder.makeNode(BuildEnv::SRC, 'untangle-node-clam', 'clam', deps, deps,
                     { 'virus-base' => BuildEnv::SRC['untangle-base-virus'], 'clam-base' => BuildEnv::SRC['untangle-base-clam'] })
