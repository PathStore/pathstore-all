import React, {FunctionComponent, ReactElement, useCallback, useContext, useState} from "react";
import {AvailableLogDates, Log} from "../../utilities/ApiDeclarations";
import {Button, Form} from "react-bootstrap";
import {NodeInfoModalContext} from "../../contexts/NodeInfoModalContext";
import {webHandler} from "../../../../utilities/Utils";
import {LoadingModalContext} from "../../contexts/LoadingModalContext";
import {ErrorModalContext} from "../../contexts/ErrorModalContext";

/**
 * This component is used to display logs to the user based on their request
 * @constructor
 */
export const LogViewer: FunctionComponent = () => {
    // Load the node id from the node info modal context
    const {data} = useContext(NodeInfoModalContext);

    // Get loadingModal
    const loadingModal = useContext(LoadingModalContext);

    // Get errorModal
    const errorModal = useContext(ErrorModalContext);

    // Internal state for the log (set when the user submits the form)
    const [log, setLog] = useState<Log | null>(null);

    // Handle the request of the form
    const onFormSubmit = useCallback((event: any): void => {
        if (loadingModal && loadingModal.show && loadingModal.close) {

            event.preventDefault();

            const date = event.target.elements.date.value.trim();

            const log_level = event.target.elements.log_level.value.trim();

            const url = '/api/v1/logs?node_id=' + data?.node + "&date=" + date + "&log_level=" + log_level;

            // show loading
            loadingModal.show();
            fetch(url, {
                method: 'GET',
                headers: {
                    'Accept': 'application/json',
                    'Content-Type': 'application/json'
                }
            })
                .then(webHandler)
                .then(setLog)
                .catch(errorModal.show)
                .finally(loadingModal.close);
        }
    }, [data, loadingModal, errorModal]);

    return (
        <>
            {formatForm(data?.availableLogDates, data?.node, onFormSubmit)}
            <br/>
            {formatLog(log)}
        </>
    )
};

/**
 * If the user has submitted a request for log info then this will make the response displayable.
 *
 * There is a chance that a certain log level is empty for their request and this will inform the user if that occurs
 */
const formatForm = (availableLogDates: AvailableLogDates[] | undefined, node: number | undefined, formSubmit: (event: any) => void): ReactElement | null => {
    if (!availableLogDates || !node) return null;
    else {

        const nodeSpecificAvailableDates = availableLogDates.filter(i => i.node_id === node)[0];

        if (!nodeSpecificAvailableDates) return null;

        const dates = [];

        for (let i = 0; i < nodeSpecificAvailableDates.date.length; i++)
            dates.push(
                <option key={i}>{nodeSpecificAvailableDates.date[i]}</option>
            );

        return (
            <div>
                <hr/>
                <h2>Log Selector</h2>
                <Form onSubmit={formSubmit}>
                    <Form.Group controlId="date">
                        <Form.Label>Select Date</Form.Label>
                        <Form.Control as="select">
                            {dates}
                        </Form.Control>
                    </Form.Group>
                    <Form.Group controlId="log_level">
                        <Form.Label>Select Log Level</Form.Label>
                        <Form.Control as="select">
                            <option>ERROR</option>
                            <option>INFO</option>
                            <option>DEBUG</option>
                            <option>FINEST</option>
                        </Form.Control>
                    </Form.Group>
                    <Button variant="primary" type="submit">
                        Submit
                    </Button>
                </Form>
            </div>
        );
    }

};

/**
 * If the user has submitted a request for log info then this will make the response displayable.
 *
 * There is a chance that a certain log level is empty for their request and this will inform the user if that occurs
 */
const formatLog = (log: Log | null): ReactElement | null => {
    if (!log) return null;
    else if (log?.logs.length === 0) {
        return (
            <div>
                <h2>Log Viewer</h2>
                <p>The log you've selected is empty</p>
            </div>
        );
    } else {
        return (
            <div>
                <h2>Log Viewer</h2>
                <div className={"logViewer"}>
                    <code id={"log"}>{log?.logs.map(i => i + "\n")}</code>
                </div>
            </div>
        );
    }
};