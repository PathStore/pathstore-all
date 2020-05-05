import ReactDOM from 'react-dom'
import React, {Component} from "react";
import Form from "react-bootstrap/Form";
import Button from "react-bootstrap/Button";
import DeployApplicationResponseModal from "./DeployApplicationResponseModal";

/**
 * This model is used to deploy an application on a set of nodes.
 * This model does not allow for impossible entices. I.e. we force the user to only select applications that are available
 * and we force the user to select only nodes that are part of the topology.
 */
export default class DeployApplication extends Component {

    /**
     * Reference to form object so we can reset it on successful submit
     */
    messageForm = null;

    /**
     * State:
     *
     * topology: list of node id's that are valid
     * applications: applications from api
     * application: currently selected application
     * nodes: currently select list of nodes
     * responseModalData: data gathered from the installation end point call
     * responseModalShow: whether to show the modal or not
     *
     * @param props
     */
    constructor(props) {
        super(props);
        this.state = {
            topology: this.parseTopology(props.topology),
            application: '',
            nodes: [],
            responseModalData: null,
            responseModalShow: false,
            responseModalType: null
        };
    }

    /**
     * If there is only one application we need to update application manually because the onchange function will not trigger
     * for a select block that only has one element.
     */
    componentDidMount() {
        if (this.props.applications !== undefined && this.props.applications[0] !== undefined)
            this.setState({application: this.props.applications[0].application});
    }

    /**
     * Allows for the parent to update children contents. We must all reparse their response for our use
     *
     * @param props
     * @param nextContext
     */
    componentWillReceiveProps(props, nextContext) {
        if (this.props.refresh !== props.refresh || this.props.applications !== props.applications)
            this.setState({
                topology: this.parseTopology(props.topology),
            }, () => this.componentDidMount());
    }

    /**
     * Since we only care about the id we will create an array of integers in increasing order
     *
     * @param topology
     * @returns array is sorted in min -> max fashion
     */
    parseTopology = (topology) => {
        let array = [];

        for (let i = 0; i < topology.length; i++)
            if (topology[i].processStatus === "DEPLOYED")
                array.push(topology[i].id);

        return array.sort((a, b) => a > b ? 1 : -1);
    };

    /**
     * On Change event for application, only gets called if > 1 application exists
     *
     * @param event
     */
    onApplicationChange = (event) => {
        event.preventDefault();
        this.setState({application: event.target.value});
    };

    /**
     * On Change event for node selection. We take all the select nodes and put it into an integer array
     * and update the state accordingly
     *
     * @param event
     */
    onNodeChange = (event) => {
        event.preventDefault();

        let nodes = [];

        for (let i = 0; i < event.target.options.length; i++)
            if (event.target.options[i].selected)
                nodes.push(parseInt(event.target.options[i].value));


        this.setState({nodes: nodes});
    };

    /**
     * This function is called when the user submits a request to install the application
     * we build the url and make a request to the backend
     *
     * @param event
     */
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

        //TODO: Handle errors using catch
        fetch(url, {
            method: 'POST'
        }).then(response => response.json())
            .then(response => {
                ReactDOM.findDOMNode(this.messageForm).reset();
                this.setState({
                    responseModalData: response,
                    responseModalShow: true,
                    responseModalType: Array.isArray(response) ? response[0].error === undefined ? 0 : 1 : 2
                });
            });
    };

    /**
     * Callback function for the DeployApplicationResponseModal to reset the parent show attribute
     */
    closeModalCallback = () => this.setState({responseModalShow: false});

    /**
     * We build select statements for both the application and nodes.
     *
     * application is a single select (can only select one)
     * nodes is a multi select (can select more than one)
     *
     * and we then display the form
     *
     * @returns {*}
     */
    render() {

        const applications = [];

        for (let i = 0; i < this.props.applications.length; i++)
            applications.push(
                <option key={i}>{this.props.applications[i].application}</option>
            );


        const nodes = [];

        for (let i = 0; i < this.state.topology.length; i++)
            nodes.push(
                <option key={i}>{this.state.topology[i]}</option>
            );

        const modal = (this.state.responseModalShow ?
            <DeployApplicationResponseModal data={this.state.responseModalData} show={this.state.responseModalShow}
                                            type={this.state.responseModalType}
                                            topology={this.props.topology}
                                            callback={this.closeModalCallback}/> : null);

        let form;

        if (this.props.applications.length > 0)
            form = <Form onSubmit={this.onFormSubmit} ref={form => this.messageForm = form}>
                <Form.Group controlId="exampleForm.ControlSelect2">
                    <Form.Label>Select Application</Form.Label>
                    <Form.Control as="select" single onChange={this.onApplicationChange}
                                  value={this.state.application}>
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
            </Form>;
        else
            form = <p>There are no applications installed on the network</p>;

        return (
            <div>
                {modal}
                {form}
            </div>
        );
    }
}