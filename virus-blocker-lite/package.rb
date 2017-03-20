# -*-ruby-*-

deps = []

%w(untangle-casing-smtp untangle-casing-ftp untangle-casing-http untangle-base-virus-blocker).each do |c|
  deps << BuildEnv::SRC[c]['src']
end

AppBuilder.makeApp(BuildEnv::SRC, 'untangle-node-virus-blocker-lite', 'virus-blocker-lite', deps, { 'clam-base' => BuildEnv::SRC['clam-base'] })
