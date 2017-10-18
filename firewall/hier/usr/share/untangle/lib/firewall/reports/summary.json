{
    "uniqueId": "firewall-upl31dqKb1",
    "category": "Firewall",
    "description": "A summary of firewall actions.",
    "displayOrder": 11,
    "enabled": true,
    "javaClass": "com.untangle.app.reports.ReportEntry",
    "textColumns": [
        "count(*) as scanned",
        "sum(firewall_flagged::int) as flagged",
        "sum(firewall_blocked::int) as blocked"
    ],
    "conditions": [
        {
            "javaClass": "com.untangle.app.reports.SqlCondition",
            "column": "firewall_rule_index",
            "operator": "is",
            "value": "not null"
        }
    ],
    "textString": "Firewall scanned {0} sessions and flagged {1} sessions of which {2} were blocked.", 
    "readOnly": true,
    "table": "sessions",
    "title": "Firewall Summary",
    "type": "TEXT"
}
