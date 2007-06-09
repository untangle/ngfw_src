# -*-ruby-*-

implDeps = []
guiDeps = []

%w(mail-casing ftp-casing http-casing).each do |c|
  implDeps << BuildEnv::SRC[c]['localapi']
  guiDeps << BuildEnv::SRC[c]['gui']
end

NodeBuilder.makeNode(BuildEnv::SRC, 'kav', implDeps, guiDeps, [],
                               'virus')
