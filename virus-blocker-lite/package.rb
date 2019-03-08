# -*-ruby-*-

deps = []

%w(untangle-app-smtp untangle-app-ftp untangle-app-http untangle-base-virus-blocker).each do |c|
  deps << BuildEnv::SRC[c]['src']
end

AppBuilder.makeApp(BuildEnv::SRC, 'untangle-app-virus-blocker-lite', 'virus-blocker-lite', deps, { 'clam-base' => BuildEnv::SRC['clam-base'] })
