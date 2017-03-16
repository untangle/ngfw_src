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
            "value": "TRUE"
        }
    ],
        "defaultColumns": ["time_stamp","hostname","username","addr","sender","virus_blocker_clean","virus_blocker_name","c_client_addr","s_server_addr","s_server_port"],
    "description": "Scanned email sessions marked clean.",
    "displayOrder": 1022,
    "javaClass": "com.untangle.app.reports.ReportEntry",
    "table": "mail_addrs",
    "title": "Clean Email Events",
    "uniqueId": "virus-blocker-M3HILGQBQJ"
}
