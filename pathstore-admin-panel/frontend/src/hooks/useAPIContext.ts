import {
    Application,
    ApplicationStatus,
    AvailableLogDates,
    Deployment,
    Server
} from "../utilities/ApiDeclarations";
import {useCallback, useState} from "react";
import {genericLoadFunction} from "../utilities/Utils";

/**
 * Definition of the api context {@link APIContext}
 */
export interface APIContextType {
    /**
     * List of deployment objects from api
     */
    readonly deployment: Deployment[]

    /**
     * Set deployment objects
     */
    readonly setDeployment: (v: Deployment[]) => void;

    /**
     * List of server objects from api
     */
    readonly servers: Server[]

    /**
     * Set server objects
     */
    readonly setServers: (v: Server[]) => void;

    /**
     * List of application objects from api
     */
    readonly applications: Application[]

    /**
     * Set application objects
     */
    readonly setApplications: (v: Application[]) => void;

    /**
     * List of node application status from api
     */
    readonly applicationStatus: ApplicationStatus[]

    /**
     * Set application status objects
     */
    readonly setApplicationStatus: (v: ApplicationStatus[]) => void;

    /**
     * List of available dates for each log
     */
    readonly availableLogDates: AvailableLogDates[]

    /**
     * Set available log dates
     */
    readonly setAvailableLogDates: (v: AvailableLogDates[]) => void;

    /**
     * Used to force refresh data
     */
    readonly forceRefresh: () => void;
}

/**
 * Sets up the state for each individual piece of data above, so that components can read the data from the api from anywhere
 * and can set it from anywhere
 */
export function useAPIContext(): APIContextType {

    const [deployment, setDeployment] = useState<Deployment[]>([]);

    const [servers, setServers] = useState<Server[]>([]);

    const [applications, setApplications] = useState<Application[]>([]);

    const [applicationStatus, setApplicationStatus] = useState<ApplicationStatus[]>([]);

    const [availableLogDates, setAvailableLogDates] = useState<AvailableLogDates[]>([]);

    const forceRefresh = useCallback(() => {
        genericLoadFunction<Server>('/api/v1/servers', setServers);
        genericLoadFunction<Application>('/api/v1/applications', setApplications);
        genericLoadFunction<Deployment>('/api/v1/deployment', setDeployment);
        genericLoadFunction<ApplicationStatus>('/api/v1/application_management', setApplicationStatus);
        genericLoadFunction<AvailableLogDates>('/api/v1/available_log_dates', setAvailableLogDates);
    }, [setServers, setApplications, setDeployment, setApplicationStatus, setAvailableLogDates]);

    return {
        deployment,
        setDeployment,
        servers,
        setServers,
        applications,
        setApplications,
        applicationStatus,
        setApplicationStatus,
        availableLogDates,
        setAvailableLogDates,
        forceRefresh
    };
}