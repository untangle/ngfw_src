{
        "javaClass": "com.untangle.uvm.node.NodeDesc",
        "className" : "com.untangle.node.ips.IpsNodeImpl",
        "name" : "untangle-node-ips",
        "displayName" : "Intrusion Prevention",
        "syslogName" : "Intrusion_Prevention",
        "type" : "NODE",
        "viewPosition" : 75,
        "autoStart" : "false",
        "parents" : {
            "javaClass": "java.util.LinkedList",
            "list": [
                "untangle-casing-http"
            ]
        },
        "annotatedClasses" : {
            "javaClass": "java.util.LinkedList",
            "list": [
                "com.untangle.node.ips.IpsRule",
                "com.untangle.node.ips.IpsSettings",
                "com.untangle.node.ips.IpsVariable",
                "com.untangle.node.ips.IpsStatisticEvent",
                "com.untangle.node.ips.IpsLogEvent"
            ]
        }
}

