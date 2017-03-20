# -*-ruby-*-

deps = []

%w(spam-blocker-base virus-blocker-base untangle-casing-smtp).each do |c|
  deps << BuildEnv::SRC[c]['src']
end

AppBuilder.makeApp(BuildEnv::SRC, 'untangle-node-phish-blocker', 'phish-blocker', deps, { 'clam-base' => BuildEnv::SRC['clam-base'] })


