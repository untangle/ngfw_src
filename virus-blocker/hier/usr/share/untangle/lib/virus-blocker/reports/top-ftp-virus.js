{
    "uniqueId": "virus-blocker-ZjsDfb05",
    "category": "Virus Blocker",
    "description": "The number of blocked viruses by FTP activity.",
    "displayOrder": 204,
    "enabled": true,
    "javaClass": "com.untangle.node.reports.ReportEntry",
    "orderByColumn": "value",
    "orderDesc": true,
    "units": "hits",
    "pieGroupColumn": "virus_blocker_name",
    "pieSumColumn": "count(*)",
    "conditions": [
        {
            "column": "virus_blocker_clean",
            "javaClass": "com.untangle.node.reports.SqlCondition",
            "operator": "=",
            "value": "false"
        }
    ],
    "readOnly": true,
    "table": "ftp_events",
    "title": "FTP Top Blocked Viruses",
    "pieStyle": "PIE",
    "type": "PIE_GRAPH"
}
