import {Server} from "../../../utilities/ApiDeclarations";
import React, {FunctionComponent} from "react";
import {Table} from "react-bootstrap";

/**
 * Properties for {@link DisplayServers}
 */
interface DisplayServersProperties {
    /**
     * List of server objects from api
     */
    readonly servers: Server[]
}

/**
 * This component is used to render a table to display all created servers
 *
 * @param props
 * @constructor
 */
export const DisplayServers: FunctionComponent<DisplayServersProperties> = (props) => {
    const tbody = [];

    for (let i = 0; i < props.servers.length; i++)
        tbody.push(
            <tr key={i}>
                <td>{props.servers[i].server_uuid}</td>
                <td>{props.servers[i].ip}</td>
                <td>{props.servers[i].username}</td>
                <td>{props.servers[i].ssh_port}</td>
                <td>{props.servers[i].rmi_port}</td>
                <td>{props.servers[i].name}</td>
            </tr>
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
};