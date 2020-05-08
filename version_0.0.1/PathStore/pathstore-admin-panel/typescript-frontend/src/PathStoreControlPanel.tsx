import React, {Component} from 'react';
import {Application, ApplicationStatus, Deployment, Server} from "./utilities/ApiDeclarations";
import ViewTopology from "./modules/topology/ViewTopology";
import NodeDeployment from "./modules/nodeDeployment/NodeDeployment";

interface PathStoreControlPanelState {
    deployment: Deployment[]
    servers: Server[]
    applications: Application[]
    applicationStatus: ApplicationStatus[]
}

export default class PathStoreControlPanel extends Component<{}, PathStoreControlPanelState> {

    private timer: any;

    constructor(props = {}) {
        super(props);

        this.state = {
            deployment: [],
            servers: [],
            applications: [],
            applicationStatus: []
        }
    }

    componentDidMount(): void {
        this.queryAll();

        this.timer = setInterval(this.queryAll, 2000);
    }

    componentWillUnmount(): void {
        clearInterval(this.timer);
    }

    queryAll = () => {
        this.genericLoadFunction<Deployment>('/api/v1/deployment')
            .then((response: Deployment[]) => this.setState({deployment: response}));

        this.genericLoadFunction<Server>('/api/v1/servers')
            .then((response: Server[]) => this.setState({servers: response}));

        this.genericLoadFunction<Application>('/api/v1/applications')
            .then((response: Application[]) => this.setState({applications: response}));

        this.genericLoadFunction<ApplicationStatus>('/api/v1/application_management')
            .then((response: ApplicationStatus[]) => this.setState({applicationStatus: response}));
    };

    genericLoadFunction = <T extends unknown>(url: string): Promise<T[]> =>
        fetch(url)
            .then(response => response.json() as Promise<T[]>);

    forceRefresh = () => {
        this.componentWillUnmount();
        this.componentDidMount();
    };


    render = () => (
        <div>
            <h1>PathStore Control Panel</h1>
            <div>
                <div>
                    <h2>Network Topology</h2>
                    <ViewTopology deployment={this.state.deployment}
                                  servers={this.state.servers}
                                  applicationStatus={this.state.applicationStatus}
                    />
                </div>
                <br/>
                <div>
                    <h2>Network Expansion</h2>
                    <NodeDeployment deployment={this.state.deployment}
                                    servers={this.state.servers}
                                    forceRefresh={this.forceRefresh}/>
                </div>
            </div>
        </div>
    );
}
