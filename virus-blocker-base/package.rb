# -*-ruby-*-

deps = []

%w(smtp ftp http).each do |c|
  deps << BuildEnv::SRC[c]['src']
end

virus = BuildEnv::SRC['virus-blocker-base']

NodeBuilder.makeBase(BuildEnv::SRC, 'virus-blocker-base', 'virus-blocker-base', deps)

http = BuildEnv::SRC['http']

deps = [virus['src'], http['src']]

ServletBuilder.new(virus, 'com.untangle.app.virus_blocker.jsp', "./virus-blocker-base/servlets/virus", [], deps, [], [BuildEnv::SERVLET_COMMON])
