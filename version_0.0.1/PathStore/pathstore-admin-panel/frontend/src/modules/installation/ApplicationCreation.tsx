import React, {Component} from "react";
import ReactDOM from 'react-dom'
import {ApplicationCreationSuccess, Error} from "../../utilities/ApiDeclarations";
import {webHandler} from "../../utilities/Utils";
import {LoadingModal} from "../LoadingModal";
import {ErrorResponseModal} from "../ErrorResponseModal";
import {Button, Form} from "react-bootstrap";
import {ApplicationCreationResponseModal} from "./ApplicationCreationResponseModal";

/**
 * Props definition for {@link ApplicationCreation}
 */
interface ApplicationCreationProperties {
    /**
     * Callback function to refresh components props
     */
    readonly forceRefresh: () => void
}

/**
 * State definition for {@link ApplicationCreation}
 */
interface ApplicationCreationState {
    /**
     * File to send to api on submission
     */
    readonly file: File | null

    /**
     * Whether or not to show the loading modal
     */
    readonly loadingModalShow: boolean

    /**
     * Whwther or not to show the response modal
     */
    readonly responseModalShow: boolean

    /**
     * What application was created (api response)
     */
    readonly responseModalData: ApplicationCreationSuccess | null

    /**
     * If api response code is >= 400
     */
    readonly responseModalError: boolean

    /**
     * Api message on error
     */
    readonly  responseModalErrorData: Error[]
}

/**
 * TODO: Maybe add a colour underneath the application name if its valid or not
 *
 * This class is used to create an application by giving a name and a schema
 */
export default class ApplicationCreation extends Component<ApplicationCreationProperties, ApplicationCreationState> {

    /**
     * Used to clear form on completion
     */
    private messageForm: any;

    /**
     * Initialize props and state
     *
     * @param props
     */
    constructor(props: ApplicationCreationProperties) {
        super(props);

        this.state = {
            file: null,
            loadingModalShow: false,
            responseModalShow: false,
            responseModalData: null,
            responseModalError: false,
            responseModalErrorData: []
        };
    }

    /**
     * Take file uploaded by user and store in the state
     *
     * @param event
     */
    handleFileSubmit = (event: any): void => this.setState({file: event.target.files[0]});

    /**
     * Send FormData to api with the following format
     *
     * applicationSchema: file
     * applicationName: name of new application
     *
     * On success render ApplicationCreationResponseModal
     * On failure render ErrorResponseModal
     *
     * @param event
     */
    onFormSubmit = (event: any): void => {
        event.preventDefault();

        const applicationName = event.target.elements.application.value.trim();

        const formData = new FormData();

        formData.append("application_name", applicationName);

        if (this.state.file != null)
            formData.append("applicationSchema", this.state.file);

        this.setState({loadingModalShow: true}, () => {
            fetch("/api/v1/applications", {
                method: 'POST',
                body: formData
            })
                .then(webHandler)
                .then((response: ApplicationCreationSuccess) => {
                    this.props.forceRefresh();
                    // @ts-ignore
                    ReactDOM.findDOMNode(this.messageForm).reset();
                    this.setState({
                        responseModalData: response,
                        responseModalError: false
                    });
                })
                .catch((response: Error[]) => {
                    this.setState({
                        responseModalErrorData: response,
                        responseModalError: true
                    });
                })
                .finally(() => this.setState({responseModalShow: true, loadingModalShow: false}));
        });
    };

    /**
     * Function for response modals to close themselves. Used for gc of modals
     */
    closeModalCallback = (): void => this.setState({responseModalShow: false});

    /**
     * First determine if the loading modal needs to be shown
     *
     * Second determine if the response modal needs to be shown and if so which one to show
     *
     * Finally render any modals and the form
     *
     * @returns {*}
     */
    render() {

        const loadingModal = (this.state.loadingModalShow ? <LoadingModal show={this.state.loadingModalShow}/> : null);

        const responseModal =
            this.state.responseModalShow ?
                this.state.responseModalError ?
                    <ErrorResponseModal show={this.state.responseModalShow}
                                        data={this.state.responseModalErrorData}
                                        callback={this.closeModalCallback}/>
                    :
                    <ApplicationCreationResponseModal show={this.state.responseModalShow}
                                                      data={this.state.responseModalData}
                                                      callback={this.closeModalCallback}/>
                : null;

        return (
            <div>
                {loadingModal}
                {responseModal}
                <Form onSubmit={this.onFormSubmit} ref={(form: any) => this.messageForm = form}>
                    <Form.Group controlId="application">
                        <Form.Label>Application Name</Form.Label>
                        <Form.Control type="plaintext" placeholder="Enter application name here"/>
                        <Form.Text>
                            Make sure your application name starts with 'pathstore_' and your cql file / keyspace name
                            matches the application name
                        </Form.Text>
                    </Form.Group>
                    <Form.Group>
                        <Form.File
                            label="PathStore Application File"
                            lang="en"
                            custom
                            onChange={this.handleFileSubmit}
                        />
                    </Form.Group>
                    <Button variant="primary" type="submit">
                        Submit
                    </Button>
                </Form>
            </div>
        );
    }
}