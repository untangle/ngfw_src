{
    "uniqueId": "firewall-",
    "category": "Firewall",
    "description": "A summary of firewall actions.",
    "displayOrder": 10,
    "enabled": true,
    "javaClass": "com.untangle.node.reporting.ReportEntry",
    "preCompileResults": false,
    "textColumns": [
        "count(*) as scanned",
        "sum(firewall_flagged::int) as flagged",
        "sum(firewall_blocked::int) as blocked"
    ],
    "conditions": [
        {
            "javaClass": "com.untangle.uvm.node.SqlCondition",
            "column": "firewall_rule_index",
            "operator": "is",
            "value": "not null"
        }
    ],
    "textString": "Firewall scanned {0} sessions and flagged {1} sessions of which {2} were blocked.", 
    "readOnly": true,
    "table": "sessions",
    "title": "Summary",
    "type": "TEXT"
}
