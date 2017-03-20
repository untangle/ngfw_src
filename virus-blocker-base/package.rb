# -*-ruby-*-

deps = []

%w(untangle-app-smtp untangle-app-ftp untangle-app-http).each do |c|
  deps << BuildEnv::SRC[c]['src']
end

virus = BuildEnv::SRC['untangle-base-virus-blocker']

AppBuilder.makeBase(BuildEnv::SRC, 'untangle-base-virus-blocker', 'virus-blocker-base', deps)

http = BuildEnv::SRC['untangle-app-http']

deps = [virus['src'], http['src']]

ServletBuilder.new(virus, 'com.untangle.app.virus_blocker.jsp', "./virus-blocker-base/servlets/virus", [], deps, [], [BuildEnv::SERVLET_COMMON])
