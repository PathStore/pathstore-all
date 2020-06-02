import React, {Component, RefObject} from "react";
import {Application, ApplicationStatus, Deployment, Server, Error} from "../../utilities/ApiDeclarations";
import {webHandler} from "../../utilities/Utils";
import {ErrorResponseModal} from "../ErrorResponseModal";
import {Button, Form} from "react-bootstrap";
import DeployApplicationResponseModal from "./DeployApplicationResponseModal";

/**
 * Properties definition for {@link DeployApplication}
 */
interface DeployApplicationProperties {
    /**
     * List of deployment objects from api
     */
    readonly deployment: Deployment[]

    /**
     * List of application objects from api
     */
    readonly applications: Application[]

    /**
     * List of node status objects from api
     */
    readonly applicationStatus: ApplicationStatus[]

    /**
     * List of server objects from api
     */
    readonly servers: Server[]
}

/**
 * State definition for {@link DeployApplication}
 */
interface DeployApplicationState {
    /**
     * Application name to send to modal
     */
    readonly responseModalApplication: string

    /**
     * List of new deployed nodes (response from post call)
     */
    readonly responseModalData: ApplicationStatus[]

    /**
     * Whether to show the response modal or not
     */
    readonly responseModalShow: boolean

    /**
     * Whether to show the error modal or not
     */
    readonly responseModalError: boolean

    /**
     * List of errors to give the error modal if needed
     */
    readonly responseModalErrorData: Error[]
}

/**
 * This component is used to allow the user to select a set of nodes to install an application on
 * It has a form which has two select dropdowns which are created using the data passed from PathStoreControlPanel
 */
export default class DeployApplication extends Component<DeployApplicationProperties, DeployApplicationState> {

    /**
     * Used to clear form
     */
    private messageForm: RefObject<HTMLFormElement> = React.createRef();

    /**
     * Initialize props and state
     *
     * @param props
     */
    constructor(props: DeployApplicationProperties) {
        super(props);

        this.state = {
            responseModalApplication: "",
            responseModalData: [],
            responseModalShow: false,
            responseModalError: false,
            responseModalErrorData: []
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
    onFormSubmit = (event: any): void => {
        event.preventDefault();

        const application = event.target.elements.application.value.trim();

        const nodes = [];

        for (let i = 0; i < event.target.elements.nodes.options.length; i++)
            if (event.target.elements.nodes.options[i].selected)
                nodes.push(parseInt(event.target.elements.nodes.options[i].value));

        let url = "/api/v1/application_management?";

        for (let i = 0; i < nodes.length; i++) {
            if (i === 0)
                url += "nodes=" + nodes[i];
            else
                url += "&nodes=" + nodes[i];
        }

        url += "&application_name=" + application;

        fetch(url, {
            method: 'POST'
        })
            .then(webHandler)
            .then((response: ApplicationStatus[]) => {
                this.messageForm.current?.reset();
                this.setState({
                    responseModalApplication: application,
                    responseModalData: response,
                    responseModalShow: true,
                    responseModalError: false,
                });
            })
            .catch((response: Error[]) =>
                this.setState({
                    responseModalErrorData: response,
                    responseModalShow: true,
                    responseModalError: true,
                })
            );
    };

    /**
     * Callback given to the response modal to all it to close when the user clicks close.
     * This allows for garbage collection of the component
     */
    closeModalCallback = (): void => this.setState({responseModalShow: false});

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
                    <ErrorResponseModal data={this.state.responseModalErrorData}
                                        show={this.state.responseModalShow}
                                        callback={this.closeModalCallback}/>
                    :
                    <DeployApplicationResponseModal data={this.state.responseModalData}
                                                    show={this.state.responseModalShow}
                                                    applicationName={this.state.responseModalApplication}
                                                    deployment={this.props.deployment}
                                                    applicationStatus={this.props.applicationStatus}
                                                    servers={this.props.servers}
                                                    callback={this.closeModalCallback}/>
                : null;

        const applications = [];

        for (let i = 0; i < this.props.applications.length; i++)
            applications.push(
                <option key={i}>{this.props.applications[i].keyspace_name}</option>
            );


        const nodes = [];

        for (let i = 0; i < this.props.deployment.length; i++)
            if (this.props.deployment[i].process_status === "DEPLOYED")
                nodes.push(
                    <option key={i}>{this.props.deployment[i].new_node_id}</option>
                );

        let form = this.props.applications.length > 0 ?
            <Form onSubmit={this.onFormSubmit} ref={this.messageForm}>
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