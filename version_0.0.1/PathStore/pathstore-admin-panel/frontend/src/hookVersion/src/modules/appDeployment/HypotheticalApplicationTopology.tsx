import React, {FunctionComponent, ReactElement, useCallback, useContext, useEffect, useState} from "react";
import {AlignedDivs, Left, Right} from "../../utilities/AlignedDivs";
import {Application, ApplicationStatus, ApplicationUpdate, Deployment} from "../../utilities/ApiDeclarations";
import {PathStoreTopology} from "../PathStoreTopology";
import {ApplicationDeploymentModalDataContext} from "../../contexts/ApplicationDeploymentModalContext";
import {createMap, identity} from "../../utilities/Utils";
import {applicationUpdateFromInfo} from "./ApplicationDeploymentForm";

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
        additionNodeIdSet,
        deletionNodeIdSet,
        application,
        reducedApplicationStatus,
        additions,
        deletions,
        updateAdditions,
        updateDeletions
    } = useContext(ApplicationDeploymentModalDataContext);

    /**
     * This function is used to determine the css class for a given node based on which set the given deployment object
     * node id is apart of
     */
    const getColour = useCallback((object: Deployment): string => {
        const node = object.new_node_id;

        if (additionNodeIdSet?.has(node)) return 'waiting_removal_node';
        else if (deletionNodeIdSet?.has(node)) return 'uninstalled_node';
        else if (installed?.has(node)) return 'installed_node';
        else if (processingInstalling?.has(node)) return 'processing_node';
        else if (installing?.has(node)) return 'installing_node';
        else if (waiting?.has(node)) return 'waiting_node';
        else return 'not_set_node';

    }, [waiting, installing, processingInstalling, installed, additionNodeIdSet, deletionNodeIdSet]);

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
                alert("You cannot perform removal on a subtree that is currently state changing")
            }
        }
    }, [additions, deletions, reducedApplicationStatus, reducedDeployment, application, updateAdditions, updateDeletions]);

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
            <Left width='35%'>
                <h2>Topology Legend</h2>
                <p>Node hypothetically installed are in <span className={'d_pink'}>pink</span></p>
                <p>Node hypothetically removed are in <span className={'d_red'}>red</span></p>
                <p>Nodes installed are in <span className={'d_green'}>green</span></p>
                <p>Nodes processing are in <span className={'d_orange'}>orange</span></p>
                <p>Nodes installing are in <span className={'d_cyan'}>cyan</span></p>
                <p>Nodes waiting are in <span className={'d_yellow'}>yellow</span></p>
                <p>Nodes not set are <span className={'d_currentLine'}>dark grey</span></p>
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

const deleteSubTree = (
    additions: ApplicationUpdate[],
    deletions: ApplicationUpdate[],
    applicationStatus: ApplicationStatus[],
    deployment: Deployment[],
    application: Application,
    node: number)
    : { additions: ApplicationUpdate[], deletions: ApplicationUpdate[] } => {

    const additionsMap: Map<number, ApplicationUpdate> = createMap<number, ApplicationUpdate>(v => v.nodeId, identity, additions);
    const deletionsMap: Map<number, ApplicationUpdate> = createMap<number, ApplicationUpdate>(v => v.nodeId, identity, deletions);
    const statusMap: Map<number, ApplicationStatus> = createMap<number, ApplicationStatus>(v => v.node_id, identity, applicationStatus);
    const deploymentMap: Map<number, Deployment> = createMap<number, Deployment>(v => v.new_node_id, identity, deployment);

    const nodeToListOfChildren: Map<number, Deployment[]> = new Map<number, Deployment[]>();

    deployment.forEach(v => {
        if (!nodeToListOfChildren.has(v.parent_node_id)) nodeToListOfChildren.set(v.parent_node_id, [v]);
        else nodeToListOfChildren.get(v.parent_node_id)?.push(v);
    });

    // Handle delete for sub tree
    deleteSubTreeHelper(additionsMap, deletionsMap, statusMap, deploymentMap, nodeToListOfChildren, application, node);

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

        if (children && !deletionsMap.has(node)) {
            for (let {new_node_id} of children) {
                deleteSubTreeHelper(additionsMap, deletionsMap, statusMap, deploymentMap, nodeToListOfChildren, application, new_node_id);
                handleDelete(additionsMap, deletionsMap, statusMap, application, new_node_id, children.map(i => i.new_node_id));
            }
        }
    }
};

const handleDelete = (
    additionsMap: Map<number, ApplicationUpdate>,
    deletionsMap: Map<number, ApplicationUpdate>,
    statusMap: Map<number, ApplicationStatus>,
    application: Application,
    node: number,
    children: number[]): void => {

    if (additionsMap.has(node)) additionsMap.delete(node);
    else if (statusMap.has(node)) {
        if (statusMap.get(node)?.process_status === "INSTALLED")
            deletionsMap.set(node, applicationUpdateFromInfo(application, node, children.length > 0 ? children : [-1]));
        else
            throw new Error();
    }
};