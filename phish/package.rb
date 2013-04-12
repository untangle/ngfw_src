# -*-ruby-*-

deps = []

%w(untangle-base-spam untangle-base-virus untangle-casing-smtp).each do |c|
  deps << BuildEnv::SRC[c]['src']
end

NodeBuilder.makeNode(BuildEnv::SRC, 'untangle-node-phish', 'phish', deps, { 'clam-base' => BuildEnv::SRC['untangle-base-clam'] })


