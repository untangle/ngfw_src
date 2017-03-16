{
    "category": "Firewall",
    "readOnly": true,
    "type": "EVENT_LIST",
    "conditions": [
        {
            "column": "firewall_rule_index",
            "javaClass": "com.untangle.app.reports.SqlCondition",
            "operator": "is",
            "value": "NOT NULL"
        }
    ],
    "defaultColumns": ["time_stamp","username","hostname","protocol","c_client_port","firewall_blocked","firewall_flagged","firewall_rule_index","c_client_addr","s_server_addr","s_server_port"],
    "description": "All events scanned by Firewall App.",
    "displayOrder": 1000,
    "javaClass": "com.untangle.app.reports.ReportEntry",
    "table": "sessions",
    "title": "All Events",
    "uniqueId": "firewall-JWVPEDU3Y6"
}
