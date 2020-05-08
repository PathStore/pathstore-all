import React, {Component} from "react";
import Table from "react-bootstrap/Table";

/**
 * This component is used to render a table to display all created servers
 *
 * Props:
 * servers: list of server objects from api
 */
export default class DisplayServers extends Component {

    /**
     * Create table body and render table with appropriate headers
     *
     * @returns {*}
     */
    render() {
        const tbody = [];

        for (let i = 0; i < this.props.servers.length; i++)
            tbody.push(
                <tr key={i}>
                    <td>{this.props.servers[i].server_uuid}</td>
                    <td>{this.props.servers[i].ip}</td>
                    <td>{this.props.servers[i].name}</td>
                </tr>
            );

        return (
            <Table>
                <thead>
                <tr>
                    <th>Server UUID</th>
                    <th>IP</th>
                    <th>Server Name</th>
                </tr>
                </thead>
                <tbody>
                {tbody}
                </tbody>
            </Table>
        );
    }
}