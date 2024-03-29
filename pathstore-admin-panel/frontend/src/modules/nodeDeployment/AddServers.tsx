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

import React, {FunctionComponent, useCallback, useContext} from "react";
import {LoadingModalContext} from "../../contexts/LoadingModalContext";
import {ErrorModalContext} from "../../contexts/ErrorModalContext";
import {APIContext} from "../../contexts/APIContext";
import {ServerCreationResponseModalContext} from "../../contexts/ServerCreationResponseModalContext";
import {webHandler} from "../../utilities/Utils";
import {Server, SERVER_AUTH_TYPE} from "../../utilities/ApiDeclarations";
import {ServerForm} from "./ServerForm";
import {SubmissionErrorModalProvider} from "../../contexts/SubmissionErrorModalContext";

/**
 * This component is used to render a form to add a server to the server pool
 * @constructor
 */
export const AddServers: FunctionComponent = () => {

    // load force refresh
    const {forceRefresh} = useContext(APIContext);

    // load the loading modal context
    const loadingModal = useContext(LoadingModalContext);

    // load error modal context
    const errorModal = useContext(ErrorModalContext);

    // load show from server creation response modal context
    const {show} = useContext(ServerCreationResponseModalContext);

    /**
     * Load all data from form and make the api call.
     *
     * @param ip ip of server
     * @param username username of server to connect
     * @param authType authType used for server
     * @param privateKey private key file
     * @param passphrase optional passphrase
     * @param password password of server to connect
     * @param ssh_port ssh port to connect on
     * @param grpc_port grpc port to host grpc server
     * @param name human readable name of server
     * @param clearForm ability to clear form
     */
    const onFormSubmit = useCallback((
        ip: string | undefined,
        username: string | undefined,
        authType: string | undefined,
        privateKey: File | undefined,
        passphrase: string | undefined,
        password: string | undefined,
        ssh_port: number | undefined,
        grpc_port: number | undefined,
        name: string | undefined,
        clearForm: () => void
    ): void => {

        if (loadingModal.show && loadingModal.close && errorModal.show && forceRefresh) {

            if (ip && username && name) {
                let formData = new FormData();
                formData.append("ip", ip);
                formData.append("username", username);
                formData.append("ssh_port", (ssh_port === undefined ? 22 : ssh_port).toString());
                formData.append("grpc_port", (grpc_port === undefined ? 1099 : grpc_port).toString());
                formData.append("name", name);


                if (authType === "Password" && password) {
                    formData.append("auth_type", SERVER_AUTH_TYPE[SERVER_AUTH_TYPE.PASSWORD]);
                    formData.append("password", password);
                } else if (authType === "Key" && privateKey) {
                    formData.append("auth_type", SERVER_AUTH_TYPE[SERVER_AUTH_TYPE.IDENTITY]);
                    formData.append("privateKey", privateKey);

                    if (passphrase)
                        formData.append("passphrase", passphrase);
                } else {
                    alert("ERROR in add server call");
                    return;
                }

                loadingModal.show();
                fetch("/api/v1/servers", {
                    method: 'POST',
                    body: formData
                })
                    .then(webHandler)
                    .then((s: Server) => {
                        if (show)
                            show(s);
                        clearForm();
                        forceRefresh();
                    })
                    .catch(errorModal.show)
                    .finally(loadingModal.close);
            }
        }
    }, [loadingModal, errorModal, forceRefresh, show]);

    return (
        <>
            <h3>Create Server</h3>
            <SubmissionErrorModalProvider>
                <ServerForm onFormSubmitCallback={onFormSubmit}/>
            </SubmissionErrorModalProvider>
        </>
    );
};