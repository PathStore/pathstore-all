import React, {FunctionComponent, useCallback, useContext} from "react";
import Modal from "react-bootstrap/Modal";
import Button from "react-bootstrap/Button";
import {NodeDeploymentModalContext, NodeDeploymentModalDataContext} from "../../contexts/NodeDeploymentModalContext";
import {Deployment, DEPLOYMENT_STATE, DeploymentUpdate} from "../../utilities/ApiDeclarations";
import {NodeDeploymentAdditionForm} from "./NodeDeploymentAdditionForm";
import {DisplayServers} from "./DisplayServers";
import {AddServers} from "./AddServers";
import {LoadingModalContext, LoadingModalProvider} from "../../contexts/LoadingModalContext";
import {ErrorModalContext, ErrorModalProvider} from "../../contexts/ErrorModalContext";
import {ServerCreationResponseModalProvider} from "../../contexts/ServerCreationResponseModalContext";
import {HypotheticalDeploymentInfoModalProvider} from "../../contexts/HypotheticalDeploymentInfoModalContext";
import {APIContext} from "../../contexts/APIContext";
import {ModifyServerModalProvider} from "../../contexts/ModifyServerModalContext";
import {SubmissionErrorModalContext, SubmissionErrorModalProvider} from "../../contexts/SubmissionErrorModalContext";
import {HypotheticalDeploymentTopology} from "./HypotheticalDeploymentTopology";
import {webHandler} from "../../utilities/Utils";
import {ConfirmationButton} from "../confirmation/ConfirmationButton";

/**
 * The component is used to allow the user to modify the network in a workbench environment. Either adding additional servers,
 * adding new hypothetical nodes to the network, or hypothetically deleting nodes.
 *
 * Once the user is okay with their changes they can submit their changes into the queue and it will close the modal
 *
 * @constructor
 */
export const NodeDeploymentModal: FunctionComponent = () => {

    // de reference force refresh to use submission
    const {forceRefresh} = useContext(APIContext);

    // loading modal reference
    const loadingModal = useContext(LoadingModalContext);

    // error modal reference
    const errorModal = useContext(ErrorModalContext);

    // submission error modal context
    const submissionErrorModal = useContext(SubmissionErrorModalContext);

    // NodeDeployment Context
    const {visible, close} = useContext(NodeDeploymentModalContext);

    // Shared data between node deployment components
    const {additions, updateAdditions, deletions, updateDeletions} = useContext(NodeDeploymentModalDataContext);

    /**
     * This callback is called when the user clicks the reset button. This function wipes all non-submitted data
     */
    const resetChanges = useCallback(() => {
        if (updateAdditions && updateDeletions) {
            updateAdditions([]);
            updateDeletions([]);
        }
    }, [updateAdditions, updateDeletions]);

    /**
     * This callback is called when the user requests to submit their changes
     */
    const submit = useCallback(() => {
        if (additions && deletions && loadingModal.show && loadingModal.close && errorModal.show && submissionErrorModal.show) {
            if (additions.length <= 0 && deletions.length <= 0) {
                submissionErrorModal.show("You have not made any changes to the network");
                return;
            }

            loadingModal.show();
            if (deletions.length > 0)
                fetch('/api/v1/deployment', {
                    method: 'DELETE',
                    headers: {
                        'Accept': 'application/json',
                        'Content-Type': 'application/json',
                    },
                    body: JSON.stringify({
                        records: deletions
                    })
                })
                    .then(webHandler)
                    .then(() => {

                        if (additions.length === 0)
                            resetChanges();

                        if (forceRefresh && close) {
                            forceRefresh();
                            close();
                        }
                    })
                    .catch(errorModal.show)
                    .finally(loadingModal.close);

            if (additions.length > 0)
                fetch('/api/v1/deployment', {
                    method: 'POST',
                    headers: {
                        'Accept': 'application/json',
                        'Content-Type': 'application/json',
                    },
                    body: JSON.stringify({
                        records: additions
                    })
                })
                    .then(webHandler)
                    .then(() => {
                        resetChanges();

                        if (forceRefresh && close) {
                            forceRefresh();
                            close();
                        }
                    })
                    .catch(errorModal.show)
                    .finally(loadingModal.close);
        }
    }, [additions, deletions, forceRefresh, loadingModal, errorModal.show, close, submissionErrorModal, resetChanges]);

    return (
        <Modal show={visible}
               size={"xl"}
               centered>
            <Modal.Header>
                <Modal.Title>Node Deployment</Modal.Title>
            </Modal.Header>
            <Modal.Body>
                {/* Topology of the hypothetical network */}
                <HypotheticalDeploymentInfoModalProvider>
                    <HypotheticalDeploymentTopology>
                        <Button onClick={resetChanges}>Reset to default</Button>
                    </HypotheticalDeploymentTopology>
                </HypotheticalDeploymentInfoModalProvider>
                {/* Addition form, allows users to make changes to the hypothetical network */}
                <SubmissionErrorModalProvider>
                    <NodeDeploymentAdditionForm/>
                </SubmissionErrorModalProvider>
                <hr/>
                {/* Display all servers to the user */}
                <ModifyServerModalProvider>
                    <DisplayServers/>
                </ModifyServerModalProvider>
                <hr/>
                {/* Allows the user to add servers to the network */}
                <LoadingModalProvider>
                    <ErrorModalProvider>
                        <ServerCreationResponseModalProvider>
                            <AddServers/>
                        </ServerCreationResponseModalProvider>
                    </ErrorModalProvider>
                </LoadingModalProvider>
            </Modal.Body>
            <Modal.Footer>
                <ConfirmationButton message={"Are you sure you want to submit your node deployment changes?"}
                                    onClick={submit}>
                    Submit Changes
                </ConfirmationButton>
                <Button onClick={close}>Close</Button>
            </Modal.Footer>
        </Modal>
    );
};

/**
 * This function is used to create a combined set of deployment objects given the api based deployment and the set
 * of addition objects
 *
 * @param deployment deployment objects from api
 * @param additions addition objects generated by the user {@link NodeDeploymentAdditionForm}
 */
export const getDeploymentObjects = (deployment: Deployment[] | undefined, additions: DeploymentUpdate[] | undefined): Deployment[] => {
    if (!deployment || !additions) return [];

    return deployment
        .concat(additions.map(i => {
            return {
                new_node_id: i.newNodeId,
                parent_node_id: i.parentId,
                server_uuid: i.serverUUID,
                process_status: DEPLOYMENT_STATE[DEPLOYMENT_STATE.WAITING_DEPLOYMENT]
            }
        }));

};