{
    "category": "Virus Blocker",
    "readOnly": true,
    "type": "EVENT_LIST",
    "conditions": [
        {
            "column": "addr_kind",
            "javaClass": "com.untangle.app.reports.SqlCondition",
            "operator": "=",
            "value": "B"
        },
        {
            "column": "virus_blocker_clean",
            "javaClass": "com.untangle.app.reports.SqlCondition",
            "operator": "is",
            "value": "FALSE"
        }
    ],
    "defaultColumns": ["time_stamp","hostname","username","addr","sender","virus_blocker_clean","virus_blocker_name","c_client_addr","s_server_addr","s_server_port"],
    "description": "Infected email sessions blocked by Virus Blocker.",
    "displayOrder": 1021,
    "javaClass": "com.untangle.app.reports.ReportEntry",
    "table": "mail_addrs",
    "title": "Infected Email Events",
    "uniqueId": "virus-blocker-P6WT1M7P80"
}
