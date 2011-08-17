{
        "javaClass": "com.untangle.uvm.node.NodeDesc",
        "className" : "com.untangle.node.webfilter.WebFilterImpl",
        "nodeBase" : "untangle-base-webfilter",
        "name" : "untangle-node-webfilter",
        "displayName" : "Web Filter Lite",
        "syslogName" : "Web_Filter_Lite",
        "type" : "NODE",
        "viewPosition" : 30,
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
                "com.untangle.node.http.RequestLine",
                "com.untangle.node.http.HttpResponseEvent",
                "com.untangle.node.webfilter.BlockTemplate",
                "com.untangle.node.webfilter.WebFilterEvent",
                "com.untangle.node.webfilter.UnblockEvent"
            ]
        }
}

