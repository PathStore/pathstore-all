import React, {FunctionComponent, useContext} from "react";
import {Table} from "react-bootstrap";
import {APIContext} from "../../contexts/APIContext";
import {NodeInfoModalContext} from "../../contexts/NodeInfoModalContext";

/**
 * This component is used to render a table to inform the user the current application statues for a desired set of nodes.
 *
 * In practice this is used in the Node Info Modal where the status information in the props is filter based on the node
 * id of the node info modal displayed
 *
 * @constructor
 */
export const ApplicationStatusViewer: FunctionComponent = () => {

    const {data} = useContext(NodeInfoModalContext);

    let {applicationStatus} = useContext(APIContext);

    // If there are no application status's in the dataset inform the user about this instead of rendering an empty table
    if (!data || !applicationStatus || applicationStatus.length === 0)
        return (
            <div>
                <h2>Application Status Viewer</h2>
                <p>There are no application status records</p>
            </div>
        );

    let body = [];

    // table body from the given data set
    for (let i = 0; i < applicationStatus.filter(i => i.node_id === data.node).length; i++) {

        let currentObject = applicationStatus[i];

        body.push(
            <tr>
                <td>{currentObject.node_id}</td>
                <td>{currentObject.keyspace_name}</td>
                <td>{currentObject.process_status}</td>
                <td>{currentObject.wait_for}</td>
            </tr>
        );
    }

    return (
        <div>
            <h2>Application Status Viewer</h2>
            <Table>
                <thead>
                <tr>
                    <th>Nodeid</th>
                    <th>Application</th>
                    <th>Status</th>
                    <th>Waiting</th>
                </tr>
                </thead>
                <tbody>
                {body}
                </tbody>
            </Table>
        </div>
    );
};