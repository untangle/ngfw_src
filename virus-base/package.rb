# -*-ruby-*-

deps = []

%w(untangle-casing-smtp untangle-casing-ftp untangle-casing-http).each do |c|
  deps << BuildEnv::SRC[c]['src']
end

virus = BuildEnv::SRC['untangle-base-virus']

NodeBuilder.makeBase(BuildEnv::SRC, 'untangle-base-virus', 'virus-base', deps)

http = BuildEnv::SRC['untangle-casing-http']

deps = [virus['src'], http['src']]

ServletBuilder.new(virus, 'com.untangle.node.virus.jsp', "./virus-base/servlets/virus", [], deps, [], [BuildEnv::SERVLET_COMMON])
