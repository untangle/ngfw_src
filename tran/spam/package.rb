# -*-ruby-*-

implDeps = []
guiDeps = []

%w(mail-casing).each do |c|
  implDeps << BuildEnv::ALPINE[c]['localapi']
  guiDeps << BuildEnv::ALPINE[c]['gui']
end

TransformBuilder.makeBase(BuildEnv::ALPINE, 'spam', implDeps, guiDeps)
