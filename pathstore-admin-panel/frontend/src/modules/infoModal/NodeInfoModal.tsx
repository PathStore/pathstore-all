/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

import React, {FunctionComponent, ReactElement, useCallback, useContext} from "react";
import {NodeInfoModalContext} from "../../contexts/NodeInfoModalContext";
import Modal from "react-bootstrap/Modal";
import Button from "react-bootstrap/Button";
import {ServerInfo} from "../modalShared/ServerInfo";
import {ApplicationStatusViewer} from "./ApplicationStatusViewer";
import {LogViewer} from "./LogViewer";
import {ErrorModalContext} from "../../contexts/ErrorModalContext";
import {LoadingModalContext} from "../../contexts/LoadingModalContext";
import {Deployment, DeploymentUpdate, DEPLOYMENT_STATE} from "../../utilities/ApiDeclarations";
import {webHandler} from "../../utilities/Utils";
import {APIContext} from "../../contexts/APIContext";
import {ConfirmationButton} from "../confirmation/ConfirmationButton";

/**
 * This component is used to display information in the main viewtopology about a given node.
 *
 * The user can see the currently applications installed, them can see the server info and they can request
 * logs from the server based on day and log level
 * @constructor
 */
export const NodeInfoModal: FunctionComponent = () => {

    // load data from api context
    const {deployment, servers, forceRefresh} = useContext(APIContext);

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
                body: JSON.stringify({record: retryData(deployment, data)})
            })
                .then(webHandler)
                .then(() => {
                    if (close)
                        close();
                    if (forceRefresh)
                        forceRefresh();
                })
                .catch(errorModal.show)
                .finally(loadingModal.close);
        }
    }, [data, errorModal, loadingModal, close, deployment, forceRefresh]);

    return (
        <Modal show={visible}
               size='xl'
               centered
        >
            <Modal.Header>
                <Modal.Title>Node Info Modal</Modal.Title>
            </Modal.Header>
            <Modal.Body>
                <ServerInfo deployment={deployment} servers={servers} node={data}/>
                <hr/>
                <ApplicationStatusViewer/>
                <LogViewer/>
            </Modal.Body>
            <Modal.Footer>
                {retryButton(deployment, data, retryOnClick)}
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

    if (deployObject && deployObject.process_status === DEPLOYMENT_STATE[DEPLOYMENT_STATE.FAILED])
        return (
            <ConfirmationButton message={"Are you sure you want to retry the deployment of the given node?"}
                                onClick={retryOnClick}>
                Retry
            </ConfirmationButton>
        );
    else return undefined;
};

/**
 * Get data for retry
 *
 * @param deployment
 * @param node
 * @returns {{newNodeId: number, serverUUID, parentId: number}}
 */
const retryData = (deployment: Deployment[] | undefined, node: number | undefined): DeploymentUpdate | undefined => {

    if (!node || !deployment) return undefined;

    const deployObject: Deployment = deployment.filter(i => i.new_node_id === node)[0];

    if (!deployObject) return undefined;

    return {
        parentId: deployObject.parent_node_id,
        newNodeId: deployObject.new_node_id,
        serverUUID: deployObject.server_uuid
    }
};