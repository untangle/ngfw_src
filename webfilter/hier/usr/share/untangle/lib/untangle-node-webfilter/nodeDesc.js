{
        "javaClass": "com.untangle.uvm.node.NodeDesc",
        "className" : "com.untangle.node.webfilter.WebFilterImpl",
        "nodeBase" : "untangle-base-webfilter",
        "name" : "untangle-node-webfilter",
        "displayName" : "Web Filter Lite",
        "syslogName" : "Web_Filter_Lite",
        "type" : "NODE",
        "viewPosition" : 20,
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
                "com.untangle.node.webfilter.BlockTemplate",
                "com.untangle.node.webfilter.UnblockEvent"
            ]
        }
}

