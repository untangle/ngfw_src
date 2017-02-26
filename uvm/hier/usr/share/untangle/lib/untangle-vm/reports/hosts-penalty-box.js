{
    "category": "Hosts",
    "readOnly": true,
    "type": "EVENT_LIST",
    "conditions": [
        {
            "column": "value",
            "javaClass": "com.untangle.node.reports.SqlCondition",
            "operator": "like",
            "value": "%penalty-box%"
        }
    ],
    "defaultColumns": ["time_stamp","address","reason","start_time","end_time"],
    "description": "Shows when hosts are placed in the penalty box and when the penalty box expires.",
    "displayOrder": 1012,
    "javaClass": "com.untangle.node.reports.ReportEntry",
    "table": "host_table_updates",
    "title": "Penalty Box Events",
    "uniqueId": "host-viewer-Kg7KXElMd7"
}
