# -*-ruby-*-

implDeps = []
guiDeps = []

%w(mail-casing).each do |c|
  implDeps << Package[c]['localapi']
  guiDeps << Package[c]['gui']
end

TransformBuilder.makeBase('spam', implDeps, guiDeps)
