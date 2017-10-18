{
    "uniqueId": "wan-failover-dGV53h7aa5",
    "category": "WAN Failover",
    "description": "The number of disconnect events grouped by WAN.",
    "displayOrder": 100,
    "enabled": true,
    "javaClass": "com.untangle.app.reports.ReportEntry",
    "orderByColumn": "interface_id",
    "orderDesc": false,
    "units": "disconnects",
    "pieGroupColumn": "interface_id",
    "pieSumColumn": "count(*)",
    "conditions": [
        {
            "javaClass": "com.untangle.app.reports.SqlCondition",
            "column": "action",
            "operator": "=",
            "value": "DISCONNECTED"
        }
    ],
    "readOnly": true,
    "table": "wan_failover_action_events",
    "title": "WAN Disconnect Events",
    "pieStyle": "PIE",
    "type": "PIE_GRAPH"
}
