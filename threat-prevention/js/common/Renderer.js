Ext.define('Ung.common.Renderer.threatprevention', {
    singleton: true,

    ruleIdMap: {},
    ruleId: function(value, row, record){
        if(value == 0){
            return;
        }
        var policyId = record && record.get && record.get('policy_id') ? record.get('policy_id') : 1;
        if(!Ung.common.Renderer.threatprevention.ruleIdMap[policyId] ||
            !Ung.common.Renderer.threatprevention.ruleIdMap[policyId] ||
            (value != Ung.common.Renderer.threatprevention.listKey && !(value in Ung.common.Renderer.threatprevention.ruleIdMap[policyId]))){
            if(!Ung.common.Renderer.threatprevention.ruleIdMap[policyId]){
                Ung.common.Renderer.threatprevention.ruleIdMap[policyId] = {};
            }
            var ruleInfo = Renderer.getReportInfo(record, ["threat-prevention"], "rules");

            if(ruleInfo){
                ruleInfo.forEach( function(rule){
                    Ung.common.Renderer.threatprevention.ruleIdMap[policyId][rule["ruleId"]] = rule["description"];
                });
            }
            if(!(value in Ung.common.Renderer.threatprevention.ruleIdMap[policyId]) && (value != Ung.common.Renderer.threatprevention.listKey)){
                // If category cannot be found, don't just keep coming back for more.
                Ung.common.Renderer.threatprevention.ruleIdMap[policyId][value] = 'Unknown'.t();
            }
        }
        if(value == Renderer.listKey){
            return Ung.common.Renderer.threatprevention.ruleIdMap[policyId];
        }else{
            return Ung.common.Renderer.threatprevention.ruleIdMap[policyId][value];
        }
    },

    reputationMap:{
        20: 'High Risk'.t(),
        40: 'Suspicious'.t(),
        60: 'Moderate Risk'.t(),
        80: 'Low Risk'.t(),
        100: 'Trustworthy'.t()
    },
    reputation: function(value, cell, record){
        if(value == 0 || value == null){
            return null;
        }
        var description = '';
        var reputationMaxes = Object.keys(Ung.common.Renderer.threatprevention.reputationMap);
        for(var i = 0; i < reputationMaxes.length; i++){
            if(value <= reputationMaxes[i]){
                description = Ung.common.Renderer.threatprevention.reputationMap[reputationMaxes[i]];
                break;
            }
        }
        return description;
    },

    reputationDetails: function(value) {
        if(value == 0 || value == null){
            return null;
        }

        return Ung.common.threatprevention.references.getReputationLevel(value).get('details');
    },

    age: function(value) {
        return Ext.String.format('{0} months'.t(), value);
    },

    webCategories: function(values) {
        if(values == 0 || values == null){
            return null;
        }

        if(Array.isArray(values)) {
            var currentCategories = [];

            for(var i in values) {
                var shortDesc = Renderer.webCategory(values[i].catid);
                var conf = " (" + values[i].conf + " % Confidence)";

                currentCategories.push(shortDesc + conf);
            }

            return currentCategories.join(", ");
        }

        return null;
    },

    reputationHistory: function(values) {
        if(values == 0 || values == null){
            return null;
        }       

        if(Array.isArray(values)) {
            var repHistory = [];

            var repTable = "<table><thead><td>Date</td><td>Categories</td></thead>";

            for(var i in values) {
                var cats = Ung.common.Renderer.threatprevention.webCategories(values[i].categories);
                var date = Renderer.timestamp(values[i].timestamp);

                repTable += "<tr><td>" + date + "</td><td>" + cats + "</td></tr>";
                repHistory.push("Date: "+ date + " Categories: " + cats);
            }

            repTable += "</table>";

            return repTable;
        }

        return null;
    },

    categoryMap:{
        0: 'Spam Sources'.t(),
        1: 'Windows Exploits'.t(),
        2: 'Web Attacks'.t(),
        3: 'Botnets'.t(),
        4: 'Scanners'.t(),
        5: 'Denial of Service'.t(),
        6: 'Reputation'.t(),
        7: 'Phishing'.t(),
        8: 'Proxy'.t(),
        11: 'Mobile Threats'.t(),
        13: 'Tor Proxy'.t(),
        16: 'Keyloggers'.t(),
        17: 'Malware'.t(),
        18: 'Spyware'.t()
    },
    category: function(value){
        if(value == 0){
            return null;
        }
        var descriptions = [];
        var threatBits = Object.keys(Ung.common.Renderer.threatprevention.categoryMap);
        for(var i = 0; i < threatBits.length; i++){
            if(Math.pow(2,threatBits[i]) & value){
                descriptions.push(Ung.common.Renderer.threatprevention.categoryMap[threatBits[i]]);
            }
        }
        return descriptions.join(', ');
    },

    ipPopularityMap:{
        1: 'Top 100,000'.t(),
        2: 'Top 1M'.t(),
        3: 'Top 10M'.t(),
        4: 'Lower than 10M'.t(),
        5: 'Unranked'.t()
    },
    ipPopularity: function(value){
        if(value == 0){
            return null;
        }
        return Ext.String.format(
                Renderer.mapValueFormat,
                ( value in Ung.common.Renderer.threatprevention.ipPopularityMap ) ? Ung.common.Renderer.threatprevention.ipPopularityMap[value] : Ung.common.Renderer.threatprevention.ipPopularity[5],
                value
        );
    }
});