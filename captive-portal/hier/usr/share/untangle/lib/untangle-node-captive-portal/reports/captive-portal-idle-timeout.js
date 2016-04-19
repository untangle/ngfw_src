{
    "category": "Captive Portal",
    "type": "EVENT_LIST",
    "conditions": [
        {
            "column": "event_info",
            "javaClass": "com.untangle.node.reports.SqlCondition",
            "operator": "=",
            "value": "INACTIVE"
        }
    ],
    "defaultColumns": ["time_stamp","client_addr","login_name","event_info","auth_type"],
    "description": "Sessions that reached the idle timeout.",
    "displayOrder": 24,
    "javaClass": "com.untangle.node.reports.ReportEntry",
    "table": "captive_portal_user_events",
    "title": "Idle Timeout",
    "uniqueId": "captive-portal-XT3EOQP18D"
}
