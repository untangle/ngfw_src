# -*-ruby-*-

implDeps = []

%w(untangle-casing-smtp untangle-casing-ftp untangle-casing-http).each do |c|
  implDeps << BuildEnv::SRC[c]['api']
end

virus = BuildEnv::SRC['untangle-base-virus']

NodeBuilder.makeBase(BuildEnv::SRC, 'untangle-base-virus', 'virus-base', implDeps)

http = BuildEnv::SRC['untangle-casing-http']

deps = [virus['api'], http['api']]

ServletBuilder.new(virus, 'com.untangle.node.virus.jsp', "./virus-base/servlets/virus", [], deps, [], [BuildEnv::SERVLET_COMMON])
