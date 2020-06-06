import {Deployment, Server, Update} from "../utilities/ApiDeclarations";
import {useContext, useState} from "react";
import {APIContext} from "../contexts/APIContext";
import {useObjectAttachedSet} from "./useObjectAttachedSet";

/**
 * This is the definition for {@link NodeDeploymentModalContextData}
 */
export interface NodeDeploymentModalContextData {
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
    readonly additions: Update[];

    /**
     * Function to modify the above list
     */
    readonly updateAdditions: (v: Update[]) => void;

    /**
     * List of deletion records, this stores all the deletion changes to the network that are staged
     */
    readonly deletions: Update[];

    /**
     * Function to modify the above list
     */
    readonly updateDeletions: (v: Update[]) => void;

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
export function useNodeDeploymentModalData(): NodeDeploymentModalContextData {

    const apiContext = useContext(APIContext);

    const [additions, updateAdditions] = useState<Update[]>([]);

    const [deletions, updateDeletions] = useState<Update[]>([]);

    const additionNodeIdSet = useObjectAttachedSet<Update, number>(additions, updateToNewNodeId);

    const deletionNodeIdSet = useObjectAttachedSet<Update, number>(deletions, updateToNewNodeId);

    return {
        deployment: apiContext.deployment ? apiContext.deployment : [],
        servers: apiContext.servers ? apiContext.servers : [],
        additions: additions,
        updateAdditions: updateAdditions,
        deletions: deletions,
        updateDeletions: updateDeletions,
        additionNodeIdSet: additionNodeIdSet,
        deletionNodeIdSet: deletionNodeIdSet,
        forceRefresh: apiContext.forceRefresh
    }
}

const updateToNewNodeId = (update: Update): number => update.newNodeId;