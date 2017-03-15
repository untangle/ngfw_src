{
    "uniqueId": "intrusion-prevention-I1utyRyO",
    "category": "Intrusion Prevention",
    "description": "The number of intrusions detected grouped by source port.",
    "displayOrder": 601,
    "enabled": true,
    "javaClass": "com.untangle.node.reports.ReportEntry",
    "orderByColumn": "value",
    "orderDesc": true,
    "units": "hits",
    "pieGroupColumn": "source_port",
    "pieSumColumn": "count(*)",
    "readOnly": true,
    "table": "intrusion_prevention_events",
    "title": "Top Source Ports (logged)",
    "pieStyle": "PIE",
    "type": "PIE_GRAPH"
}
