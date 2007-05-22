# -*-ruby-*-

mail = BuildEnv::ALPINE['mail-casing']

TransformBuilder.makeTransform(BuildEnv::ALPINE, 'spamassassin', [mail['localapi']], [ mail['gui']], [], 'spam')
