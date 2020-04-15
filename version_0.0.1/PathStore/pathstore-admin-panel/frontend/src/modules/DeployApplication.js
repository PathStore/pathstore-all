import React, {Component} from "react";
import Form from "react-bootstrap/Form";
import Button from "react-bootstrap/Button";

export default class DeployApplication extends Component {
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