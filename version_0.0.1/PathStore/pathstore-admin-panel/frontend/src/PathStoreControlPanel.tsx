import React, {Component} from 'react';
import {Application, ApplicationStatus, AvailableLogDates, Deployment, Server} from "./utilities/ApiDeclarations";
import ViewTopology from "./modules/topology/ViewTopology";
import NodeDeployment from "./modules/nodeDeployment/NodeDeployment";
import LiveTransitionVisual from "./modules/topology/LiveTransitionVisual";
import {DisplayAvailableApplications} from "./modules/installation/DisplayAvailableApplications";
import ApplicationCreation from "./modules/installation/ApplicationCreation";
import DeployApplication from "./modules/deployment/DeployApplication";
import {Center} from "./utilities/AlignedDivs";

/**
 * State definition for {@link PathStoreControlPanel}
 */
interface PathStoreControlPanelState {
    /**
     * List of deployment objects from api
     */
    readonly deployment: Deployment[]

    /**
     * List of server objects from api
     */
    readonly servers: Server[]

    /**
     * List of application objects from api
     */
    readonly applications: Application[]

    /**
     * List of node application status from api
     */
    readonly applicationStatus: ApplicationStatus[]

    /**
     * List of available dates for each log
     */
    readonly availableLogDates: AvailableLogDates[]
}

/**
 * This is the main component for the PathStore Control Panel it is the parent of multiple sub components and has the
 * main function of querying all main data for the website on a timer which refreshes ever 2 seconds. This can however
 * be over written by the force refresh function that can be passed to child components to call once they've committed
 * a state changing operation.
 *
 * The main component structure looks like:
 *
 * ViewTopology
 * NodeDeployment
 *  - NodeDeploymentModal
 *      - NodeDeploymentAdditionForm
 *      - AddServers
 *          - ServerCreationResponseModal
 *      - DisplayServers
 * LiveTransitionVisual
 *  - LiveTransitionVisualModal
 * DisplayAvailableApplications
 * ApplicationCreation
 *  - ApplicationCreationResponseModal
 * DeployApplication
 *  - DeployApplicationResponseModal
 *
 * The Common components used throughout the application are:
 * PathStoreTopology
 * ErrorResponseModal
 * LoadingModal
 * NodeInfoModal
 *
 * And all global function are in
 * Utils.tsx
 *
 * And all api responses and formats are in
 * ApiDeclarations.ts
 *
 * For more information about this code base and the project see the pathstore github page
 */
export default class PathStoreControlPanel extends Component<{}, PathStoreControlPanelState> {

    /**
     * Timer for auto refresh of data
     */
    private timer: any;

    /**
     * Denotes whether or not a specific request is still loading (Fixes issue with request stacking)
     */
    private loading: boolean[] = [false, false, false];

    /**
     * Initialize state and empty props
     *
     * @param props
     */
    constructor(props = {}) {
        super(props);

        this.state = {
            deployment: [],
            servers: [],
            applications: [],
            applicationStatus: [],
            availableLogDates: []
        }
    }

    /**
     * On mount of the component query all needed data to fill the state and set a timer to refresh this data every 2
     * seconds
     */
    componentDidMount(): void {

        this.refreshData();

        this.queryAll();

        this.timer = setInterval(this.queryAll, 2000);
    }

    /**
     * Garbage collect interval thread
     */
    componentWillUnmount(): void {
        clearInterval(this.timer);
    }

    /**
     * Function that queries data that does not need to be on a timer
     */
    refreshData = (): void => {
        this.genericLoadFunctionWithOutWait<Server>('/api/v1/servers', "servers");
        this.genericLoadFunctionWithOutWait<Application>('/api/v1/applications', "applications");
    };

    /**
     * Call fetch all to get all the responses and load them into the state
     */
    queryAll = (): void => {
        this.genericLoadFunctionWait<Deployment>('/api/v1/deployment', 0, "deployment");
        this.genericLoadFunctionWait<ApplicationStatus>('/api/v1/application_management', 1, "applicationStatus");
        this.genericLoadFunctionWait<AvailableLogDates>('/api/v1/available_log_dates', 2, "availableLogDates");
    };


    /**
     * This function is used to query from a url and store it in the state without the wait condition
     *
     * @param url url to query
     * @param name name to store in state
     */
    genericLoadFunctionWithOutWait = <T extends unknown>(url: string, name: string): void => {
        this.genericLoadFunctionBase<T>(url)
            .then((response: T[]) => this.setState({[name]: response} as unknown as PathStoreControlPanelState));
    };

    /**
     * This function is used to query from a url and store it in the state with the wait condition
     *
     * @param url url to query
     * @param index index of wait
     * @param name name to store in state
     */
    genericLoadFunctionWait = <T extends unknown>(url: string, index: number, name: string): void => {
        if (!this.loading[index]) {
            this.loading[index] = true;
            this.genericLoadFunctionBase<T>(url)
                .then((response: T[]) => this.setState({[name]: response} as unknown as PathStoreControlPanelState, () => this.loading[index] = false));
        }
    };

    /**
     * This function is used to return an array of parsed object data from the api.
     *
     * @param url url to query
     */
    genericLoadFunctionBase = <T extends unknown>(url: string): Promise<T[]> => {
        return fetch(url)
            .then(response => response.json() as Promise<T[]>);
    };

    /**
     * Used by child component to force refresh this data. This is only given to child components who are capable
     * of making state changing operations (i.e effecting our state data within this class)
     */
    forceRefresh = (): void => {
        this.refreshData();
        this.queryAll();
    };

    /**
     * Render all child components with the following ordering policy
     *
     * Network structure related components
     *
     * Application related components
     *
     * @returns {*}
     */
    render = () => (
        <div className={"main"}>
            <Center>
                <h1 style={{fontWeight: 700, textDecoration: 'underline'}}>PathStore Control Panel</h1>
            </Center>
            <div>
                <ViewTopology deployment={this.state.deployment}
                              servers={this.state.servers}
                              applicationStatus={this.state.applicationStatus}
                              availableLogDates={this.state.availableLogDates}
                              forceRefresh={this.forceRefresh}/>
            </div>
            <div>
                <h2>Network Expansion</h2>
                <NodeDeployment deployment={this.state.deployment}
                                servers={this.state.servers}
                                forceRefresh={this.forceRefresh}/>
            </div>
            <hr/>
            <div>
                <h2>Live Installation Viewer</h2>
                <LiveTransitionVisual applications={this.state.applications}
                                      applicationStatus={this.state.applicationStatus}
                                      deployment={this.state.deployment}/>
            </div>
            <hr/>
            <div>
                <h2>Application Creation</h2>
                <DisplayAvailableApplications applications={this.state.applications}/>
                <ApplicationCreation forceRefresh={this.forceRefresh}/>
            </div>
            <hr/>
            <div>
                <h2>Application Deployment</h2>
                <DeployApplication deployment={this.state.deployment}
                                   applications={this.state.applications}
                                   applicationStatus={this.state.applicationStatus}
                                   servers={this.state.servers}/>
            </div>
        </div>
    );
}
