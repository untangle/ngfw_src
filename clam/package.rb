# -*-ruby-*-

implDeps = []

%w(untangle-casing-smtp untangle-casing-ftp untangle-casing-http).each do |c|
  implDeps << BuildEnv::SRC[c]['api']
end

NodeBuilder.makeNode(BuildEnv::SRC, 'untangle-node-clam', 'clam', implDeps, implDeps,
                     { 'virus-base' => BuildEnv::SRC['untangle-base-virus'],
                       'clam-base' => BuildEnv::SRC['untangle-base-clam'] })
