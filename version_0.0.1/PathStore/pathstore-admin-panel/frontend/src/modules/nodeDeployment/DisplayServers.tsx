import React, {FunctionComponent, ReactElement, useCallback, useContext, useEffect, useState} from "react";
import {Table} from "react-bootstrap";
import {NodeDeploymentModalDataContext} from "../../contexts/NodeDeploymentModalContext";
import {ModifyServerModalContext} from "../../contexts/ModifyServerModalContext";
import {ObjectRow} from "../../utilities/ObjectRow";
import {Server} from "../../utilities/ApiDeclarations";

/**
 * This component is used to display a list of servers to the node deployment modal.
 * You can click on each record to modify or delete it iff the record is not attached to a deployment record
 *
 * @constructor
 */
export const DisplayServers: FunctionComponent = () => {

    // load servers from the node deployment modal data
    const {deployment, servers, additions} = useContext(NodeDeploymentModalDataContext);

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
            for (let [index, server] of servers.entries()) {
                temp.push(
                    <ObjectRow<Server> key={index} value={server} handleClick={handleClick}>
                        <td>{server.server_uuid}</td>
                        <td>{server.ip}</td>
                        <td>{server.username}</td>
                        <td>{server.ssh_port}</td>
                        <td>{server.rmi_port}</td>
                        <td>{server.name}</td>
                    </ObjectRow>
                );
            }
            updateTbody(temp);
        }
    }, [servers, updateTbody, handleClick]);

    return (
        <>
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
        </>
    );
};