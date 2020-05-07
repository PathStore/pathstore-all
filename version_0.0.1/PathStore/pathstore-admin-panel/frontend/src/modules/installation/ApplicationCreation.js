import ReactDOM from 'react-dom'
import React, {Component} from "react";
import Form from "react-bootstrap/Form";
import Button from "react-bootstrap/Button";
import ApplicationCreationResponseModal from "./ApplicationCreationResponseModal";
import LoadingModal from "../LoadingModal";
import {webHandler} from "../Utils";
import ErrorResponseModal from "../ErrorResponseModal";

export default class ApplicationCreation extends Component {

    constructor(props) {
        super(props);
        this.state = {
            file: null,
            loadingModalShow: false,
            responseModalShow: false,
            responseModalData: null,
            responseModalError: false
        };
    }

    handleFileSubmit = (event) => this.setState({file: event.target.files[0]});

    closeModalCallback = () => this.setState({responseModalShow: false});

    onFormSubmit = (event) => {
        event.preventDefault();

        const applicationName = event.target.elements.application.value.trim();

        const formData = new FormData();

        formData.append("applicationName", applicationName);

        if (this.state.file != null)
            formData.append("applicationSchema", this.state.file);

        this.setState({loadingModalShow: true}, () => {
            fetch("/api/v1/applications", {
                method: 'POST',
                body: formData
            }).then(webHandler)
                .then(response => {
                    this.setState({loadingModalShow: false}, () => {
                        this.props.forceRefresh();
                        ReactDOM.findDOMNode(this.messageForm).reset();
                        this.setState({
                            responseModalShow: true,
                            responseModalData: response,
                            responseModalError: false
                        });
                    });
                }).catch(response => {
                this.setState({loadingModalShow: false}, () => {
                    this.props.forceRefresh();
                    ReactDOM.findDOMNode(this.messageForm).reset();
                    this.setState({
                        responseModalShow: true,
                        responseModalData: response,
                        responseModalError: true
                    });
                });
            })
        });
    };

    render() {

        const loadingModal = (this.state.loadingModalShow ? <LoadingModal show={this.state.loadingModalShow}/> : null);

        const responseModal = this.state.responseModalShow ?
            this.state.responseModalError ?
                <ErrorResponseModal show={this.state.responseModalShow}
                                                  data={this.state.responseModalData}
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
                <Form onSubmit={this.onFormSubmit} ref={form => this.messageForm = form}>
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
            </div>
        )
    }
}