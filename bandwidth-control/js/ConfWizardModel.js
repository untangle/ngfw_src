Ext.define('Ung.apps.bandwidthcontrol.ConfWizardModel', {
    extend: 'Ext.app.ViewModel',
    alias: 'viewmodel.app-bandwidth-control-wizard',

    formulas: {
        selectedConf: {
            get: function () {
                return;
            },
            set: function (val) {
                var details = this.get('confs')[val], html = '<p>' + details.desc + '</p>';
                if (details.benefits) {
                    html += '<p style="font-weight: bold;">' + 'Benefits:'.t() + '</p><ul>';
                    details.benefits.forEach(function (val) { html += '<li>' + val + '</li>'; });
                    html += '</ul>';
                }
                if (details.prioritizes) {
                    html += '<p style="font-weight: bold;">' + 'Prioritizes:'.t() + '</p><ul>';
                    details.prioritizes.forEach(function (val) { html += '<li>' + val + '</li>'; });
                    html += '</ul>';
                }
                if (details.deprioritizes) {
                    html += '<p style="font-weight: bold;">' + 'De-prioritizes:'.t() + '</p><ul>';
                    details.deprioritizes.forEach(function (val) { html += '<li>' + val + '</li>'; });
                    html += '</ul>';
                }
                if (details.limits) {
                    html += '<p style="font-weight: bold;">' + 'Limits:'.t() + '</p><ul>';
                    details.limits.forEach(function (val) { html += '<li>' + val + '</li>'; });
                    html += '</ul>';
                }
                this.set('confDetails', html);
            }
        }
    },

    data: {
        // nextBtnText: 'WAN Bandwidth'.t(),

        quota: {
            enabled: false,
            hostEnabled: true,
            userEnabled: true,
            expiration: -2,
            size: 1,
            unit: 1024*1024*1024,
            priority: 6
        },

        confs: {
            business_business: {
                desc: 'This initial configuration provides common settings suitable for most small and medium-sized businesses.'.t(),
                benefits: [
                    'optimizes internet responsiveness by prioritizing traffic of interactive services most commonly used in businesses'.t(),
                    'de-prioritizes traffic of greedy non-work-related activities such as peer-to-peer file sharing'.t(),
                    'de-prioritizes non-real-time background traffic, to avoid slowing your internet connection when you need it most'.t()
                ],
                prioritizes: [
                    'interactive traffic and services (Remote Desktop, Email, DNS, SSH)'.t(),
                    'interactive web traffic'.t()
                ],
                deprioritizes: [
                    'non-real-time background services (e.g. Microsoft&reg; updates, backup services)'.t(),
                    'any detected Peer-to-Peer traffic'.t(),
                    'all web traffic in violation of company policy'.t()
                ]
            },
            school_school: {
                desc: 'This initial configuration provides common settings suitable for most School Districts, Elementary through High Schools, or Charter Schools.'.t(),
                benefits: [
                    'optimizes internet responsiveness by prioritizing traffic of interactive services most commonly used in schools'.t(),
                    'prioritizes school-related traffic, such as traffic to Education sites or Search Engines'.t(),
                    'de-prioritizes traffic of greedy non-school-related activities such as peer-to-peer file sharing'.t(),
                    'de-prioritizes non-real-time background traffic, to avoid slowing your internet connection when you need it most'.t()
                ],
                prioritizes: [
                    'interactive traffic and services (Remote Desktop, Email, DNS, SSH)'.t(),
                    'interactive web traffic'.t()
                ],
                deprioritizes: [
                    'web traffic to download sites'.t(),
                    'non-real-time background services (e.g. Microsoft&reg; updates, backup services)'.t(),
                    'any detected Peer-to-Peer traffic'.t(),
                    'all web traffic in violation of school policy'.t()
                ]
            },
            school_college: {
                desc: 'This initial configuration provides common settings suitable for most Community Colleges, Universities, and associated Campus Networks.'.t(),
                benefits: [
                    'optimizes internet responsiveness by prioritizing traffic of interactive services most commonly used in schools'.t(),
                    'prioritizes school-related traffic, such as traffic to Education sites or Search Engines'.t(),
                    'de-prioritizes traffic of greedy non-school-related activities, peer-to-peer file sharing, or BitTorrent'.t(),
                    'de-prioritizes non-real-time background traffic, to avoid slowing your internet connection when you need it most'.t()
                ],
                prioritizes: [
                    'interactive traffic and services (Remote Desktop, Email, DNS, SSH)'.t(),
                    'interactive web traffic'.t()
                ],
                deprioritizes: [
                    'web traffic to download sites'.t(),
                    'non-real-time background services (e.g. Microsoft&reg; updates, backup services)'.t(),
                    'any detected Peer-to-Peer traffic'.t(),
                    'all web traffic in violation of school policy'.t()
                ],
                limits: [
                    'heavily limits BitTorrent usage'.t()
                ]
            },
            business_government: {
                desc: 'This initial configuration provides common settings suitable for most governmental organizations.'.t(),
                benefits: [
                    'optimizes internet responsiveness by prioritizing traffic of interactive services most commonly used in government'.t(),
                    'de-prioritizes traffic of greedy non-work-related activities, such as peer-to-peer file sharing'.t(),
                    'de-prioritizes non-real-time background traffic, to avoid slowing your internet connection when you need it most'.t()
                ],
                prioritizes: [
                    'interactive traffic and services (Remote Desktop, Email, DNS, SSH)'.t(),
                    'interactive web traffic'.t()
                ],
                deprioritizes: [
                    'non-real-time background services (e.g. Microsoft&reg; updates, backup services)'.t(),
                    'any detected Peer-to-Peer traffic'.t(),
                    'all web traffic in violation of organization\'s policy'.t()
                ],
                limits: [
                    'heavily limits BitTorrent usage'.t()
                ]
            },
            business_nonprofit: {
                desc: 'This initial configuration provides common settings suitable for most charitable and not-for-profit organizations.'.t(),
                benefits: [
                    'optimizes internet responsiveness by prioritizing traffic of interactive services most commonly used in businesses'.t(),
                    'de-prioritizes traffic of greedy non-work-related activities, such as peer-to-peer file sharing'.t(),
                    'de-prioritizes non-real-time background traffic, to avoid slowing your internet connection when you need it most'.t(),
                    'saves money by controlling bandwidth'.t()
                ],
                prioritizes: [
                    'interactive traffic and services (Remote Desktop, Email, DNS, SSH)'.t(),
                    'interactive web traffic'.t()
                ],
                deprioritizes: [
                    'non-real-time background services (e.g. Microsoft&reg; updates, backup services)'.t(),
                    'any detected Peer-to-Peer traffic'.t(),
                    'all web traffic in violation of organization\'s policy'.t()
                ]
            },
            school_hotel: {
                desc: 'This initial configuration provides common settings suitable for most Hotel and Motels.'.t(),
                benefits: [
                    'optimizes internet responsiveness by prioritizing traffic of interactive services most commonly used by guests'.t(),
                    'web traffic to business-related sites (Search Engines, Finance, Business/Services, etc)'.t(),
                    'de-prioritizes traffic of peer-to-peer file sharing'.t(),
                    'de-prioritizes non-real-time background traffic, to avoid slowing your internet connection when it is needed most'.t()
                ],
                prioritizes: [
                    'interactive traffic and services (Remote Desktop, Email, DNS, SSH)'.t(),
                    'interactive web traffic'.t()
                ],
                deprioritizes: [
                    'web traffic to download sites'.t(),
                    'non-real-time background services (e.g. Microsoft&reg; updates, backup services)'.t(),
                    'any detected Peer-to-Peer traffic'.t(),
                    'all web traffic in violation of policy'.t()
                ],
                limits: [
                    'heavily limits BitTorrent usage'.t()
                ]
            },
            home: {
                desc: 'This initial configuration provides common settings suitable for most home users and households.'.t(),
                benefits: [
                    'optimizes internet responsiveness by prioritizing traffic of interactive services most commonly used in households'.t(),
                    'prioritizes internet radio and video so that these services run flawlessly while downloads are running'.t(),
                    'de-prioritizes non-real-time background traffic, to avoid slowing your internet connection when you need it most'.t()
                ],
                prioritizes: [
                    'interactive traffic and services (Remote Desktop, Email, DNS, SSH)'.t(),
                    'internet radio (e.g. Pandora&reg;,  Last.fm<small><sup>TM</sup></small>)'.t(),
                    'internet video (e.g. YouTube&reg;, Hulu<small><sup>TM</sup></small>, NetFlix&reg;)'.t(),
                    'interactive web traffic'.t()
                ],
                deprioritizes: [
                    'non-real-time background services (e.g. Microsoft&reg; updates, backup services)'.t(),
                    'all web traffic in violation of the household policy'.t()
                ]
            },
            metered: {
                desc: 'This initial configuration provides common settings suitable for organizations that pay variable rates for bandwidth.'.t() + '<br/><br/>' +
                    'For organizations that have to pay bandwidth rates proportional to bandwidth usage or have significant overage fees, this configuration maximizes bandwidth available to important interactive services, while minimizing bandwidth use for other less important or background tasks.'.t(),
                benefits: [
                    'optimizes internet responsiveness by prioritizing traffic of interactive services most commonly used in organizations'.t(),
                    'de-prioritizes traffic of non-work-related activities, such as gaming, peer-to-peer file sharing, or online videos'.t(),
                    'de-prioritizes non-real-time background traffic, to avoid slowing your internet connection when you need it most'.t(),
                    'saves money'.t()
                ],
                prioritizes: [
                    'interactive traffic and services (Remote Desktop, Email, DNS, SSH)'.t(),
                    'interactive web traffic'.t()
                ],
                deprioritizes: [
                    'any detected Peer-to-Peer traffic'.t(),
                    'all web traffic in violation of the organization\'s policy'.t()
                ],
                limits: [
                    'non-real-time background services (e.g. Microsoft&reg; updates, backup services)'.t(),
                    'all web traffic to Download Sites'.t(),
                    'heavily limits BitTorrent usage'.t()
                ]
            },
            custom: {
                desc: 'This is a basic configuration with no rules set by default.'.t() + '<br/><br/>' +
                    'This is a good option for users who wish to build their own rules configuration from scratch.'.t()
            }
        }

    },
});
