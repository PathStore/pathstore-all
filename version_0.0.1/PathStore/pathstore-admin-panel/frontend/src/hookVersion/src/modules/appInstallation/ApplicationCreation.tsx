import React, {FunctionComponent, useCallback, useContext, useRef, useState} from "react";
import {Button, Form} from "react-bootstrap";
import {webHandler} from "../../../../utilities/Utils";
import {APIContext} from "../../contexts/APIContext";
import {LoadingModalContext} from "../../contexts/LoadingModalContext";
import {ErrorModalContext} from "../../contexts/ErrorModalContext";
import {ApplicationCreationSuccess} from "../../../../utilities/ApiDeclarations";
import {SubmissionErrorModalContext} from "../../contexts/SubmissionErrorModalContext";

/**
 * This component is used to load an application into the network. This component must be used before deploying an application
 * as deploying an application depends on this component
 *
 * @constructor
 */
export const ApplicationCreation: FunctionComponent = () => {

    // Load force refresh from api context
    const {forceRefresh} = useContext(APIContext);

    // load the loading modal context to display on submission
    const loadingModal = useContext(LoadingModalContext);

    // load the error modal context to display error if an error occurs during submission
    const errorModal = useContext(ErrorModalContext);

    // load the submission error modal to inform the user if they've inputted invalid data
    const submissionErrorModal = useContext(SubmissionErrorModalContext);

    // State to store the file TODO: Find proper type
    const [file, setFile] = useState<File | null>(null);

    // Form ref
    const formRef = useRef<HTMLFormElement>(null);

    /**
     * This function will send the given file and application name to the api on submission.
     *
     * If either the name is blank or the file is null inform the user they must submit both of those parameters
     */
    const onFormSubmit = useCallback((event: any): void => {
        event.preventDefault();

        const applicationName = event.target.elements.application.value.trim();

        if (submissionErrorModal.show) {

            if (applicationName.trim() === "" || file == null) {
                submissionErrorModal.show("You must submit an application and upload a schema");
                return;
            }

            const formData = new FormData();

            formData.append("application_name", applicationName);
            formData.append("applicationSchema", file);

            if (loadingModal.show && loadingModal.close && errorModal.show) {
                loadingModal.show();
                fetch("/api/v1/applications", {
                    method: 'POST',
                    body: formData
                })
                    .then(webHandler)
                    .then((response: ApplicationCreationSuccess) => {
                        alert(response.keyspace_created);
                        if (formRef.current)
                            formRef.current.reset();
                        if (forceRefresh)
                            forceRefresh();
                    }) // Show creation modal
                    .catch(errorModal.show)
                    .finally(loadingModal.close);
            }
        }
    }, [file, formRef, forceRefresh, loadingModal, errorModal, submissionErrorModal]);

    /**
     * This callback is used to take the inputted file and stored it inside the internal state
     */
    const updateFile = useCallback((event: any) => setFile(event.target.files[0]), [setFile]);

    return (
        <Form onSubmit={onFormSubmit} ref={formRef}>
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
                    onChange={updateFile}
                />
            </Form.Group>
            <Button variant="primary" type="submit">
                Submit
            </Button>
        </Form>
    );
};