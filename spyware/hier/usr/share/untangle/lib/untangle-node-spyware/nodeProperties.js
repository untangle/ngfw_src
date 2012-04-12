{
        "javaClass": "com.untangle.uvm.node.NodeProperties",
        "className" : "com.untangle.node.spyware.SpywareImpl",
        "name" : "untangle-node-spyware",
        "displayName" : "Spyware Blocker",
        "syslogName" : "Spyware_Blocker",
        "type" : "NODE",
        "viewPosition" : 50,
        "parents" : {
            "javaClass": "java.util.LinkedList",
            "list": [
                "untangle-casing-http"
            ]
        },
        "annotatedClasses" : {
            "javaClass": "java.util.LinkedList",
            "list": [
                "com.untangle.node.http.HttpSettings",
                "com.untangle.node.http.HttpRequestEvent",
                "com.untangle.node.http.HttpResponseEvent",
                "com.untangle.node.spyware.SpywareAccessEvent",
                "com.untangle.node.spyware.SpywareUrlEvent",
                "com.untangle.node.spyware.SpywareCookieEvent"
            ]
        }
}

