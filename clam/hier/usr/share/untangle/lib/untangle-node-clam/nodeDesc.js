{
        "javaClass": "com.untangle.uvm.node.NodeDesc",
        "className" : "com.untangle.node.clam.ClamNode",
        "name" : "untangle-node-clam",
        "nodeBase" : "untangle-base-virus",
        "displayName" : "Virus Blocker Lite",
        "syslogName" : "Virus_Blocker_Lite",
        "type" : "NODE",
        "viewPosition" : 70,
        "parents" : {
            "javaClass": "java.util.LinkedList",
            "list": [
                "untangle-casing-http",
                "untangle-casing-ftp",
                "untangle-casing-mail"
            ]
        },
        "annotatedClasses" : {
            "javaClass": "java.util.LinkedList",
            "list": [
                "com.untangle.node.virus.VirusConfig",
                "com.untangle.node.virus.VirusIMAPConfig",
                "com.untangle.node.virus.VirusMailEvent",
                "com.untangle.node.virus.VirusPOPConfig",
                "com.untangle.node.virus.VirusSMTPConfig",
                "com.untangle.node.virus.VirusSettings",
                "com.untangle.node.virus.VirusBaseSettings",
                "com.untangle.node.virus.VirusHttpEvent",
                "com.untangle.node.virus.VirusLogEvent",
                "com.untangle.node.virus.VirusSmtpEvent"
            ]
        }
}

