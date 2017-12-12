
'use strict';

// tag::vars[]
const React = require('react');
const ReactDOM = require('react-dom');
const $ = require("jquery");
const Highcharts = require('highcharts');
const addHeatmap = require('highcharts/modules/heatmap');

// end::vars[]

// tag::app[]
class App extends React.Component {

    constructor(props) {
        super(props);
        this.state = {protein: {sequence: []}};
    }

    componentDidMount() {
        const self = this;
        $.ajax({
            url: "/api/proteins/" + accession
        }).then(function (data) {
            self.setState({protein: data});
        });
    }

    render() {
        addHeatmap(Highcharts);
        // const options = {
        //     title: {
        //         text: 'Fruit Consumption',
        //     },
        //     xAxis: {
        //         categories: [
        //             'Apples',
        //             'Bananas',
        //             'Oranges',
        //             'Pineapples',
        //             'Blueberries',
        //         ],
        //     },
        //     yAxis: {
        //         title: {
        //             text: 'Fruit eaten',
        //         },
        //     },
        //     chart: {
        //         type: 'line',
        //     },
        //     series: [
        //         {
        //             name: 'Jane',
        //             data: [1, 0, 4, 0, 3],
        //         },
        //         {
        //             name: 'John',
        //             data: [5, 7, 3, 2, 4],
        //         },
        //         {
        //             name: 'Doe',
        //             data: [0, 0, 0, 1, 0],
        //         },
        //     ],
        // };
        let data = [];
        let categories = [];
        this.state.protein.sequence.forEach(function(base, x) {
            // if (x < 100) {
            categories.push(base.reference);
                base.pst.forEach(function (val, y) {
                    // if (y < 5) {
                        data.push([x, y, val]);
                    // }
                });
            // }
        });

        console.log(data);
        if (data.length === 0 ) {
            return null;
        }

        const options = {

            chart: {
                type: 'heatmap',
                // marginTop: 40,
                // marginBottom: 80,
                plotBorderWidth: 1
            },

            boost: {
                useGPUTranslations: true
            },


            title: {
                text: accession
            },

            xAxis: {
                categories: categories,
                minPadding: 0,
                maxPadding: 0,
                startOnTick: true,
                endOnTick: true,
                tickWidth: 1,
                tickInterval: 1,
                padding: 1,
                labels: {
                    staggerLines: 3,
                    style: {
                        fontSize: '8px',
                    },
                },
                step: 1,
                // tickPositions: [0, 6, 12, 18, 24],
            },

            yAxis: {
                // categories: ['Monday', 'Tuesday', 'Wednesday', 'Thursday', 'Friday'],
                title: null,
                startOnTick: true,
                endOnTick: false,
                min: 0,
                max:19,
            },

            colorAxis: {
                min: 0,
                max: 1,
                stops: [[0, '#ffffcc'],
                [0.7, '#a1dab4'],
                [0.8, '#41b6c4'],
                [0.9, '#2c7fb8'],
                [0.95, '#253494']],
                // minColor: '#FFFFFF',
                // maxColor: '#000099' //Highcharts.getOptions().colors[0]
            },

            legend: {
                align: 'right',
                layout: 'vertical',
                margin: 0,
                verticalAlign: 'top',
                y: 29,
                symbolHeight: 304
            },

            tooltip: {
                // formatter: function () {
                //     return '<b>' + this.series.xAxis.categories[this.point.x] + '</b> sold <br><b>' +
                //         this.point.value + '</b> items on <br><b>' + this.series.yAxis.categories[this.point.y] + '</b>';
                // }
            },

            series: [{
                boostThreshold: 100,
                name: 'PolyST',
                borderWidth: 0,
                data: data, //[[0, 0, 10], [0, 1, 19], [0, 2, 8], [0, 3, 24], [0, 4, 67], [1, 0, 92], [1, 1, 58], [1, 2, 78], [1, 3, 117], [1, 4, 48], [2, 0, 35], [2, 1, 15], [2, 2, 123], [2, 3, 64], [2, 4, 52], [3, 0, 72], [3, 1, 132], [3, 2, 114], [3, 3, 19], [3, 4, 16], [4, 0, 38], [4, 1, 5], [4, 2, 8], [4, 3, 117], [4, 4, 115], [5, 0, 88], [5, 1, 32], [5, 2, 12], [5, 3, 6], [5, 4, 120], [6, 0, 13], [6, 1, 44], [6, 2, 88], [6, 3, 98], [6, 4, 96], [7, 0, 31], [7, 1, 1], [7, 2, 82], [7, 3, 32], [7, 4, 30], [8, 0, 85], [8, 1, 97], [8, 2, 123], [8, 3, 64], [8, 4, 84], [9, 0, 47], [9, 1, 114], [9, 2, 31], [9, 3, 48], [9, 4, 91]],
                dataLabels: {
                    enabled: false,
                    color: '#000000'
                },
                turboThreshold: 100000,
            }]

        };

        return (
            <div className="App">
                <Chart options={options} />
            </div>
        );
    }
}
// end::app[]

class HeatMap extends React.Component {
    componentDidMount() {
        this.chart = new Highcharts[this.props.type || 'Chart'](
            this.chartEl,
            this.props.options
        );
    }

    componentWillUnmount() {
        this.chart.destroy();
    }

    render() {
        return <div ref={el => (this.chartEl = el)} />;
    }
}

class Chart extends React.Component {
    componentDidMount() {
        this.chart = new Highcharts[this.props.type || 'Chart'](
            this.chartEl,
            this.props.options
        );
    }

    componentWillUnmount() {
        this.chart.destroy();
    }

    render() {
        return <div ref={el => (this.chartEl = el)} />;
    }
}

// tag::render[]
ReactDOM.render(
    <App />,
    document.getElementById('react')
)
// end::render[]