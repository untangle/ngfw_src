# -*-ruby-*-

mail = Package["mail-casing"]
http = Package['http-casing']

TransformBuilder.makeTransform("clamphish",
                               [ mail["localapi"], http["localapi"] ],
                               [ mail["gui"], http["gui"] ], [],
                               [ "spam", "clam-base" ])

# ServletBuilder.new(clamphish, 'com.untangle.tran.clamphish.jsp',
#                    'tran/clamphish/servlets/clamphish', [], deps, [],
#                    [$BuildEnv.servletcommon])
