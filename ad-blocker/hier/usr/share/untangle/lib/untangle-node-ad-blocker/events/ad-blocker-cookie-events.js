{
    "category": "Ad Blocker",
    "conditions": [
        {
            "column": "ad_blocker_cookie_ident",
            "javaClass": "com.untangle.node.reports.SqlCondition",
            "operator": "IS",
            "value": "NOT NULL"
        }
    ],
    "defaultColumns": ["time_stamp","username","hostname","host","uri","ad_blocker_cookie_ident","s_server_addr"],
    "description": "Requests blocked by cookie filters.",
    "displayOrder": 12,
    "javaClass": "com.untangle.node.reports.EventEntry",
    "table": "http_events",
    "title": "Blocked Cookie Events",
    "uniqueId": "ad-blocker-J1XXTX577J"
}
