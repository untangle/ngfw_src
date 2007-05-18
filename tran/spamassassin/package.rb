# -*-ruby-*-

mail = Package['mail-casing']

TransformBuilder.makeTransform(ALPINE_HOME, 'spamassassin', [mail['localapi']], [ mail['gui']], [], 'spam')
