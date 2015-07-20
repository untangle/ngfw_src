{
    "category": "Firewall",
    "conditions": [
        {
            "column": "firewall_rule_index",
            "javaClass": "com.untangle.node.reporting.SqlCondition",
            "operator": "is",
            "value": "NOT NULL"
        }
    ],
    "defaultColumns": ["time_stamp","username","hostname","c_client_port","firewall_blocked","firewall_flagged","firewall_rule_index","s_server_addr","s_server_port"],
    "description": "All events scanned by Firewall App.",
    "displayOrder": 10,
    "javaClass": "com.untangle.node.reporting.EventEntry",
    "table": "sessions",
    "title": "All Events",
    "uniqueId": "firewall-JWVPEDU3Y6"
}
