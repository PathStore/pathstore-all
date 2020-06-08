import React, {FunctionComponent, useCallback, useContext} from "react";
import Modal from "react-bootstrap/Modal";
import {Button} from "react-bootstrap";
import {
    ApplicationDeploymentModalContext,
    ApplicationDeploymentModalDataContext
} from "../../contexts/ApplicationDeploymentModalContext";
import {ApplicationDeploymentForm} from "./ApplicationDeploymentForm";
import {HypotheticalApplicationTopology} from "./HypotheticalApplicationTopology";

/**
 * TODO: Redo comments after change
 * @constructor
 */
export const ApplicationDeploymentModal: FunctionComponent = () => {

    // Load information from the data context to properly display the topology
    const {application, additions, updateAdditions, deletions, updateDeletions} = useContext(ApplicationDeploymentModalDataContext);

    // Callback function to reset the changes to the network
    const reset = useCallback(() => {
        if (updateAdditions && updateDeletions) {
            updateAdditions([]);
            updateDeletions([]);
        }
    }, [updateAdditions, updateDeletions]);

    // TODO: Finish
    const submitChanges = useCallback(() => {
        console.log(additions);
        console.log(deletions);
    }, [additions, deletions]);

    // load the modal information from the modal context
    const {visible, close} = useContext(ApplicationDeploymentModalContext);

    return (
        <Modal show={visible}
               size={"xl"}
               centered
        >
            <Modal.Header>
                <Modal.Title>Application Deployment for the keyspace {application?.keyspace_name}</Modal.Title>
            </Modal.Header>
            <Modal.Body>
                <HypotheticalApplicationTopology>
                    <Button onClick={reset}>Reset Changes</Button>
                </HypotheticalApplicationTopology>
                <ApplicationDeploymentForm/>
            </Modal.Body>
            <Modal.Footer>
                <Button onClick={submitChanges}>Submit Changes</Button>
                <Button onClick={close}>close</Button>
            </Modal.Footer>
        </Modal>
    );
};