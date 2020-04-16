import React, {Component} from "react";
import Form from "react-bootstrap/Form";
import Button from "react-bootstrap/Button";

/**
 * TODO: Handle errors
 *
 * This model is used to load an application into the pathstore system
 * based on a file and a name the user has given
 */
export default class ApplicationLoader extends Component {

    /**
     * State:
     *
     * applications: available applications from api
     * file: file that is uploaded
     *
     * @param props
     */
    constructor(props) {
        super(props);
        this.state = {
            file: null,
            applications: []
        };
    }

    /**
     * Updates applications if a refresh occurs (state changing operation in another model occured)
     *
     * @param props
     * @param nextContext
     */
    componentWillReceiveProps(props, nextContext) {
        if (this.props.refresh !== props.refresh)
            this.setState({applications: props.applications});

    }

    /**
     * On Change function for the file handler. Updates state to the file the user passed
     *
     * @param event
     */
    handleFileSubmit = (event) => {
        this.setState({file: event.target.files[0]})
    };

    /**
     * TODO: Handle backend errors
     *
     * Form submission function, this is used to check user input and to execute their request
     *
     * @param event
     */
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

    /**
     * Renders the form
     *
     * @returns {*}
     */
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