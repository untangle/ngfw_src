{
    "uniqueId": "Administration-tFb0iLvxHE",
    "category": "Administration",
    "description": "The number of total, successful, and failed admin logins over time.",
    "displayOrder": 100,
    "enabled": true,
    "javaClass": "com.untangle.app.reports.ReportEntry",
    "orderDesc": false,
    "units": "sessions",
    "readOnly": true,
    "table": "admin_logins",
    "conditions": [
        {
            "javaClass": "com.untangle.app.reports.SqlCondition",
            "column": "login",
            "operator": "!=",
            "value": "localadmin"
        }
    ],
    "timeDataColumns": [
        "count(*) as total",
        "sum(succeeded::int) as succeeded",
        "sum((not succeeded)::int) as failed"
    ],
    "colors": [
        "#b2b2b2",
        "#396c2b",
        "#8c0000"
    ],
    "timeDataInterval": "AUTO",
    "timeStyle": "BAR_OVERLAPPED",
    "title": "Admin Logins",
    "type": "TIME_GRAPH"
}
