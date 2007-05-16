# -*-ruby-*-

mail = Package['mail-casing']

TransformBuilder.makeTransform('spamassassin', [mail['localapi']], [ mail['gui']], [], 'spam')
