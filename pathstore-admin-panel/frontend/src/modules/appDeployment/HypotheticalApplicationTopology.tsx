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

import React, {FunctionComponent, ReactElement, useCallback, useContext, useEffect, useState} from "react";
import {AlignedDivs, Left, Right} from "../../utilities/AlignedDivs";
import {
    Application,
    APPLICATION_STATE,
    ApplicationStatus,
    ApplicationUpdate,
    Deployment
} from "../../utilities/ApiDeclarations";
import {PathStoreTopology} from "../PathStoreTopology";
import {ApplicationDeploymentModalDataContext} from "../../contexts/ApplicationDeploymentModalContext";
import {createMap, identity} from "../../utilities/Utils";
import {applicationUpdateFromInfo} from "./ApplicationDeploymentForm";
import {SubmissionErrorModalContext} from "../../contexts/SubmissionErrorModalContext";

/**
 * This component is used to display the hypothetical topology for a given application
 *
 * @constructor
 */
export const HypotheticalApplicationTopology: FunctionComponent = (props) => {
    // Load sets from data context to determine colour
    const {
        reducedDeployment,
        waiting,
        installing,
        processingInstalling,
        installed,
        waitingRemoving,
        removing,
        processingRemoving,
        additionNodeIdSet,
        deletionNodeIdSet,
        application,
        reducedApplicationStatus,
        additions,
        deletions,
        updateAdditions,
        updateDeletions
    } = useContext(ApplicationDeploymentModalDataContext);

    // submission error modal
    const submissionErrorModal = useContext(SubmissionErrorModalContext);

    /**
     * This function is used to determine the css class for a given node based on which set the given deployment object
     * node id is apart of
     */
    const getColour = useCallback((object: Deployment): string => {
        const node = object.new_node_id;

        if (additionNodeIdSet?.has(node)) return 'd_orange_node';
        else if (deletionNodeIdSet?.has(node)) return 'd_red_node';
        else if (processingRemoving?.has(node) || removing?.has(node)) return 'd_purple_node';
        else if (waitingRemoving?.has(node)) return 'd_pink_node';
        else if (installed?.has(node)) return 'd_green_node';
        else if (processingInstalling?.has(node) || installing?.has(node)) return 'd_cyan_node';
        else if (waiting?.has(node)) return 'd_yellow_node';
        else return 'd_current_line_node';

    }, [waiting, installing, processingInstalling, installed, processingRemoving, removing, waitingRemoving, additionNodeIdSet, deletionNodeIdSet]);

    /**
     * Handles the deletion of a node and its subtree if applicable when clicking a given node
     */
    const handleClick = useCallback((event: any, node: number) => {
        if (additions && deletions && reducedApplicationStatus && reducedDeployment && application && updateAdditions && updateDeletions) {

            try {
                const update = deleteSubTree(additions, deletions, reducedApplicationStatus, reducedDeployment, application, node);

                updateAdditions(update.additions);
                updateDeletions(update.deletions);
            } catch (e) {
                if (submissionErrorModal.show)
                    submissionErrorModal.show(e.message);
            }
        }
    }, [additions, deletions, reducedApplicationStatus, reducedDeployment, application, updateAdditions, updateDeletions, submissionErrorModal]);

    // Store an internal state for the tree as the deployment records may be null on startup
    const [tree, updateTree] = useState<ReactElement | null>(null);

    // update the tree when the deployment records change
    useEffect(() => {
        const value: ReactElement =
            reducedDeployment ?
                <PathStoreTopology width={700}
                                   height={500}
                                   deployment={reducedDeployment}
                                   get_colour={getColour}
                                   get_click={handleClick}/>
                : <p>Loading...</p>;
        updateTree(value);
    }, [reducedDeployment, getColour, handleClick, updateTree]);

    return (
        <AlignedDivs>
            <Left width='45%'>
                <h2>Topology Legend</h2>
                <br/>
                <p>Hypothetical additions are in <span className={'d_orange'}>orange</span></p>
                <p>Hypothetical deletions are in <span className={'d_red'}>red</span></p>
                <br/>
                <p>Processing Removing and Removing are in <span className={'d_purple'}>purple</span></p>
                <p>Waiting Removal nodes are in <span className={'d_pink'}>pink</span></p>
                <br/>
                <p>Installed nodes are in <span className={'d_green'}>green</span></p>
                <p>Processing Installing and Installing are in <span className={'d_cyan'}>cyan</span></p>
                <p>Waiting installing nodes are in <span className={'d_yellow'}>yellow</span></p>
                <p>Not set nodes are in <span className={'d_current_line'}>grey</span></p>
            </Left>
            <Right>
                <h2>Topology</h2>
                {tree}
                <p>Click on a node to queue the removal of an application</p>
                {props.children}
            </Right>
        </AlignedDivs>
    );
};

/**
 * This function is used to un-deploy an application on the subtree of the network where the root of subtree is given
 * by the node that was clicked on.
 *
 * @param additions current additions to the network as we will override this list on completion
 * @param deletions current deletions to the network as we will override this list on completion
 * @param applicationStatus applications status based on the given keyspace
 * @param deployment deployment list to denote the current deployed topology
 * @param application application selected
 * @param node which node was clicked on
 */
const deleteSubTree = (
    additions: ApplicationUpdate[],
    deletions: ApplicationUpdate[],
    applicationStatus: ApplicationStatus[],
    deployment: Deployment[],
    application: Application,
    node: number)
    : { additions: ApplicationUpdate[], deletions: ApplicationUpdate[] } => {

    // generate maps from data from node id -> object of interest
    const additionsMap: Map<number, ApplicationUpdate> = createMap<number, ApplicationUpdate>(v => v.nodeId, identity, additions);
    const deletionsMap: Map<number, ApplicationUpdate> = createMap<number, ApplicationUpdate>(v => v.nodeId, identity, deletions);
    const statusMap: Map<number, ApplicationStatus> = createMap<number, ApplicationStatus>(v => v.node_id, identity, applicationStatus);
    const deploymentMap: Map<number, Deployment> = createMap<number, Deployment>(v => v.new_node_id, identity, deployment);

    // map to denote the list of children from a given node id
    const nodeToListOfChildren: Map<number, Deployment[]> = new Map<number, Deployment[]>();
    for (let d of deployment)
        if (!nodeToListOfChildren.has(d.parent_node_id)) nodeToListOfChildren.set(d.parent_node_id, [d]);
        else nodeToListOfChildren.get(d.parent_node_id)?.push(d);

    // Handle delete for sub tree
    deleteSubTreeHelper(additionsMap, deletionsMap, statusMap, deploymentMap, nodeToListOfChildren, application, node);

    // gather children from submitted node
    const submittedChildren = nodeToListOfChildren.get(node);

    // handle delete for requested node
    handleDelete(additionsMap, deletionsMap, statusMap, application, node, submittedChildren ? submittedChildren.map(i => i.new_node_id) : [-1]);

    return (
        {
            additions: Array.from(additionsMap.values()),
            deletions: Array.from(deletionsMap.values())
        }
    );
};

/**
 * This function is used to recursively call all the nodes from the submitted node down to the leafs of that given sub tree.
 * It will only continue to recurse if the node hit is not already inside the deletions map
 *
 * @param additionsMap additions map, to remove from if a node in the subtree is queued for installation
 * @param deletionsMap deletions map, to add from it if a node is currently at the INSTALLED state
 * @param statusMap status map, to determine what and if a current node has a record for the given keyspace
 * @param deploymentMap deployment map, to determine the subtree of the network
 * @param nodeToListOfChildren map to determine what the list of children are from a given node id
 * @param application what the current application is
 * @param node currently selected node
 */
const deleteSubTreeHelper = (
    additionsMap: Map<number, ApplicationUpdate>,
    deletionsMap: Map<number, ApplicationUpdate>,
    statusMap: Map<number, ApplicationStatus>,
    deploymentMap: Map<number, Deployment>,
    nodeToListOfChildren: Map<number, Deployment[]>,
    application: Application,
    node: number): void => {

    if (node === -1) return;
    else {
        const children = nodeToListOfChildren.get(node);

        if (children && !deletionsMap.has(node))
            for (let {new_node_id} of children) {
                deleteSubTreeHelper(additionsMap, deletionsMap, statusMap, deploymentMap, nodeToListOfChildren, application, new_node_id);

                const childrenChildren = nodeToListOfChildren.get(new_node_id);

                handleDelete(additionsMap, deletionsMap, statusMap, application, new_node_id, childrenChildren ? childrenChildren.map(i => i.new_node_id) : [-1]);
            }
    }
};

/**
 * This function is responsible for managing the map state by performing the actual delete.
 *
 * The cases for delete are:
 * (1) it is part of the additions map so we remove that
 * (2) It currently has a status
 *      (a) its currently installed, then it adds it to the deletions map
 *      (b) its at any other state,
 *          (1) If its waiting_installing, installing, processing_installing, throw an error describing what happened
 *          (2) Else it must be waiting_removing, removing, processing_removing, throw an error describing what happened
 *
 * @param additionsMap additions map, to remove from if a node in the subtree is queued for installation
 * @param deletionsMap deletions map, to add from it if a node is currently at the INSTALLED state
 * @param statusMap status map, to determine what and if a current node has a record for the given keyspace
 * @param application what the current application is
 * @param node currently selected node
 * @param children current list of children for the given node
 */
const handleDelete = (
    additionsMap: Map<number, ApplicationUpdate>,
    deletionsMap: Map<number, ApplicationUpdate>,
    statusMap: Map<number, ApplicationStatus>,
    application: Application,
    node: number,
    children: number[]): void => {

    if (additionsMap.has(node)) additionsMap.delete(node);
    else if (statusMap.has(node)) {
        switch (statusMap.get(node)?.process_status) {
            case APPLICATION_STATE[APPLICATION_STATE.INSTALLED]:
                deletionsMap.set(node, applicationUpdateFromInfo(application, node, children.length > 0 ? children : [-1]));
                break;
            case APPLICATION_STATE[APPLICATION_STATE.PROCESSING_INSTALLING]:
            case APPLICATION_STATE[APPLICATION_STATE.INSTALLING]:
            case APPLICATION_STATE[APPLICATION_STATE.WAITING_INSTALL]:
            case undefined:
                throw new Error("You cannot perform application deletion on a subtree with a currently installing node");
            default:
                throw new Error("You cannot perform application deletion on a subtree with a currently removing node" + node); // TODO: Maybe don't have this
        }
    }
};