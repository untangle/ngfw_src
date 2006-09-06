# -*-ruby-*-

http = Package["http-casing"]

TransformBuilder.makeTransform( "httpblocker", [ http["localapi"]], [ http["gui"]]  )
