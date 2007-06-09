# -*-ruby-*-

implDeps = []
guiDeps = []

%w(mail-casing).each do |c|
  implDeps << BuildEnv::SRC[c]['localapi']
  guiDeps << BuildEnv::SRC[c]['gui']
end

NodeBuilder.makeBase(BuildEnv::SRC, 'spam', implDeps, guiDeps)
