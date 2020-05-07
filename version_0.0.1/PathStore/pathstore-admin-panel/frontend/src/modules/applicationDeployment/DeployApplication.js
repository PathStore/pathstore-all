import ReactDOM from 'react-dom'
import React, {Component} from "react";
import Form from "react-bootstrap/Form";
import Button from "react-bootstrap/Button";
import DeployApplicationResponseModal from "./DeployApplicationResponseModal";
import {webHandler} from "../Utils";
import ErrorResponseModal from "../ErrorResponseModal";

/**
 * This component is used to allow the user to select a set of nodes to install an application on
 * It has a form which has two select dropdowns which are created using the data passed from PathStoreControlPanel
 *
 * Props:
 * topology: list of deployment objects gathered from api
 * applications: list of application objects gathered from api
 * applicationStatus: list of application status objects gathered from api
 */
export default class DeployApplication extends Component {

    /**
     * State:
     * responseModalData: data to passed to response modal on request submit
     * responseModalShow: whether or not to show the response modal
     * responseModalError: whether to display the error response modal
     *
     * @param props
     */
    constructor(props) {
        super(props);
        this.state = {
            responseModalData: null,
            responseModalShow: false,
            responseModalError: false
        };
    }

    /**
     * Gathers data from form submit and submits an api request.
     *
     * If success then the values are written to (responseModalData, responseModalShow, responseModalError) respectively
     * as (response from api, true, false)
     *
     * If failed then the values are written respecitvely as (response from api, true, true)
     *
     * @param event
     */
    onFormSubmit = (event) => {
        event.preventDefault();

        const application = event.target.elements.application.value.trim();

        const nodes = [];

        for (let i = 0; i < event.target.elements.nodes.options.length; i++)
            if (event.target.elements.nodes.options[i].selected)
                nodes.push(parseInt(event.target.elements.nodes.options[i].value));

        let url = "/api/v1/application_management?";

        for (let i = 0; i < nodes.length; i++) {
            if (i === 0)
                url += "node=" + nodes[i];
            else
                url += "&node=" + nodes[i];
        }

        url += "&applicationName=" + application;

        fetch(url, {
            method: 'POST'
        }).then(webHandler)
            .then(response => {
                ReactDOM.findDOMNode(this.messageForm).reset();
                this.setState({
                    responseModalData: response,
                    responseModalShow: true,
                    responseModalError: false,
                });
            }).catch(response =>
            this.setState({
                responseModalData: response,
                responseModalShow: true,
                responseModalError: true,
            })
        );
    };

    /**
     * Callback given to the response modal to all it to close when the user clicks close.
     * This allows for garbage collection of the component
     */
    closeModalCallback = () => this.setState({responseModalShow: false});

    /**
     * First we figure out of a modal needs to be rendered
     *
     * Second we gather all application options
     *
     * Thirdly we gather all node options
     *
     * Finally if application options exist we render the form else we inform the user there are no application to deploy
     *
     * @returns {*}
     */
    render() {

        const modal =
            this.state.responseModalShow ?
                this.state.responseModalError ?
                    <ErrorResponseModal data={this.state.responseModalData}
                                        show={this.state.responseModalShow}
                                        callback={this.closeModalCallback}/>
                    :
                    <DeployApplicationResponseModal data={this.state.responseModalData}
                                                    show={this.state.responseModalShow}
                                                    applicationName={this.state.application}
                                                    topology={this.props.topology}
                                                    applicationStatus={this.props.applicationStatus}
                                                    callback={this.closeModalCallback}/>
                : null;

        const applications = [];

        for (let i = 0; i < this.props.applications.length; i++)
            applications.push(
                <option key={i}>{this.props.applications[i].keyspace_name}</option>
            );


        const nodes = [];

        for (let i = 0; i < this.props.topology.length; i++)
            if (this.props.topology[i].process_status === "DEPLOYED")
                nodes.push(
                    <option key={i}>{this.props.topology[i].new_node_id}</option>
                );

        let form = this.props.applications.length > 0 ?
            <Form onSubmit={this.onFormSubmit} ref={form => this.messageForm = form}>
                <Form.Group controlId="application">
                    <Form.Label>Select Application</Form.Label>
                    <Form.Control as="select">
                        {applications}
                    </Form.Control>
                </Form.Group>
                <Form.Group controlId="nodes">
                    <Form.Label>Select Nodes</Form.Label>
                    <Form.Control as="select" multiple>
                        {nodes}
                    </Form.Control>
                </Form.Group>
                <Button variant="primary" type="submit">
                    Submit
                </Button>
            </Form>
            : <p>There are no applications installed on the network</p>;

        return (
            <div>
                {modal}
                {form}
            </div>
        );
    }
}