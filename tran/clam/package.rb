# -*-ruby-*-

implDeps = []
guiDeps = []

%w(mail-casing ftp-casing http-casing).each do |c|
  implDeps << BuildEnv::ALPINE[c]['localapi']
  guiDeps << BuildEnv::ALPINE[c]['gui']
end

TransformBuilder.makeTransform(BuildEnv::ALPINE, 'clam', implDeps, guiDeps, [],
                               ['virus', 'clam-base'])
