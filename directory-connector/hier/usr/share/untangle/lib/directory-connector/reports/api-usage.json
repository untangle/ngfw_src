{
    "uniqueId": "directory-connector-D6IabIxIrC",
    "category": "Directory Connector",
    "description": "The amount of login, update and logout user notification API events over time.",
    "displayOrder": 100,
    "enabled": true,
    "javaClass": "com.untangle.app.reports.ReportEntry",
    "orderDesc": false,
    "units": "events",
    "readOnly": true,
    "table": "directory_connector_login_events",
    "timeDataColumns": [
        "sum(case when type='I' then 1 else 0 end) as logins",
        "sum(case when type='O' then 1 else 0 end) as logouts",
        "sum(case when type='U' then 1 else 0 end) as updates"
    ],
    "colors": [
        "#396c2b",
        "#0099ff",
        "#6600ff"
    ],
    "timeDataInterval": "AUTO",
    "timeStyle": "BAR",
    "title": "API Usage",
    "type": "TIME_GRAPH"
}
