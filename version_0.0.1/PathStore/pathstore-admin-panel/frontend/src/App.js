import React, {Component} from 'react';
import './App.css';
import Tree from 'react-tree-graph';
import 'react-tree-graph/dist/style.css'
import Table from 'react-bootstrap/Table'
import Modal from 'react-modal';
import Form from 'react-bootstrap/Form'
import Button from 'react-bootstrap/Button'

class ViewTopology extends Component {
    constructor(props) {
        super(props);
        this.state = {
            topology: this.createTree(this.props.topology, -1),
            info: null,
            refreshInfo: false
        };
    }

    componentWillReceiveProps(props, nextContext) {
        if (this.props.refresh !== props.refresh) {
            this.setState({topology: this.createTree(props.topology, -1)})
        }
    }

    createTree = (array, parentId) => {
        let children = [];

        for (let i = 0; i < array.length; i++)
            if (parentId === -1) {
                if (array[i].parentid === parentId) {
                    return this.createTreeObject(array[i].id, array);
                }
            } else {
                if (array[i].parentid === parentId)
                    children.push(this.createTreeObject(array[i].id, array));
            }


        return children;
    };

    createTreeObject = (name, array) => {
        return {
            name: name,
            textProps: {x: -20, y: 25},
            children: this.createTree(array, name)
        }
    };

    handleClick = (event, node) => {
        this.setState({
            info: <Info node={node} refresh={!this.state.refreshInfo}/>,
            refreshInfo: !this.state.refreshInfo
        })
    };

    render() {
        return (
            <div>
                <p>Click on a node to view its current applications</p>
                <Tree data={this.state.topology}
                      nodeRadius={15}
                      margins={{top: 20, bottom: 10, left: 20, right: 200}}
                      height={1000}
                      width={1080}
                      gProps={{
                          className: 'node',
                          onClick: this.handleClick
                      }}
                />
                {this.state.info}
            </div>
        );
    }
}

class Info extends Component {
    constructor(props) {
        super(props);
        this.state = {
            message: [],
            isOpen: true
        };
    }

    componentWillReceiveProps(props, nextContext) {
        if (props.refresh !== this.props.refresh)
            this.setState({isOpen: true, message: []}, () => this.componentDidMount());
    }

    componentDidMount() {
        fetch('/api/v1/application_management')
            .then(response => response.json())
            .then(message => {
                const filtered = message.filter(i => i.nodeid === this.props.node);

                let messages = [];

                for (let i = 0; i < filtered.length; i++)
                    messages.push(this.createMessageObject(filtered[i]));


                this.setState({message: this.formatClickEven(messages), currentMessage: this.props.node});
            });
    }

    createMessageObject = (object) => {
        return {
            nodeid: object.nodeid,
            application: object.keyspace_name,
            status: object.process_status,
            waiting: object.wait_for,
            jobUUID: object.process_uuid
        }
    };

    formatClickEven = (messages) => {

        let response = [];

        response.push(
            <thead key={0}>
            <tr>
                <th>Nodeid</th>
                <th>Application</th>
                <th>Status</th>
                <th>Waiting</th>
                <th>Job UUID</th>
            </tr>
            </thead>
        );

        let body = [];

        for (let i = 0; i < messages.length; i++) {

            let currentObject = messages[i];

            body.push(
                <tr>
                    <td>{currentObject.nodeid}</td>
                    <td>{currentObject.application}</td>
                    <td>{currentObject.status}</td>
                    <td>{currentObject.waiting}</td>
                    <td>{currentObject.jobUUID}</td>
                </tr>)
        }

        response.push(
            <tbody key={1}>
            {body}
            </tbody>
        );

        return response;
    };

    closeModal = () => {
        this.setState({isOpen: false});
    };

    render() {
        return (
            <Modal isOpen={this.state.isOpen}>
                <Table>{this.state.message}</Table>
                <button onClick={this.closeModal}>close</button>
            </Modal>
        );
    }
}

class ApplicationCreation extends Component {

    constructor(props) {
        super(props);

        this.state = {
            applications: [],
            refresh: false,
            loadingModal: false
        };
    }

    componentWillReceiveProps(props, nextContext) {
        if (this.props.refresh !== props.refresh) {
            this.setState({applications: props.applications, refresh: !this.state.refreshInfo})
        }
    }


    refreshComponents = () => {
        this.props.forceRefresh();
        this.setState({refresh: !this.state.refresh, loadingModel: false});
    };

    spawnLoadingModel = () => {
        this.setState({loadingModel: true});
    };

    render() {
        return (
            <div>
                <h2>Application Creation</h2>
                <DisplayAvailableApplication applications={this.state.applications} refresh={this.state.refresh}/>
                <ApplicationLoader applications={this.state.applications} refresh={this.state.refresh}
                                   forceRefresh={this.refreshComponents} spawnLoadingModel={this.spawnLoadingModel}/>
                <LoadingModel show={this.state.loadingModel}/>
            </div>
        )
    }
}

class DisplayAvailableApplication extends Component {

    constructor(props) {
        super(props);
        this.state = {
            message: [],
            applications: this.props.applications
        };
    }

    componentDidMount() {
        let response = [];

        response.push(
            <thead key={0}>
            <tr>
                <th>Application Name</th>
                <th>TODO</th>
            </tr>
            </thead>
        );

        let body = [];

        for (let i = 0; i < this.state.applications.length; i++) {
            body.push(
                <tr>
                    <td>{this.state.applications[i].application}</td>
                </tr>
            )
        }

        response.push(
            <tbody key={1}>
            {body}
            </tbody>
        );

        this.setState({message: response});
    }


    componentWillReceiveProps(props, nextContext) {
        if (this.props.refresh !== props.refresh)
            this.setState({applications: props.applications}, () => this.componentDidMount());

    }


    render() {
        return (
            <div>
                <p>Available applications</p>
                <Table>
                    {this.state.message}
                </Table>
            </div>
        )
    }
}

/**
 * TODO: Handle errors
 */
class ApplicationLoader extends Component {

    constructor(props) {
        super(props);
        this.state = {
            file: null,
            applications: []
        };
    }

    componentWillReceiveProps(props, nextContext) {
        if (this.props.refresh !== props.refresh)
            this.setState({applications: props.applications});

    }

    handleFileSubmit = (event) => {
        this.setState({file: event.target.files[0]})
    };

    onFormSubmit = (event) => {
        event.preventDefault();

        const applicationName = event.target.elements.application.value.trim();

        if (applicationName === "") {
            alert("You must specify an application name");
            return;
        }

        if (!applicationName.startsWith("pathstore_")) {
            event.target.elements.application.value = null;
            alert("Your application name must start with \"pathstore_\"");
            return;
        }

        for (let i = 0; i < this.state.applications.length; i++) {
            if (this.state.applications[i].application === applicationName) {
                event.target.elements.application.value = null;
                alert("The application name you specified has already been created");
                return;
            }
        }

        if (this.state.file == null) {
            alert("You must specify a file");
            return;
        }

        this.props.spawnLoadingModel();

        const formData = new FormData();

        formData.append("applicationName", applicationName);
        formData.append("applicationSchema", this.state.file);

        fetch("/api/v1/applications", {
            method: 'POST',
            body: formData
        }).then(ignored => {
            this.props.forceRefresh();
        });
    };

    render() {
        return (
            <Form onSubmit={this.onFormSubmit}>
                <Form.Group controlId="application">
                    <Form.Label>Application Name</Form.Label>
                    <Form.Control type="text" placeholder="Enter application name here"/>
                    <Form.Text className="text-muted">
                        Make sure your application name starts with 'pathstore_' and your cql file / keyspace name
                        matches the application name
                    </Form.Text>
                </Form.Group>
                <Form.File
                    id="custom-file-translate-scss"
                    label="Custom file input"
                    lang="en"
                    custom
                    onChange={this.handleFileSubmit}
                />
                <Button variant="primary" type="submit">
                    Submit
                </Button>
            </Form>
        )
    }
}

/**
 * TODO: Show some spinning image or something
 */
class LoadingModel extends Component {
    constructor(props) {
        super(props);
        this.state = {
            show: props.show
        }
    }

    componentWillReceiveProps(props, nextContext) {
        if (this.props.show !== props.show) {
            this.setState({show: props.show});
        }
    }

    render() {
        return (
            <Modal isOpen={this.state.show}>
                <p>Loading</p>
            </Modal>
        );
    }
}

class ApplicationInstallation extends Component {

    constructor(props) {
        super(props);
        this.state = {
            topology: props.topology,
            applications: props.application,
            childRefresh: false
        };
    }

    componentWillReceiveProps(props, nextContext) {
        if (this.props.refresh !== props.refresh)
            this.setState({
                applications: props.applications,
                topology: props.topology,
                childRefresh: !this.state.childRefresh
            });

    }


    render() {
        return (
            <div>
                <h2>Application Deployment</h2>
                <DeployApplication topology={this.state.topology} applications={this.state.applications}
                                   refresh={this.state.childRefresh}/>
            </div>
        )
    }

}

class DeployApplication extends Component {
    constructor(props) {
        super(props);
        this.state = {
            topology: [],
            applications: [],
            application: '',
            nodes: []
        };
    }

    componentDidMount() {
        if (this.state.applications.length === 1)
            this.setState({application: this.state.applications[0].application});

    }

    componentWillReceiveProps(props, nextContext) {
        if (this.props.refresh !== props.refresh)
            this.setState({
                topology: this.parseTopology(props.topology),
                applications: props.applications
            }, () => this.componentDidMount());
    }

    parseTopology = (topology) => {
        let array = [];

        for (let i = 0; i < topology.length; i++)
            array.push(topology[i].id);

        return array.sort((a, b) => a > b ? 1 : -1);
    };

    onApplicationChange = (event) => {
        event.preventDefault();
        this.setState({application: event.target.value});
    };

    onNodeChange = (event) => {
        event.preventDefault();

        let nodes = [];

        for (let i = 0; i < event.target.options.length; i++)
            if (event.target.options[i].selected)
                nodes.push(parseInt(event.target.options[i].value));


        this.setState({nodes: nodes});
    };

    onFormSubmit = (event) => {
        event.preventDefault();

        let url = "/api/v1/application_management?";

        for (let i = 0; i < this.state.nodes.length; i++) {
            if (i === 0)
                url += "node=" + this.state.nodes[i];
            else
                url += "&node=" + this.state.nodes[i];
        }

        url += "&applicationName=" + this.state.application;

        fetch(url, {
            method: 'POST'
        }).then(ignored => {
        });
    };

    render() {

        const applications = [];

        for (let i = 0; i < this.state.applications.length; i++)
            applications.push(
                <option key={i}>{this.state.applications[i].application}</option>
            );


        const nodes = [];

        for (let i = 0; i < this.state.topology.length; i++)
            nodes.push(
                <option key={i}>{this.state.topology[i]}</option>
            );

        return (
            <Form onSubmit={this.onFormSubmit}>
                <Form.Group controlId="exampleForm.ControlSelect2">
                    <Form.Label>Select Application</Form.Label>
                    <Form.Control as="select" single onChange={this.onApplicationChange} value={this.state.application}>
                        {applications}
                    </Form.Control>
                </Form.Group>
                <Form.Group controlId="exampleForm.ControlSelect2">
                    <Form.Label>Select Nodes</Form.Label>
                    <Form.Control as="select" multiple onChange={this.onNodeChange}>
                        {nodes}
                    </Form.Control>
                </Form.Group>
                <Button variant="primary" type="submit">
                    Submit
                </Button>
            </Form>
        );
    }
}

/**
 * TODO: Query topology and available applications here
 */
class Login extends Component {

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

export default Login;
