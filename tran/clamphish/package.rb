# -*-ruby-*-

mail = Package["mail-casing"]
http = Package['http-casing']

TransformBuilder.makeTransform( "clamphish",
                                [ mail["localapi"], http["localapi"] ],
                                [ mail["gui"]], [],
                                [ "spam", "clam-base" ] )
