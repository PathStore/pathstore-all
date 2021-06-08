/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

import React, {FunctionComponent, useCallback, useContext, useRef, useState} from "react";
import {Button, Form} from "react-bootstrap";
import {APIContext} from "../../contexts/APIContext";
import {LoadingModalContext} from "../../contexts/LoadingModalContext";
import {ErrorModalContext} from "../../contexts/ErrorModalContext";
import {SubmissionErrorModalContext} from "../../contexts/SubmissionErrorModalContext";
import {webHandler} from "../../utilities/Utils";

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

    // name of file which was uploaded
    const [fileName, setFileName] = useState<string>("");

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

        const masterPassword = event.target.elements.password.value.trim();

        const masterPasswordConfirmation = event.target.elements.password_confirmation.value.trim();

        const clientLeaseTime = parseInt(event.target.elements.client_lease_time.value.trim());

        const serverAdditionalTime = parseInt(event.target.elements.server_additional_time.value.trim());

        if (submissionErrorModal.show) {

            if (applicationName === "" || file == null || masterPassword === "" || masterPasswordConfirmation === "" || clientLeaseTime <= 0 || serverAdditionalTime <= 0) {
                submissionErrorModal.show("You must submit an application, password, password confirmation, client lease time, additional server time, and upload a schema");
                return;
            }

            if (masterPassword !== masterPasswordConfirmation) {
                submissionErrorModal.show("Master passwords entered do not match");
                return;
            }

            const formData = new FormData();

            formData.append("application_name", applicationName);
            formData.append("applicationSchema", file);
            formData.append("master_password", masterPassword);
            formData.append("client_lease_time", clientLeaseTime.toString());
            formData.append("server_additional_time", serverAdditionalTime.toString());

            if (loadingModal.show && loadingModal.close && errorModal.show) {
                loadingModal.show();
                fetch("/api/v1/applications", {
                    method: 'POST',
                    body: formData
                })
                    .then(webHandler)
                    .then(() => {
                        if (formRef.current)
                            formRef.current.reset();
                        setFileName("");
                        if (forceRefresh)
                            forceRefresh();
                    }) // Show creation modal
                    .catch(errorModal.show)
                    .finally(loadingModal.close);
            }
        }
    }, [file, formRef, setFileName, forceRefresh, loadingModal, errorModal, submissionErrorModal]);

    /**
     * This callback is used to take the inputted file and stored it inside the internal state
     */
    const updateFile = useCallback((event: any) => {
        const file: File = event.target.files[0];
        setFile(file);
        setFileName(file.name);
    }, [setFile, setFileName]);

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
            <Form.Group controlId="password">
                <Form.Label>Master Password</Form.Label>
                <Form.Control type="password" placeholder="Enter master password here"/>
                <Form.Text>
                    This password will be used to allow your clients to connect to a pathstore node.
                </Form.Text>
            </Form.Group>
            <Form.Group controlId="password_confirmation">
                <Form.Label>Master Password</Form.Label>
                <Form.Control type="password" placeholder="Re-enter master password here"/>
                <Form.Text>
                    Re-type password as you did above
                </Form.Text>
            </Form.Group>
            <Form.Group controlId="client_lease_time">
                <Form.Label>Client Lease Time</Form.Label>
                <Form.Control type="text" placeholder="10 seconds is on the low end, a minute is on the high end"/>
                <Form.Text>
                    This time entered here denotes how long a client side query is valid for. Once their lease is
                    expired the client must communicate with their local node to issue a new lease. (Units in
                    milliseconds)
                </Form.Text>
            </Form.Group>
            <Form.Group controlId="server_additional_time">
                <Form.Label>Server Additional Time</Form.Label>
                <Form.Control type="text"
                              placeholder="This should be a constance factor times by your entered client lease time"/>
                <Form.Text>
                    The server lease time is the entered client lease time plus this provided value. As in even if a
                    client lease is expired that does not mean the server garbage collects that data. This must be a
                    integer greater than zero. (Units in milliseconds)
                </Form.Text>
            </Form.Group>
            <Form.Group>
                <Form.Label>Application File</Form.Label>
                <Form.File
                    label={fileName}
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