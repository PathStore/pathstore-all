import React, {Component} from "react";
import {AvailableLogDates, Log} from "../utilities/ApiDeclarations";
import {Button, Form} from "react-bootstrap";
import {webHandler} from "../utilities/Utils";
import {LoadingModal} from "./LoadingModal";

/**
 * Properties definition for {@link LogViewer}
 */
interface LogViewerProps {
    /**
     * Node id of node to show info of
     */
    readonly node: number

    /**
     * List of available dates for each log
     */
    readonly availableLogDates?: AvailableLogDates[]
}

/**
 * State definition for {@link LogViewer}
 */
interface LogViewerState {
    /**
     * Set of logs queried from the user on form submit
     */
    readonly log: Log | null

    /**
     * Denotes whether or not to show the loading modal
     */
    readonly showLoading: boolean
}

/**
 * This component is used to select a specific log based on date and log level. It will then submit the request and display the response
 * iff the response is populated with log records
 *
 * @param props
 * @constructor
 */
export default class LogViewer extends Component<LogViewerProps, LogViewerState> {

    /**
     * Initialize props and state
     *
     * @param props {@link LogViewerProps}
     */
    constructor(props: LogViewerProps) {
        super(props);

        this.state = {
            log: null,
            showLoading: false
        };
    }


    /**
     * Takes the values in the form and requests the api for the data based on form input.
     *
     * Note: No need to error check form input as all data is guaranteed to be valid as it is derived from api data
     *
     * @param event form submission event
     */
    onFormSubmit = (event: any): void => {

        event.preventDefault();

        const date = event.target.elements.date.value.trim();

        const log_level = event.target.elements.log_level.value.trim();

        const url = '/api/v1/logs?node_id=' + this.props.node + "&date=" + date + "&log_level=" + log_level;

        this.setState({showLoading: true}, () => {
            fetch(url, {
                method: 'GET',
                headers: {
                    'Accept': 'application/json',
                    'Content-Type': 'application/json'
                }
            })
                .then(webHandler)
                .then((response: Log) => {
                    this.setState({log: response});
                })
                .catch(response => {
                    alert("ERROR: " + JSON.stringify(response));
                })
                .finally(() => {
                    this.setState({showLoading: false});
                });
        })
    };

    /**
     * If there is available log dates for this specific node then it will render a form to allow the user
     * to select what day and what log level they want to view
     *
     */
    formatForm = (): any => {
        if (this.props.availableLogDates === undefined) return null;
        else {

            const nodeSpecificAvailableDates = this.props.availableLogDates.filter(i => i.node_id === this.props.node)[0];

            if (nodeSpecificAvailableDates === undefined) return null;

            const dates = [];

            for (let i = 0; i < nodeSpecificAvailableDates.date.length; i++)
                dates.push(
                    <option key={i}>{nodeSpecificAvailableDates.date[i]}</option>
                );

            return (
                <div>
                    <h2>Log Selector</h2>
                    <Form onSubmit={this.onFormSubmit}>
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
    formatLog = (): any => {
        if (this.state.log === null) return null;
        else if (this.state.log?.logs.length === 0) {
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
                        <code id={"log"}>{this.state.log?.logs.map(i => i + "\n")}</code>
                    </div>
                </div>
            );
        }
    };

    /**
     * Loading modal
     *
     * Form
     *
     * Log
     */
    render = () => {

        const loadingModal = this.state.showLoading ?
            <LoadingModal show={this.state.showLoading}/>
            : null;

        return (
            <div>
                {loadingModal}
                {this.formatForm()}
                <br/>
                {this.formatLog()}
            </div>
        );
    };
};