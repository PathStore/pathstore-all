import React, {Component} from 'react';
import './App.css';
import 'react-tree-graph/dist/style.css'
import ViewTopology from "./modules/ViewTopology";
import ApplicationCreation from "./modules/ApplicationCreation";
import ApplicationInstallation from "./modules/ApplicationInstallation";

/**
 * TODO: Query topology and available applications here
 */
export default class Login extends Component {

    constructor(props) {
        super(props);

        this.state = {
            topology: [],
            applications: [],
            refresh: false
        }
    }

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

    //Message is a json array
    parse = (message) => {
        let array = [];

        message.forEach(i => array.push({parentid: i.parent_nodeid, id: i.nodeid}));

        return array;
    };

    createApplicationObject = (object) => {
        return {
            application: object.keyspace_name
        }
    };

    forceRefresh = () => this.setState({refresh: !this.state.refresh}, () => this.componentDidMount());

    render() {
        return (
            <div>
                <h1>PathStore Control Panel</h1>
                <ViewTopology topology={this.state.topology} refresh={this.state.refresh}/>
                <br/>
                <ApplicationCreation applications={this.state.applications} refresh={this.state.refresh}
                                     forceRefresh={this.forceRefresh}/>
                <br/>
                <ApplicationInstallation topology={this.state.topology} applications={this.state.applications}
                                         refresh={this.state.refresh}/>
            </div>
        );
    }
}
