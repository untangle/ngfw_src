# -*-ruby-*-

mail = Package["mail-casing"]

TransformBuilder.makeTransform( "clamphish", [ mail["localapi"]], [ mail["gui"]], [], 
                                [ "spam", "clam-base" ] )
