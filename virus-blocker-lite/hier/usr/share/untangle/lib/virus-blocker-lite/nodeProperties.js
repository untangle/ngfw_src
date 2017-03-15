{
        "javaClass": "com.untangle.uvm.node.NodeProperties",
        "className" : "com.untangle.node.virus_blocker_lite.VirusBlockerLiteApp",
        "name" : "virus-blocker-lite",
        "nodeBase" : "virus-blocker-base",
        "displayName" : "Virus Blocker Lite",
        "type" : "FILTER",
        "viewPosition" : 40,
        "parents" : {
            "javaClass": "java.util.LinkedList",
            "list": [
                "http",
                "ftp",
                "smtp"
            ]
        },
        "supportedArchitectures" : {
            "javaClass": "java.util.LinkedList",
            "list": ["i386","amd64"]
        }
}

