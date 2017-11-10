Ext.define('Ung.reports.Theme', {
    alternateClassName: 'Theme',
    singleton: true,

    DEFAULT: {
        colors: ['#00b000', '#3030ff', '#009090', '#00ffff', '#707070', '#b000b0', '#fff000', '#b00000', '#ff0000', '#ff6347', '#c0c0c0'],
        chart: {
            backgroundColor: '#FFFFFF'
        },
        xAxis: {
            lineColor: '#C0D0E0',
            labels: {
                style: {
                    color: '#777'
                }
            }
        },
        yAxis: {
            lineColor: '#C0D0E0',
            gridLineColor: '#EEE',
            labels: {
                style: {
                    color: '#777'
                }
            },
            title: {
                style: {
                    color: '#777'
                }
            }
        },
        plotOptions: {
            series: {
                tooltip: {
                    headerFormat: '<span style="font-size: 14px; color: #333;">{point.key}</span><br/>'
                },
                dataLabels: {
                    color: '#555555'
                }
            },
            pie: {
                borderColor: '#FFF'
            }
        },
        tooltip: {
            backgroundColor: 'rgba(247, 247, 247, 0.95)',
            style: {
                color: '#333'
            }
        },
        legend: {
            title: {
                style: {
                    color: '#000'
                }
            },
            itemStyle: {
                color: '#333'
            },
            itemHoverStyle: {
                color: '#000'
            }
        },
        loading: {
            style: {
                backgroundColor: '#FFF',
                opacity: 0.5
            }
        }
    },

    DARK: {
        colors: ['#2b908f', '#90ee7e', '#f45b5b', '#7798BF', '#aaeeee', '#ff0066', '#eeaaee', '#55BF3B', '#DF5353', '#7798BF', '#aaeeee'],
        chart: {
            backgroundColor: {
                linearGradient: { x1: 0, y1: 1, x2: 0, y2: 1 },
                stops: [
                   [0, '#2a2a2b'],
                   [1, '#3e3e40']
                ]
            }
        },
        xAxis: {
            lineColor: '#707073',
            labels: {
                style: {
                    color: '#CCC'
                }
            }
        },
        yAxis: {
            lineColor: '#707073',
            gridLineColor: '#444',
            labels: {
                style: {
                    color: '#CCC'
                }
            },
            title: {
                style: {
                    color: '#CCC'
                }
            }
        },
        plotOptions: {
            series: {
                tooltip: {
                    headerFormat: '<span style="font-size: 14px; color: #FFF;">{point.key}</span><br/>'
                },
                dataLabels: {
                    color: '#B0B0B3'
                }
            },
            pie: {
                borderColor: '#333'
            }
        },
        tooltip: {
            backgroundColor: 'rgba(0, 0, 0, 0.85)',
            style: {
                color: '#FFF'
            }
        },
        legend: {
            title: {
                style: {
                    color: '#FFFFFF'
                }
            },
            itemStyle: {
                color: '#DDD'
            },
            itemHoverStyle: {
                color: '#FFF'
            }
        },
        loading: {
            style: {
                backgroundColor: '#555',
                opacity: 0.2
            }
        }
    },

    SAND: {
        colors: ['#f45b5b', '#8085e9', '#8d4654', '#7798BF', '#aaeeee', '#ff0066', '#eeaaee', '#55BF3B', '#DF5353', '#7798BF', '#aaeeee'],
        chart: {
            backgroundColor: null
        },
        xAxis: {
            lineColor: '#C0D0E0',
            labels: {
                style: {
                    color: '#777'
                }
            }
        },
        yAxis: {
            lineColor: '#C0D0E0',
            gridLineColor: '#DDD',
            labels: {
                style: {
                    color: '#777'
                }
            },
            title: {
                style: {
                    color: '#777'
                }
            }
        },
        plotOptions: {
            series: {
                tooltip: {
                    headerFormat: '<span style="font-size: 14px; color: #555;">{point.key}</span><br/>'
                },
                dataLabels: {
                    color: '#555'
                }
            },
            pie: {
                borderColor: '#E2E2E2'
            }
        },
        tooltip: {
            backgroundColor: 'rgba(247, 247, 247, 0.95)',
            style: {
                color: '#333'
            }
        },
        legend: {
            title: {
                style: {
                    color: '#333'
                }
            },
            itemStyle: {
                color: '#333'
            },
            itemHoverStyle: {
                color: '#000'
            }
        },
        loading: {
            style: {
                backgroundColor: '#FFF',
                opacity: 0.5
            }
        }
    }
});
