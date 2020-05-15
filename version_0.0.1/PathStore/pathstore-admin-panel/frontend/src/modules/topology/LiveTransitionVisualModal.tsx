import {Component} from "react";
import {ApplicationStatus, Deployment} from "../../utilities/ApiDeclarations";
import {contains} from "../../utilities/Utils";
import Modal from "react-modal";
import {PathStoreTopology} from "../PathStoreTopology";
import React from "react";
import {Button} from "react-bootstrap";

/**
 * Properties definition for {@link LiveTransitionVisualModal}
 */
interface LiveTransitionVisualModalProperties {
    /**
     * Whether to show the modal or not
     */
    readonly show: boolean

    /**
     * What application to filter by
     */
    readonly application: string | null

    /**
     * List of deployment objects from api
     */
    readonly deployment: Deployment[]

    /**
     * List of node application status from api
     */
    readonly applicationStatus: ApplicationStatus[]

    /**
     * callback to close modal
     */
    readonly callback: () => void
}

/**
 * State definition for {@link LiveTransitionVisualModal}
 */
interface LiveTransitionVisualModalState {
    /**
     * List of node id's who are waiting
     */
    readonly waiting: number[]

    /**
     * List of node id's who are installing
     */
    readonly installing: number[]

    /**
     * List of node id's who are installed
     */
    readonly installed: number[]
}

/**
 * This component is used to display buttons for each keyspace that you can watch transition live and visually
 */
export default class LiveTransitionVisualModal extends Component<LiveTransitionVisualModalProperties, LiveTransitionVisualModalState> {

    /**
     * Initalize props and state
     *
     * @param props
     */
    constructor(props: LiveTransitionVisualModalProperties) {
        super(props);
        this.state = {
            waiting: [],
            installing: [],
            installed: []
        };
    }

    /**
     * Since we have internal state data based on a prop we need to derive new states everytime the applicationStatus
     * prop updates
     *
     * This function filters all applicationStatus data in 3 different ways. This is needed to colour the nodes based
     * on their current process status
     *
     * @param nextProps
     * @param prevState
     * @returns {{installed: number[], waiting: number[], installing: number[]}}
     */
    static getDerivedStateFromProps(nextProps: LiveTransitionVisualModalProperties, prevState: LiveTransitionVisualModalState): LiveTransitionVisualModalState {
        return {
            waiting:
                nextProps.applicationStatus
                    .filter(i => i.keyspace_name === nextProps.application)
                    .filter(i => i.process_status === "WAITING_INSTALL")
                    .map(i => i.nodeid),
            installing:
                nextProps.applicationStatus
                    .filter(i => i.keyspace_name === nextProps.application)
                    .filter(i => i.process_status === "INSTALLING")
                    .map(i => i.nodeid),
            installed:
                nextProps.applicationStatus
                    .filter(i => i.keyspace_name === nextProps.application)
                    .filter(i => i.process_status === "INSTALLED")
                    .map(i => i.nodeid)
        }
    }

    /**
     * Function for pathstore topology to colour nodes based on their process status
     *
     * Installed nodes are green (installed_node)
     * Installing nodes are blue (installing_node)
     * Waiting nodes are orange (waiting_node)
     *
     * @param object
     * @returns {string}
     */
    getClassName = (object: Deployment): string => {
        const name = object.new_node_id;

        if (contains<number>(this.state.installed, name)) return 'installed_node';
        else if (contains<number>(this.state.installing, name)) return 'installing_node';
        else if (contains<number>(this.state.waiting, name)) return 'waiting_node';
        else return 'not_set_node';
    };

    /**
     * Load colour description to user
     *
     * Load topology filtered that each node is deployed
     *
     * @returns {*}
     */
    render() {
        return (
            <Modal isOpen={this.props.show} style={{overlay: {zIndex: 1}}} ariaHideApp={false}>
                <div>
                    <p>Live updates for: {this.props.application}</p>
                    <p>Nodes installed are in green</p>
                    <p>Nodes installing are in blue</p>
                    <p>Nodes waiting are in orange</p>
                    <p>Nodes not set are black</p>
                    <PathStoreTopology deployment={this.props.deployment.filter(i => i.process_status === "DEPLOYED")}
                                       get_colour={this.getClassName}/>
                </div>
                <Button onClick={this.props.callback}>close</Button>
            </Modal>
        )
    }
}