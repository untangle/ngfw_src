{
        "javaClass": "com.untangle.uvm.app.AppProperties",
        "className" : "com.untangle.app.phish_blocker.PhishBlockerApp",
        "appBase" : "spam-blocker-base",
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

