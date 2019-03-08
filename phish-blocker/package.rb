# -*-ruby-*-

deps = []

%w(untangle-base-spam-blocker untangle-base-virus-blocker untangle-app-smtp).each do |c|
  deps << BuildEnv::SRC[c]['src']
end

AppBuilder.makeApp(BuildEnv::SRC, 'untangle-app-phish-blocker', 'phish-blocker', deps, { 'clam-base' => BuildEnv::SRC['clam-base'] })


