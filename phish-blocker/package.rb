# -*-ruby-*-

deps = []

%w(untangle-base-spam-blocker untangle-base-virus-blocker untangle-casing-smtp).each do |c|
  deps << BuildEnv::SRC[c]['src']
end

NodeBuilder.makeNode(BuildEnv::SRC, 'untangle-node-phish-blocker', 'phish-blocker', deps, { 'clam-base' => BuildEnv::SRC['untangle-base-clam'] })


