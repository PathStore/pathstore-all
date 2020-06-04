import React, {FunctionComponent, useCallback, useContext, useEffect} from "react";
import Modal from "react-bootstrap/Modal";
import Button from "react-bootstrap/Button";
import {AlignedDivs, Left, Right} from "../../utilities/AlignedDivs";
import {NodeDeploymentModalContext, NodeDeploymentModalData} from "../../contexts/NodeDeploymentModalContext";
import {Deployment, Update} from "../../utilities/ApiDeclarations";
import {PathStoreTopology} from "../../../../modules/PathStoreTopology";
import {NodeDeploymentAdditionForm} from "./NodeDeploymentAdditionForm";
import {DisplayServers} from "./DisplayServer";
import {AddServers} from "./AddServers";
import {LoadingModalContext, LoadingModalProvider} from "../../contexts/LoadingModalContext";
import {ErrorModalContext, ErrorModalProvider} from "../../contexts/ErrorModalContext";
import {ServerCreationResponseModalProvider} from "../../contexts/ServerCreationResponseModalContext";
import {HypotheticalInfoModalContext} from "../../contexts/HypotheticalInfoModalContext";
import {webHandler} from "../../../../utilities/Utils";
import {APIContext} from "../../contexts/APIContext";
import {ModifyServerModalProvider} from "../../contexts/ModifyServerModalContext";
import {SubmissionErrorModalContext, SubmissionErrorModalProvider} from "../../contexts/SubmissionErrorModalContext";

/**
 * TODO: Handle case where deployment objects change and there are conflicting addition records
 *
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

    // Allows the hypothetical info modal to be used
    const hypotheticalInfoModal = useContext(HypotheticalInfoModalContext);

    // NodeDeployment Context
    const nodeDeploymentModalContext = useContext(NodeDeploymentModalContext);

    // Shared data between node deployment components
    const {deployment, additions, updateAdditions, deletions, updateDeletions, additionNodeIdSet, updateAdditionNodeIdSet, deletionNodeIdSet, updateDeletionNodeIdSet} = useContext(NodeDeploymentModalData);

    /**
     * Everytime the additions and or the deletions updates the addition node id and deletion node id sets are updated
     */
    useEffect(() => {
        if (updateAdditionNodeIdSet && updateDeletionNodeIdSet && additions && deletions) {
            updateAdditionNodeIdSet(new Set<number>(additions.map(i => i.newNodeId)));
            updateDeletionNodeIdSet(new Set<number>(deletions.map(i => i.newNodeId)));
        }
    }, [additions, deletions, updateAdditionNodeIdSet, updateDeletionNodeIdSet]);

    /**
     * Graph colour function.
     *
     * If the node is in the addition set then the node is cyan
     * If the node is in the deletion set then the node is red
     * Else the node is grey
     */
    const getColour = useCallback((object: Deployment): string => {
        if (!additionNodeIdSet || !deletionNodeIdSet) return 'not_set_node';
        else {
            return additionNodeIdSet.has(object.new_node_id) ?
                'hypothetical'
                :
                deletionNodeIdSet.has(object.new_node_id) ?
                    'uninstalled_node'
                    :
                    object.process_status === "DEPLOYED" ?
                        'not_set_node'
                        : 'waiting_node';
        }
    }, [additionNodeIdSet, deletionNodeIdSet]);

    /**
     * If any node is clicked render a hypothetical info modal with their information
     */
    const handleClick = useCallback((event: any, node: number): void => {
        if (hypotheticalInfoModal.show && additionNodeIdSet)
            hypotheticalInfoModal.show({
                node: node,
                isHypothetical: additionNodeIdSet.has(node)
            });
    }, [additionNodeIdSet, hypotheticalInfoModal]);

    /**
     * This callback is called when the user clicks the reset button. This function wipes all non-submitted data
     */
    const resetChanges = useCallback(() => {
        if (updateAdditionNodeIdSet && updateDeletionNodeIdSet && updateAdditions && updateDeletions) {
            updateAdditionNodeIdSet(new Set<number>());
            updateDeletionNodeIdSet(new Set<number>());
            updateAdditions([]);
            updateDeletions([]);
        }
    }, [updateAdditionNodeIdSet, updateDeletionNodeIdSet, updateAdditions, updateDeletions]);

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
                        if (forceRefresh && nodeDeploymentModalContext.close) {
                            forceRefresh();
                            nodeDeploymentModalContext.close();
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
                        if (forceRefresh && nodeDeploymentModalContext.close) {
                            forceRefresh();
                            nodeDeploymentModalContext.close();
                        }
                    })
                    .catch(errorModal.show)
                    .finally(loadingModal.close);
        }
    }, [additions, deletions, forceRefresh, loadingModal, errorModal.show, nodeDeploymentModalContext, submissionErrorModal]);

    return (
        <Modal show={nodeDeploymentModalContext.visible}
               size={"xl"}
               centered>
            <Modal.Header>
                <Modal.Title>Node Deployment</Modal.Title>
            </Modal.Header>
            <Modal.Body>
                <AlignedDivs>
                    <Left width='35%'>
                        <h2>Topology Legend</h2>
                        <br/>
                        <p>Deployed Node: <span className={'d_currentLine'}>Light Grey</span></p>
                        <p>Hypothetical Node: <span className={'d_cyan'}>Cyan</span></p>
                        <p>Hypothetical Deletion: <span className={'d_red'}>Red</span></p>
                    </Left>
                    <Right>
                        <h2>Hypothetical Topology</h2>
                        <PathStoreTopology width={700}
                                           height={500}
                                           deployment={getDeploymentObjects(deployment, additions)}
                                           get_colour={getColour}
                                           get_click={handleClick}
                        />
                        <Button onClick={resetChanges}>Reset to default</Button>
                    </Right>
                </AlignedDivs>
                <SubmissionErrorModalProvider>
                    <NodeDeploymentAdditionForm/>
                </SubmissionErrorModalProvider>
                <hr/>
                <ModifyServerModalProvider>
                    <DisplayServers/>
                </ModifyServerModalProvider>
                <hr/>
                <LoadingModalProvider>
                    <ErrorModalProvider>
                        <ServerCreationResponseModalProvider>
                            <AddServers/>
                        </ServerCreationResponseModalProvider>
                    </ErrorModalProvider>
                </LoadingModalProvider>
            </Modal.Body>
            <Modal.Footer>
                <Button onClick={submit}>Submit changes</Button>
                <Button onClick={nodeDeploymentModalContext.close}>Close</Button>
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
export const getDeploymentObjects = (deployment: Deployment[] | undefined, additions: Update[] | undefined): Deployment[] => {
    if (!deployment || !additions) return [];

    return deployment
        .concat(additions.map(i => {
            return {
                new_node_id: i.newNodeId,
                parent_node_id: i.parentId,
                server_uuid: i.serverUUID,
                process_status: "WAITING_INSTALL"
            }
        }));

};