import React, {Component} from "react";
import Table from "react-bootstrap/Table";

export default class DisplayServers extends Component {

    constructor(props) {
        super(props);

        this.state = {
            servers: []
        }

    }

    componentDidMount() {
        fetch('/api/v1/servers')
            .then(response => response.json())
            .then(response => this.setState({servers: this.parseServers(response)}))
    }

    parseServers = (servers) => {
        let list = [];

        for (let i = 0; i < servers.length; i++)
            list.push({server_uuid: servers[i].server_uuid, ip: servers[i].ip});

        return list;
    };

    render() {

        const tbody = [];

        for (let i = 0; i < this.state.servers.length; i++)
            tbody.push(
                <tr key={i}>
                    <td>{this.state.servers[i].server_uuid}</td>
                    <td>{this.state.servers[i].ip}</td>
                </tr>
            );

        return (
            <Table>
                <thead>
                <tr>
                    <th>Server UUID</th>
                    <th>IP</th>
                </tr>
                </thead>
                <tbod>
                    {tbody}
                </tbod>
            </Table>
        );
    }
}