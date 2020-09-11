import {Deployment, Server, DeploymentUpdate} from "../utilities/ApiDeclarations";
import {useContext, useState} from "react";
import {APIContext} from "../contexts/APIContext";
import {useObjectAttachedSet} from "./useObjectAttachedSet";

/**
 * This is the definition for {@link NodeDeploymentModalData}
 */
export interface NodeDeploymentModalData {
    /**
     * List of deployment records from the api
     */
    readonly deployment: Deployment[]

    /**
     * List of server records from the api
     */
    readonly servers: Server[];

    /**
     * List of addition records, this stores all the additive changes to the network that are staged
     */
    readonly additions: DeploymentUpdate[];

    /**
     * Function to modify the above list
     */
    readonly updateAdditions: (v: DeploymentUpdate[]) => void;

    /**
     * List of deletion records, this stores all the deletion changes to the network that are staged
     */
    readonly deletions: DeploymentUpdate[];

    /**
     * Function to modify the above list
     */
    readonly updateDeletions: (v: DeploymentUpdate[]) => void;

    /**
     * Set of addition nodes. This is for O(1) tc on the get colour function
     */
    readonly additionNodeIdSet: Set<number>;

    /**
     * Set of deletion nodes. This is for O(1) tc on the get colour function
     */
    readonly deletionNodeIdSet: Set<number>;

    /**
     * Force refresh function used to force refresh {@link APIContext}
     */
    readonly forceRefresh: (() => void) | undefined;
}

/**
 * This custom hook is used to generate the default data state as described above
 */
export function useNodeDeploymentModalData(): NodeDeploymentModalData {

    // load api context
    const {deployment, servers, forceRefresh} = useContext(APIContext);

    // store additions
    const [additions, updateAdditions] = useState<DeploymentUpdate[]>([]);

    // store deletions
    const [deletions, updateDeletions] = useState<DeploymentUpdate[]>([]);

    // attached set for additions
    const additionNodeIdSet = useObjectAttachedSet<DeploymentUpdate, number>(additions, updateToNewNodeId);

    // attached set for deletions
    const deletionNodeIdSet = useObjectAttachedSet<DeploymentUpdate, number>(deletions, updateToNewNodeId);

    return {
        deployment: deployment ? deployment : [],
        servers: servers ? servers : [],
        additions: additions,
        updateAdditions: updateAdditions,
        deletions: deletions,
        updateDeletions: updateDeletions,
        additionNodeIdSet: additionNodeIdSet,
        deletionNodeIdSet: deletionNodeIdSet,
        forceRefresh: forceRefresh
    }
}

// Function to convert an update object to its newNodeId
const updateToNewNodeId = (update: DeploymentUpdate): number => update.newNodeId;