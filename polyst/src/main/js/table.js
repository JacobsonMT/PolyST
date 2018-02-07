'use strict';

// tag::vars[]
const React = require('react');
const ReactDOM = require('react-dom');
const $ = require("jquery");
import ReactTable from "react-table";
import "react-table/react-table.css";
// end::vars[]

// tag::app[]
class App extends React.Component {

    constructor(props) {
        super(props);
        this.state = {
            loading: true,
            data: []
        };
    }

    componentDidMount() {
        const self = this;
        $.ajax({
            url: "/api/proteins"
        }).then(function (data) {
            self.setState({
                data: data,
                loading: false
            });
        });
    }

    render() {
        if (this.state.data.length === 0) {
            return null;
        }
        let data = this.state.data;

        const columns = [
            {
                Header: "Accession",
                accessor: "accession",
                filterMethod: (filter, row) => row[filter.id].toLowerCase().includes(filter.value.toLowerCase()),
                Cell: row => (
                    <div style={{display: 'flex', flexWrap: 'wrap', alignItems: 'center', justifyContent: 'center'}}>
                        <a href={"//www.uniprot.org/uniprot/" + row.value} target="_blank" style={{margin: '0 1em'}}>
                            <img
                                src="/uniprot_rgb_320x146.png"
                                style={{width: `40px`, margin: "0", verticalAlign: 'middle'}}
                            />
                        </a>
                        {row.value}
                    </div>
                ),
                // maxWidth: 500,
            },
            {
                Header: "Size",
                accessor: "size",
                filterMethod: (filter, row) => row[filter.id] <= filter.value,
                Cell: row => (
                    <div style={{display: 'flex', flexWrap: 'wrap', alignItems: 'center', justifyContent: 'center'}}>
                        {row.value}
                    </div>
                ),
                // maxWidth: 300,
            },
            {
                Header: "Results",
                sortable: false,
                filterable: false,
                Cell: cellInfo => (
                    <div style={{display: 'flex', flexWrap: 'wrap', alignItems: 'center', justifyContent: 'center'}}>
                        <a href={"/proteins/" + cellInfo.row.accession} target="_blank">Matrix</a>
                        <a href={"/api/proteins/" + cellInfo.row.accession + "/download"} style={{margin: '0 1em'}}>Download</a>
                    </div>
                ),
                // maxWidth: 400
            }
        ];


        return (
                <ReactTable
                    filterable
                    resizable
                    data={data}
                    columns={columns}
                    defaultPageSize={10}
                    className="-striped -highlight"
                />
        );
    }
}
// end::app[]

class ProteinForm extends React.Component {
    constructor(props) {
        super(props);
        this.state = {
            value: ''
        };

        this.handleChange = this.handleChange.bind(this);
        this.handleSubmit = this.handleSubmit.bind(this);
    }

    handleChange(event) {
        this.setState({value: event.target.value});
    }

    handleSubmit(event) {
        let proteinRequests = [];
        let lines = this.state.value.split('\n');
        let trimmed = [];
        for(let i = 0;i < lines.length;i++){
            let l = $.trim(lines[i]).split(":");
            if (l.length > 1) {
                proteinRequests.push({"accession": l[0], "location": parseInt(l[1]), "ref": l[2], "alt": l[3]});
                trimmed.push(l);
            }
        }

        const self = this;
        $.ajax( {
            cache : false,
            type : 'POST',
            url : '/api/proteins',
            data : JSON.stringify(proteinRequests),
            contentType : "application/json"
        } ).then(function (data) {
            let output = [];
            trimmed.forEach(function(l, k) {
                output.push(l.join(":") + "\t" + data[k]);
            });
            self.setState({output: output.join("\n")});
        });
        event.preventDefault();
    }

    render() {
        return (
            <form onSubmit={this.handleSubmit}>
                <textarea className="input" value={this.state.value} onChange={this.handleChange}
                          placeholder="Example Input:                                         Q8WVH0:112:D:G                                         P53365:312:N:K                                         Q9ULP0:254:T:V                                         Q8NHW4:15:A:M                                         Q96EW2:436:I:Y"/>
                <input type="submit" value=">>" />
                <textarea className="output" value={this.state.output} placeholder="Output Predictions" disabled/>
            </form>
        );
    }
}

// tag::render[]
ReactDOM.render(
    <ProteinForm />,
    document.getElementById('protein-form')
);
ReactDOM.render(
    <App />,
    document.getElementById('react-table')
);
// end::render[]