# -*-ruby-*-

implDeps = []
guiDeps = []

%w(untangle-casing-mail untangle-casing-ftp untangle-casing-http).each do |c|
  implDeps << BuildEnv::SRC[c]['localapi']
  guiDeps << BuildEnv::SRC[c]['gui']
end

NodeBuilder.makeNode(BuildEnv::SRC, 'untangle-node-clam', 'clam', implDeps, guiDeps, [],
                     { 'virus-base' => BuildEnv::SRC['untangle-base-virus'],
                       'clam-base' => BuildEnv::SRC['untangle-base-clam'] })
