# -*-ruby-*-

deps = []

%w(spam-blocker-base virus-blocker-base smtp).each do |c|
  deps << BuildEnv::SRC[c]['src']
end

AppBuilder.makeApp(BuildEnv::SRC, 'phish-blocker', 'phish-blocker', deps, { 'clam-base' => BuildEnv::SRC['clam-base'] })


