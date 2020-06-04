import React, {FunctionComponent, ReactElement, useCallback, useContext} from "react";
import {NodeInfoModalContext} from "../../contexts/NodeInfoModalContext";
import Modal from "react-bootstrap/Modal";
import Button from "react-bootstrap/Button";
import {ServerInfo} from "../modalShared/ServerInfo";
import {ApplicationStatusViewer} from "./ApplicationStatusViewer";
import {LogViewer} from "./LogViewer";
import {Deployment, Update} from "../../../../utilities/ApiDeclarations";
import {webHandler} from "../../../../utilities/Utils";
import {ErrorModalContext} from "../../contexts/ErrorModalContext";
import {LoadingModalContext} from "../../contexts/LoadingModalContext";

/**
 * This component is used to display information in the main viewtopology about a given node.
 *
 * The user can see the currently applications installed, them can see the server info and they can request
 * logs from the server based on day and log level
 * @constructor
 */
export const NodeInfoModal: FunctionComponent = () => {
    // declare loading modal usage
    const loadingModal = useContext(LoadingModalContext);

    // declare error modal usage
    const errorModal = useContext(ErrorModalContext);

    // dereference internal modal information
    const {visible, data, close} = useContext(NodeInfoModalContext);

    /**
     * This function is used to submit a retry request to the api when a node has failed.
     */
    const retryOnClick = useCallback(() => {
        if (loadingModal && loadingModal.show && loadingModal.close) {
            loadingModal.show();
            fetch('/api/v1/deployment', {
                method: 'PUT',
                headers: {
                    'Accept': 'application/json',
                    'Content-Type': 'application/json',
                },
                body: JSON.stringify({record: retryData(data?.deployment, data?.node)})
            })
                .then(webHandler)
                .then(() => {
                    if (close)
                        close();
                    if (data?.forceRefresh)
                        data?.forceRefresh()
                })
                .catch(errorModal.show)
                .finally(loadingModal.close);
        }
    }, [data, errorModal, loadingModal, close]);

    return (
        <Modal show={visible}
               size='xl'
               centered
        >
            <Modal.Header>
                <Modal.Title>Node Info Modal</Modal.Title>
            </Modal.Header>
            <Modal.Body>
                <ServerInfo deployment={data?.deployment} servers={data?.servers} node={data?.node}/>
                <hr/>
                <ApplicationStatusViewer/>
                <LogViewer/>
            </Modal.Body>
            <Modal.Footer>
                {retryButton(data?.deployment, data?.node, retryOnClick)}
                <Button onClick={close}>close</Button>
            </Modal.Footer>
        </Modal>
    );
};

/**
 * Returns a button or null iff the node is eligible for re-trying deployment (the node has failed deployment)
 *
 * @param deployment
 * @param node
 * @param retryOnClick
 */
const retryButton = (deployment: Deployment[] | undefined, node: number | undefined, retryOnClick: () => void): ReactElement | undefined => {
    if (!node || !deployment) return undefined;

    const deployObject: Deployment = deployment.filter(i => i.new_node_id === node)[0];

    if (deployObject && deployObject.process_status === "FAILED")
        return <Button onClick={retryOnClick}>Retry</Button>;
    else return undefined;
};

/**
 * Get data for retry
 *
 * @param deployment
 * @param node
 * @returns {{newNodeId: number, serverUUID, parentId: number}}
 */
const retryData = (deployment: Deployment[] | undefined, node: number | undefined): Update | undefined => {

    if (!node || !deployment) return undefined;

    const deployObject: Deployment = deployment.filter(i => i.new_node_id === node)[0];

    if (!deployObject) return undefined;

    return {
        parentId: deployObject.parent_node_id,
        newNodeId: deployObject.new_node_id,
        serverUUID: deployObject.server_uuid
    }
};