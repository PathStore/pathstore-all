import React, {Component} from 'react';
import './App.css';
import 'react-tree-graph/dist/style.css'
import ViewTopology from "./modules/topology/ViewTopology";
import ApplicationCreation from "./modules/installation/ApplicationCreation";
import DisplayAvailableApplication from "./modules/installation/DisplayAvailableApplication";
import DeployApplication from "./modules/deployment/DeployApplication";
import LiveTransitionVisual from "./modules/topology/LiveTransitionVisual";

/**
 * This class is used to display needed sub-modules for the website
 *
 *  - ViewTopology (Used to show a graphical visualization of the network diagram)
 *  - ApplicationCreate (Used to allow a user to deploy a new application (DB Schema))
 *  - ApplicationInstallation (Used to allow a user to deploy a created application on a subset of nodes)
 *
 *  For more information on application creation / application installation please see the readme for the pathstore
 *  website API.
 */
export default class App extends Component {

    /**
     * State:
     *
     * topology: array of parentid to childid objects used to denote the network structure
     * applications: array of currently created applications from {@link ApplicationCreation}
     * refresh: flip-flop. When changed the module is refreshed
     */
    constructor(props) {
        super(props);

        this.state = {
            topology: [],
            applications: [],
            refresh: false
        }
    }

    /**
     * Calls the topology end point and parses then data. Then calls the applications endpoint and parses that data.
     */
    componentDidMount() {
        fetch('/api/v1/topology')
            .then(response => response.json())
            .then(message => this.setState({topology: this.parse(message)}))
            .then(() => {
                fetch('/api/v1/applications')
                    .then(response => response.json())
                    .then(message => {
                        let messages = [];

                        for (let i = 0; i < message.length; i++)
                            messages.push(this.createApplicationObject(message[i]));

                        this.setState({applications: messages, refresh: !this.state.refresh});
                    });
            })
    }

    /**
     * Parses topology json array into an array of readable objects
     *
     * @param message response from topology end point
     * @returns array of readable data
     */
    parse = (message) => {
        let array = [];

        message.forEach(i => array.push({parentid: i.parent_nodeid, id: i.nodeid}));

        return array;
    };

    /**
     * Filters out un-needed data from api response
     *
     * @param object api response
     * @returns {application: *}
     */
    createApplicationObject = (object) => {
        return {
            application: object.keyspace_name
        }
    };

    /**
     * Swaps the refresh flip-flop and reloads the component
     */
    forceRefresh = () => this.setState({refresh: !this.state.refresh}, () => this.componentDidMount());

    /**
     * Rends all needed components and spaced correctly
     * @returns {*}
     */
    render() {
        return (
            <div>
                <h1>PathStore Control Panel</h1>
                <div>
                    <h2>Network Topology</h2>
                    <ViewTopology topology={this.state.topology} refresh={this.state.refresh}/>
                    <h2>Live Installation Viewer</h2>
                    <LiveTransitionVisual applications={this.state.applications} topology={this.state.topology}
                                          refresh={this.state.refresh}/>
                </div>
                <br/>
                <div>
                    <h2>Application Creation</h2>
                    <DisplayAvailableApplication applications={this.state.applications} refresh={this.state.refresh}/>
                    <br/>
                    <ApplicationCreation applications={this.state.applications} refresh={this.state.refresh}
                                         forceRefresh={this.forceRefresh}/>
                </div>
                <br/>
                <div>
                    <h2>Application Deployment</h2>
                    <DeployApplication topology={this.state.topology} applications={this.state.applications}
                                       refresh={this.state.refresh}/>
                </div>
            </div>
        );
    }
}
