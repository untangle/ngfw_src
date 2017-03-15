{
        "javaClass": "com.untangle.uvm.node.NodeProperties",
        "className" : "com.untangle.node.phish_blocker.PhishBlockerApp",
        "nodeBase" : "spam-blocker-base",
        "name" : "phish-blocker",
        "displayName" : "Phish Blocker",
        "type" : "FILTER",
        "viewPosition" : 70,
        "parents" : {
            "javaClass": "java.util.LinkedList",
            "list": [
                "http",
                "smtp"
            ]
        },
        "supportedArchitectures" : {
            "javaClass": "java.util.LinkedList",
            "list": ["i386","amd64"]
        },
        "minimumMemory": 1200000000
}

