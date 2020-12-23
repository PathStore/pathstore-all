import {
    Application,
    APPLICATION_STATE,
    ApplicationStatus,
    ApplicationUpdate,
    Deployment
} from "../utilities/ApiDeclarations";
import {useObjectAttachedSet} from "./useObjectAttachedSet";
import {useCallback, useContext} from "react";
import {useReducedState} from "./useReducedState";
import {APIContext} from "../contexts/APIContext";
import {useCachedState} from "./useCachedState";

/**
 * Application Deployment Modal data for that modal and the children
 */
export interface ApplicationDeploymentModalData {
    /**
     * Deployment object set with only deployed objects
     */
    readonly reducedDeployment: Deployment[]

    /**
     * Application status objects only for the supplied keyspace
     */
    readonly reducedApplicationStatus: ApplicationStatus[];

    /**
     * Application to load the modal with
     */
    readonly application: Application | undefined;

    /**
     * Set of waiting node id's based on the reduced application status
     */
    readonly waiting: Set<number>;

    /**
     * Set of installing node id's based on the reduced application status
     */
    readonly installing: Set<number>;

    /**
     * Set of processing installing node id's based on the reduced application status
     */
    readonly processingInstalling: Set<number>;

    /**
     * Set of installed node id's based on the reduced application status
     */
    readonly installed: Set<number>;

    /**
     * Set of waitingRemoving node id's based on the reduced application status
     */
    readonly waitingRemoving: Set<number>;

    /**
     * Set of removing node id's based on the reduced application status
     */
    readonly removing: Set<number>;

    /**
     * Set of processingRemoving node id's based on the reduced application status
     */
    readonly processingRemoving: Set<number>;

    /**
     * List of addition objects. This list is based on which nodes you plan on installing on
     */
    readonly additions: ApplicationUpdate[];

    /**
     * Callback to update additions list
     */
    readonly updateAdditions: (additions: ApplicationUpdate[]) => void;

    /**
     * List of deletion objects. This list is based on which nodes you plan on removing
     */
    readonly deletions: ApplicationUpdate[];

    /**
     * Callback to update deletions list
     */
    readonly updateDeletions: (deletions: ApplicationUpdate[]) => void;

    /**
     * Node id set of the addition list for graph colouring
     */
    readonly additionNodeIdSet: Set<number>;

    /**
     * Node id set of the deletion list for graph colouring
     */
    readonly deletionNodeIdSet: Set<number>;
}

/**
 * This hook is used to generate the deployment modal data on loading of the Application Deployment Modal
 *
 * @param application application that was selected
 */
export function useApplicationDeploymentModalData(application: Application | undefined): ApplicationDeploymentModalData {

    // Load application status from the api context
    const {deployment, applicationStatus} = useContext(APIContext);

    // reduce the deployment list to only objects that are DEPLOYED
    const reducedDeployment = useReducedState<Deployment>(
        deployment,
        deployedFilter
    );

    // Callback function to map the application status object to a specific keyspace
    const mapApplicationStatusToKeySpaceName = useCallback(
        (applicationStatus: ApplicationStatus) => applicationStatus.keyspace_name === application?.keyspace_name,
        [application]);

    // Create a reduced application status set based on what keyspace was passed
    const reducedApplicationStatus = useReducedState<ApplicationStatus>(
        applicationStatus,
        mapApplicationStatusToKeySpaceName);

    // Keep an updated waiting deployment set to use too determining each node's colour
    const waiting: Set<number> = useObjectAttachedSet<ApplicationStatus, number>(
        reducedApplicationStatus,
        mapApplicationStatusToNodeId,
        waitingFilter);

    // Keep an updated installing deployment set to use to determine each node's colour
    const installing: Set<number> = useObjectAttachedSet<ApplicationStatus, number>(
        reducedApplicationStatus,
        mapApplicationStatusToNodeId,
        installingFilter);

    // Keep an updated installing deployment set to use to determine each node's colour
    const processingInstalling: Set<number> = useObjectAttachedSet<ApplicationStatus, number>(
        reducedApplicationStatus,
        mapApplicationStatusToNodeId,
        processingInstallingFilter);

    // Keep an updated installed deployment set to use to determine each node's colour
    const installed: Set<number> = useObjectAttachedSet<ApplicationStatus, number>(
        reducedApplicationStatus,
        mapApplicationStatusToNodeId,
        installedFilter);

    // Keep an updated installed deployment set to use to determine each node's colour
    const waitingRemoving: Set<number> = useObjectAttachedSet<ApplicationStatus, number>(
        reducedApplicationStatus,
        mapApplicationStatusToNodeId,
        waitingRemoveFilter);

    // Keep an updated installed deployment set to use to determine each node's colour
    const removing: Set<number> = useObjectAttachedSet<ApplicationStatus, number>(
        reducedApplicationStatus,
        mapApplicationStatusToNodeId,
        removingFilter);

    // Keep an updated installed deployment set to use to determine each node's colour
    const processingRemoving: Set<number> = useObjectAttachedSet<ApplicationStatus, number>(
        reducedApplicationStatus,
        mapApplicationStatusToNodeId,
        processingRemovingFilter);

    // additions based on the current keyspace
    const [additions, updateAdditions] = useCachedState<string, ApplicationUpdate>(application?.keyspace_name);

    // deletions based on the current keyspace
    const [deletions, updateDeletions] = useCachedState<string, ApplicationUpdate>(application?.keyspace_name);

    // node id set based on the addition objects
    const additionNodeIdSet = useObjectAttachedSet<ApplicationUpdate, number>(
        additions,
        mapApplicationUpdateToNodeId
    );

    // node id set based on the deletion objects
    const deletionNodeIdSet = useObjectAttachedSet<ApplicationUpdate, number>(
        deletions,
        mapApplicationUpdateToNodeId
    );

    return {
        reducedDeployment: reducedDeployment,
        reducedApplicationStatus: reducedApplicationStatus,
        application: application,
        waiting: waiting,
        installing: installing,
        processingInstalling: processingInstalling,
        installed: installed,
        waitingRemoving: waitingRemoving,
        removing: removing,
        processingRemoving: processingRemoving,
        additions: additions,
        updateAdditions: updateAdditions,
        deletions: deletions,
        updateDeletions: updateDeletions,
        additionNodeIdSet: additionNodeIdSet,
        deletionNodeIdSet: deletionNodeIdSet
    };
}

// filter lambda function to only allow deployed nodes to be shown
const deployedFilter = (deployment: Deployment) => deployment.process_status === "DEPLOYED";

// How to map an application status object to its node id
const mapApplicationStatusToNodeId = (applicationStatus: ApplicationStatus) => applicationStatus.node_id;

// Filter a given state by what their current process status is
const filterByState = (value: APPLICATION_STATE) => (applicationStatus: ApplicationStatus) => applicationStatus.process_status === APPLICATION_STATE[value];

// NOTE: All functions are statically defined rather then using callbacks for efficiency.

// filter by waiting install
const waitingFilter = filterByState(APPLICATION_STATE.WAITING_INSTALL);

// filter by installing
const installingFilter = filterByState(APPLICATION_STATE.INSTALLING);

// filter by processing installing
const processingInstallingFilter = filterByState(APPLICATION_STATE.PROCESSING_INSTALLING);

// filter by installed
const installedFilter = filterByState(APPLICATION_STATE.INSTALLED);

// filter by installed
const waitingRemoveFilter = filterByState(APPLICATION_STATE.WAITING_REMOVE);

// filter by installed
const removingFilter = filterByState(APPLICATION_STATE.REMOVING);

// filter by installed
const processingRemovingFilter = filterByState(APPLICATION_STATE.PROCESSING_REMOVING);

// function to map an application update object to a node id
const mapApplicationUpdateToNodeId = (applicationUpdate: ApplicationUpdate) => applicationUpdate.nodeId;