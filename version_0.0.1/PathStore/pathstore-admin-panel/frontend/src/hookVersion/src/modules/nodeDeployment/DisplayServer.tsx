import React, {Component, FunctionComponent, ReactElement, useCallback, useContext, useEffect, useState} from "react";
import {Server} from "../../../../utilities/ApiDeclarations";
import {Table} from "react-bootstrap";
import {NodeDeploymentModalData} from "../../contexts/NodeDeploymentModalContext";
import {ModifyServerModalContext} from "../../contexts/ModifyServerModalContext";

/**
 * This component is used to display a list of servers to the node deployment modal.
 * You can click on each record to modify or delete it iff the record is not attached to a deployment record
 *
 * @constructor
 */
export const DisplayServers: FunctionComponent = () => {

    // load servers from the node deployment modal data
    const {deployment, servers, additions} = useContext(NodeDeploymentModalData);

    // Modify server reference to load the modal when a record is clicked
    const modifyServerModal = useContext(ModifyServerModalContext);

    // used to store the t body
    const [tbody, updateTbody] = useState<ReactElement[]>([]);

    /**
     * This callback function is used to render the modify server modal on click
     */
    const handleClick = useCallback((server: Server) => {
        if (deployment && additions)
            if (!new Set<string>(deployment.map(i => i.server_uuid).concat(additions.map(i => i.serverUUID))).has(server.server_uuid))
                if (modifyServerModal.show)
                    modifyServerModal.show(server)
    }, [deployment, additions, modifyServerModal]);

    /**
     * Everytime the servers update, update the t body
     */
    useEffect(() => {
        const temp = [];

        // load all rows
        if (servers) {
            for (let i = 0; i < servers.length; i++)
                temp.push(
                    <ServerRow key={i} server={servers[i]} handleClick={handleClick}/>
                );
            updateTbody(temp);
        }
    }, [servers, updateTbody, handleClick]);

    return (
        <div>
            <h2>Servers</h2>
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
            <p>Click on a free server record to modify it</p>
        </div>
    );
};

/**
 * Server Row properties
 */
interface ServerRowProps {
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
            <tr onClick={this.handleClick}>
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