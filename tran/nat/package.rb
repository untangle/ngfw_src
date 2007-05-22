# -*-ruby-*-

TransformBuilder.makeTransform(BuildEnv::ALPINE, 'nat',
                               [BuildEnv::ALPINE['ftp-casing']['localapi']])
