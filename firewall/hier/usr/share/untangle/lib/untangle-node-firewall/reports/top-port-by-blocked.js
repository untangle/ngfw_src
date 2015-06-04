{
    "uniqueId": "firewall-uywAGHuIud",
    "category": "Firewall",
    "description": "The number of flagged session grouped by server (destination) port.",
    "displayOrder": 702,
    "enabled": true,
    "javaClass": "com.untangle.node.reporting.ReportEntry",
    "orderByColumn": "value",
    "orderDesc": true,
    "units": "hits",
    "pieGroupColumn": "s_server_port",
    "pieSumColumn": "count(*)",
    "preCompileResults": false,
    "conditions": [
        {
            "javaClass": "com.untangle.uvm.node.SqlCondition",
            "column": "firewall_blocked",
            "operator": "=",
            "value": "true"
        }
    ],
    "readOnly": true,
    "table": "sessions",
    "title": "Top Blocked Server Ports",
    "type": "PIE_GRAPH"
}
