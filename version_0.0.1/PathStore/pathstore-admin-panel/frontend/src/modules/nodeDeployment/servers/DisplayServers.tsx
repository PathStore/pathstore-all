import {Server} from "../../../utilities/ApiDeclarations";
import React, {Component} from "react";
import {Table} from "react-bootstrap";
import {webHandler} from "../../../utilities/Utils";

/**
 * Properties for {@link DisplayServers}
 */
interface DisplayServersProperties {
    /**
     * List of server objects from api
     */
    readonly servers: Server[]

    /**
     * Callback function to refresh all component props
     */
    readonly forceRefresh: () => void
}

/**
 * This component is used to render a table to display all created servers
 *
 * @param props
 * @constructor
 */
export default class DisplayServers extends Component<DisplayServersProperties> {

    /**
     * Delete object using the api. Then force refresh the data so the server is removed on the client side
     *
     * @param server
     */
    handleClick = (server: Server): void => {
        fetch('/api/v1/servers?serverUUID=' + server.server_uuid, {
            method: 'DELETE'
        })
            .then(webHandler)
            .then(() => {
                this.props.forceRefresh();
            })
            .catch(response => {
                alert(JSON.stringify(response));
            })
    };

    /**
     * Render table
     */
    render() {
        const tbody = [];

        for (let i = 0; i < this.props.servers.length; i++)
            tbody.push(
                <ServerRow key={i} server={this.props.servers[i]} handleClick={this.handleClick}/>
            );

        return (
            <Table>
                <thead>
                <tr>
                    <th>Server UUID</th>
                    <th>IP</th>
                    <th>Username</th>
                    <th>SSH Port</th>
                    <th>RMI Port</th>
                    <th>Server Name</th>
                </tr>
                </thead>
                <tbody>
                {tbody}
                </tbody>
            </Table>
        );
    }
};

/**
 * Server Row properties
 */
interface ServerRowProps {
    /**
     * Key to render with tr statement
     */
    readonly key: number

    /**
     * Server object to display and to return on click
     */
    readonly server: Server;

    /**
     * What function to call on click
     */
    readonly handleClick: (server: Server) => void
}

/**
 * This component is used to render a given row in the servers display table. It is also used to all for the on click
 * function to have a server object as the property so that there doesn't need to be additional parsing of data
 */
class ServerRow extends Component<ServerRowProps> {

    /**
     * Call given on click function and pass server object
     */
    handleClick = () => this.props.handleClick(this.props.server);

    /**
     * Render the table record
     */
    render() {
        return (
            <tr key={this.props.key} onClick={this.handleClick}>
                <td>{this.props.server.server_uuid}</td>
                <td>{this.props.server.ip}</td>
                <td>{this.props.server.username}</td>
                <td>{this.props.server.ssh_port}</td>
                <td>{this.props.server.rmi_port}</td>
                <td>{this.props.server.name}</td>
            </tr>
        );
    }
}