# -*-ruby-*-

deps = []

%w(untangle-casing-smtp untangle-casing-ftp untangle-casing-http virus-blocker-base).each do |c|
  deps << BuildEnv::SRC[c]['src']
end

AppBuilder.makeApp(BuildEnv::SRC, 'untangle-node-virus-blocker-lite', 'virus-blocker-lite', deps, { 'clam-base' => BuildEnv::SRC['clam-base'] })
