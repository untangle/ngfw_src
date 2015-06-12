{
    "uniqueId": "intrusion-prevention-c4BSL6qq",
    "category": "Intrusion Prevention",
    "description": "The number of intrusions blocked by classtype.",
    "displayOrder": 302,
    "enabled": true,
    "javaClass": "com.untangle.node.reporting.ReportEntry",
    "orderByColumn": "value",
    "orderDesc": true,
    "units": "hits",
    "pieGroupColumn": "classtype",
    "pieSumColumn": "sum(blocked::int)",
    "readOnly": true,
    "table": "intrusion_prevention_events",
    "title": "Top Classtypes (blocked)",
    "type": "PIE_GRAPH"
}
