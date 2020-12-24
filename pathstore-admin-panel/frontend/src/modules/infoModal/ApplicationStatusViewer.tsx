import React, {FunctionComponent, ReactElement, useCallback, useContext, useEffect, useState} from "react";
import {Table} from "react-bootstrap";
import {APIContext} from "../../contexts/APIContext";
import {NodeInfoModalContext} from "../../contexts/NodeInfoModalContext";
import {useReducedState} from "../../hooks/useReducedState";
import {ApplicationStatus} from "../../utilities/ApiDeclarations";

/**
 * This component is used to render a table to inform the user the current application statues for a desired set of nodes.
 *
 * In practice this is used in the Node Info Modal where the status information in the props is filter based on the node
 * id of the node info modal displayed
 *
 * @constructor
 */
export const ApplicationStatusViewer: FunctionComponent = () => {

    // load the passed data from the node info modal context
    const {data} = useContext(NodeInfoModalContext);

    // load application Status objects from the api
    const {applicationStatus} = useContext(APIContext);

    // Filter function to filter the application Status based on the passed node id
    const filterApplicationStatus = useCallback(
        (applicationStatus: ApplicationStatus) => applicationStatus.node_id === data,
        [data]);

    // Reduce the application status info based on the node id of each record
    const reducedApplicationStatus = useReducedState<ApplicationStatus>(applicationStatus, filterApplicationStatus);

    // Store the value to be displayed in the internal state to not reload every tick.
    const [table, setTable] = useState<ReactElement | null>(null);

    /**
     * This function is used to set the table value everytime data or the reduced application status list changes.
     *
     * If the reduced application status has a length of 0 then we inform the user that there are no applications installed
     * on the given node. Else we display a table to inform them of what applications are installed or what their current
     * process status is.
     *
     */
    useEffect(() => {
        let value: ReactElement;

        if (!data || !reducedApplicationStatus || reducedApplicationStatus.length === 0)
            value = (
                <p>There are no application status records</p>
            );
        else {
            let tBody = [];

            // table body from the given data set
            for (let [index, application] of reducedApplicationStatus.entries()) {

                tBody.push(
                    <tr key={index}>
                        <td>{application.node_id}</td>
                        <td>{application.keyspace_name}</td>
                        <td>{application.process_status}</td>
                        <td>{application.wait_for}</td>
                    </tr>
                );
            }

            value = (
                <Table>
                    <thead>
                    <tr>
                        <th>Node Id</th>
                        <th>Application</th>
                        <th>Status</th>
                        <th>Waiting</th>
                    </tr>
                    </thead>
                    <tbody>
                    {tBody}
                    </tbody>
                </Table>
            );
        }

        setTable(value);

    }, [data, reducedApplicationStatus, setTable]);

    return (
        <>
            <h2>Application Status Viewer</h2>
            {table}
        </>
    );
};