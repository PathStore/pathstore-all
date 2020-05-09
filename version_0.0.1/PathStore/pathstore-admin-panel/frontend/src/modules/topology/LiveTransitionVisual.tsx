import React, {Component} from "react";
import {Application, ApplicationStatus, Deployment} from "../../utilities/ApiDeclarations";
import {Button} from "react-bootstrap";
import LiveTransitionVisualModal from "./LiveTransitionVisualModal";

/**
 * Properties definition for {@link LiveTransitionVisual}
 */
interface LiveTransitionVisualProperties {
    /**
     * List of application objects from api
     */
    readonly applications: Application[]

    /**
     * List of node application status from api
     */
    readonly applicationStatus: ApplicationStatus[]

    /**
     * List of deployment objects from api
     */
    readonly deployment: Deployment[]
}

/**
 * State definition for {@link LiveTransitionVisual}
 */
interface LiveTransitionVisualState {
    /**
     * Whether to show the transition modal or not
     */
    readonly showModal: boolean

    /**
     * What application did the user request
     */
    readonly dataModal: string | null
}

/**
 * This component is used to display buttons for each keyspace that you can watch transition live and visually
 */
export default class LiveTransitionVisual extends Component<LiveTransitionVisualProperties, LiveTransitionVisualState> {

    /**
     * Initializes props and state
     *
     * @param props
     */
    constructor(props: LiveTransitionVisualProperties) {
        super(props);

        this.state = {
            showModal: false,
            dataModal: null
        }
    }

    /**
     * On button click display the modal and give the modal which keyspace to display
     *
     * @param event
     */
    onButtonClick = (event: any): void => this.setState({showModal: true, dataModal: event.target.value});

    /**
     * Callback for modal to close itself
     */
    callBack = (): void => this.setState({showModal: false});

    /**
     * First gather all buttons based on the number of applications installed on the network
     *
     * If there are no applications inform the user of this
     *
     * Secondly figure out if you need to load a modal
     *
     * Finally load the potential modal and all buttons or information message to user
     *
     * @returns {*}
     */
    render() {

        const buttons = [];

        for (let i = 0; i < this.props.applications.length; i++)
            buttons.push(
                <Button key={i}
                        variant="primary"
                        value={this.props.applications[i].keyspace_name}
                        onClick={this.onButtonClick}>{this.props.applications[i].keyspace_name}
                </Button>
            );

        if (buttons.length === 0) buttons.push(<p key={0}>No Applications are installed on the system</p>);

        const modal =
            this.state.showModal ?
                <LiveTransitionVisualModal show={this.state.showModal}
                                           application={this.state.dataModal}
                                           deployment={this.props.deployment}
                                           applicationStatus={this.props.applicationStatus}
                                           callback={this.callBack}/>
                : null;

        return (
            <div>
                {modal}
                {buttons}
            </div>
        );
    }
};