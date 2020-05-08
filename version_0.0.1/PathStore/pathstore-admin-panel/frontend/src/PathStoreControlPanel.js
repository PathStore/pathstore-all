import React, {Component} from 'react';
import 'react-tree-graph/dist/style.css'
import ViewTopology from "./modules/topology/ViewTopology";
import ApplicationCreation from "./modules/installation/ApplicationCreation";
import DisplayAvailableApplication from "./modules/installation/DisplayAvailableApplication";
import DeployApplication from "./modules/applicationDeployment/DeployApplication";
import LiveTransitionVisual from "./modules/topology/LiveTransitionVisual";
import NodeDeployment from "./modules/nodeDeployment/NodeDeployment";
import {
    createApplicationObject,
    createApplicationStatusObject,
    createDeploymentObject,
    createServerObject
} from "./modules/Utils";

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
 * LiveTransitionVisual
 *  - LiveTransitionVisualModal
 * DisplayAvailableApplications
 * ApplicationCreation
 * DeployApplication
 *
 * The Common components used throughout the application are:
 * PathStoreTopology
 * ErrorResponseModal
 * LoadingModal
 * NodeInfoModal
 *
 * And all global function are in
 * Utils.js
 *
 * For more information about this code base and the project see the pathstore github page
 */
export default class PathStoreControlPanel extends Component {

    /**
     * Global:
     * timer: stores the timer object used for gc on closure of the component
     *
     * State:
     * deployment: list of deployment objects from api
     * servers: list of server objects from api
     * applications: list of application objects from api
     * applicationStatus: list of node status based on applications from api
     *
     * @param props
     */
    constructor(props) {
        super(props);

        this.state = {
            deployment: [],
            servers: [],
            applications: [],
            applicationStatus: []
        }
    }

    /**
     * On mount of the component query all needed data to fill the state and set a timer to refresh this data every 2
     * seconds
     */
    componentDidMount() {
        this.queryAll();

        this.timer = setInterval(this.queryAll, 2000);
    }

    /**
     * Garbage collect interval thread
     */
    componentWillUnmount() {
        clearInterval(this.timer);
    }

    /**
     * Call fetch all to get all the responses and load them into the state
     */
    queryAll = () => {
        this.fetchAll().then(([deployment, servers, applications, applicationStatus]) =>
            this.setState({
                deployment: deployment,
                servers: servers,
                applications: applications,
                applicationStatus: applicationStatus
            }));
    };

    /**
     * Fetches all data needed for the website from the api
     *
     * @returns {Promise<[[], [], [], []]>} 4 array responses
     */
    fetchAll = () => {
        return Promise.all(
            [
                this.genericLoadFunction('/api/v1/deployment', createDeploymentObject),
                this.genericLoadFunction('/api/v1/servers', createServerObject),
                this.genericLoadFunction('/api/v1/applications', createApplicationObject),
                this.genericLoadFunction('/api/v1/application_management', createApplicationStatusObject)
            ]
        );
    };

    /**
     * This function is used to return an array of parsed object data from the api.
     *
     * @param url which url to query
     * @param parsingFunction how to parse each json object in a json array
     * @returns {Promise<[]>} parsed array response
     */
    genericLoadFunction = (url, parsingFunction) => {
        return fetch(url)
            .then(response => response.json())
            .then(response => {
                let messages = [];

                for (let i = 0; i < response.length; i++)
                    messages.push(parsingFunction(response[i]));

                return messages;
            })
    };

    /**
     * Used by child component to force refresh this data. This is only given to child components who are capable
     * of making state changing operations (i.e effecting our state data within this class)
     */
    forceRefresh = () => {
        this.componentWillUnmount();
        this.componentDidMount();
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
    render() {
        return (
            <div>
                <h1>PathStore Control Panel</h1>
                <div>
                    <div>
                        <h2>Network Topology</h2>
                        <ViewTopology topology={this.state.deployment}
                                      servers={this.state.servers}
                                      applicationStatus={this.state.applicationStatus}
                        />
                    </div>
                    <br/>
                    <div>
                        <h2>Network Expansion</h2>
                        <NodeDeployment topology={this.state.deployment}
                                        servers={this.state.servers}
                                        forceRefresh={this.forceRefresh}/>
                    </div>
                </div>
                <br/>
                <div>
                    <div>
                        <h2>Live Installation Viewer</h2>
                        <LiveTransitionVisual applications={this.state.applications}
                                              applicationStatus={this.state.applicationStatus}
                                              topology={this.state.deployment}/>
                    </div>
                    <br/>
                    <div>
                        <h2>Application Creation</h2>
                        <DisplayAvailableApplication applications={this.state.applications}/>
                        <br/>
                        <ApplicationCreation forceRefresh={this.forceRefresh}/>
                    </div>
                    <br/>
                    <div>
                        <h2>Application Deployment</h2>
                        <DeployApplication topology={this.state.deployment}
                                           applications={this.state.applications}
                                           applicationStatus={this.state.applicationStatus}/>
                    </div>
                </div>
            </div>
        );
    }
}
