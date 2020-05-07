import React, {Component} from 'react';
import './App.css';
import 'react-tree-graph/dist/style.css'
import ViewTopology from "./modules/topology/ViewTopology";
import ApplicationCreation from "./modules/installation/ApplicationCreation";
import DisplayAvailableApplication from "./modules/installation/DisplayAvailableApplication";
import DeployApplication from "./modules/applicationDeployment/DeployApplication";
import LiveTransitionVisual from "./modules/topology/LiveTransitionVisual";
import NodeDeployment from "./modules/nodeDeployment/NodeDeployment";

export default class App extends Component {

    constructor(props) {
        super(props);

        this.state = {
            deployment: [],
            servers: [],
            applications: [],
            applicationStatus: []
        }
    }

    componentDidMount() {
        this.queryAll();

        this.timer = setInterval(this.queryAll, 2000);
    }

    componentWillUnmount() {
        clearInterval(this.timer);
    }

    queryAll = () => {
        this.loadAll().then(([deployment, servers, applications, applicationStatus]) =>
            this.setState({
                deployment: deployment,
                servers: servers,
                applications: applications,
                applicationStatus: applicationStatus
            }));
    };

    loadAll = () => {
        return Promise.all(
            [
                this.genericLoadFunction('/api/v1/deployment', this.createDeploymentObject),
                this.genericLoadFunction('/api/v1/servers', this.createServerObject),
                this.genericLoadFunction('/api/v1/applications', this.createApplicationObject),
                this.genericLoadFunction('/api/v1/application_management', this.createApplicationStatusObject)
            ]
        );
    };

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

    createDeploymentObject = (object) => {
        return {
            new_node_id: parseInt(object.new_node_id),
            parent_node_id: parseInt(object.parent_node_id),
            process_status: object.process_status,
            server_uuid: object.server_uuid
        }
    };

    createServerObject = (object) => {
        return {
            server_uuid: object.server_uuid,
            ip: object.ip,
            username: object.username,
            name: object.name
        }
    };

    createApplicationObject = (object) => {
        return {
            keyspace_name: object.keyspace_name
        }
    };

    createApplicationStatusObject = (object) => {
        return {
            nodeid: parseInt(object.nodeid),
            keyspace_name: object.keyspace_name,
            process_status: object.process_status,
            wait_for: object.wait_for,
            process_uuid: object.process_uuid
        }
    };

    forceRefresh = () => this.setState({refresh: !this.state.refresh}, () => {
        this.componentWillUnmount();
        this.componentDidMount();
    });

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
