import {Component, RefObject} from "react";
import {ApplicationStatus, Deployment} from "../../utilities/ApiDeclarations";
import {PathStoreTopology} from "../PathStoreTopology";
import React from "react";
import {Button} from "react-bootstrap";
import Modal from "react-bootstrap/Modal";
import {AlignedDivs, Left, Right} from "../../utilities/AlignedDivs";

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
    readonly waiting: Set<number>

    /**
     * List of node id's who are installing
     */
    readonly installing: Set<number>

    /**
     * List of node id's who are installed
     */
    readonly installed: Set<number>
}

/**
 * This component is used to display buttons for each keyspace that you can watch transition live and visually
 */
export default class LiveTransitionVisualModal extends Component<LiveTransitionVisualModalProperties, LiveTransitionVisualModalState> {

    /**
     * Reference to the right div to determine the height and width
     */
    private rightRef: RefObject<HTMLDivElement> = React.createRef<HTMLDivElement>();

    /**
     * Initialize props and state
     *
     * @param props
     */
    constructor(props: LiveTransitionVisualModalProperties) {
        super(props);
        this.state = {
            waiting: new Set<number>(),
            installing: new Set<number>(),
            installed: new Set<number>()
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
                new Set<number>(
                    nextProps.applicationStatus
                        .filter(i => i.keyspace_name === nextProps.application)
                        .filter(i => i.process_status === "WAITING_INSTALL")
                        .map(i => i.node_id)
                ),
            installing:
                new Set<number>(
                    nextProps.applicationStatus
                        .filter(i => i.keyspace_name === nextProps.application)
                        .filter(i => i.process_status === "INSTALLING" || i.process_status === "PROCESSING_INSTALLING")
                        .map(i => i.node_id)
                ),
            installed:
                new Set<number>(
                    nextProps.applicationStatus
                        .filter(i => i.keyspace_name === nextProps.application)
                        .filter(i => i.process_status === "INSTALLED")
                        .map(i => i.node_id)
                )
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

        if (this.state.installed.has(name)) return 'installed_node';
        else if (this.state.installing.has(name)) return 'installing_node';
        else if (this.state.waiting.has(name)) return 'waiting_node';
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

        let width = this.rightRef.current?.clientWidth;

        //let height = this.rightRef.current?.clientHeight;

        if (width === undefined) width = 500;

        let height = 500;

        return (
            <Modal show={this.props.show}
                   size={"xl"}
                   centered
            >
                <Modal.Header>
                    <Modal.Title>Live updates for {this.props.application}</Modal.Title>
                </Modal.Header>
                <Modal.Body>
                    <AlignedDivs>
                        <Left width='35%'>
                            <h2>Topology Legend</h2>
                            <p>Nodes installed are in <span className={'d_green'}>green</span></p>
                            <p>Nodes installing are in <span className={'d_cyan'}>cyan</span></p>
                            <p>Nodes waiting are in <span className={'d_yellow'}>yellow</span></p>
                            <p>Nodes not set are <span className={'d_currentLine'}>dark grey</span></p>
                        </Left>
                        <Right divRef={this.rightRef}>
                            <h2>Topology</h2>
                            <PathStoreTopology width={width}
                                               height={height}
                                               deployment={this.props.deployment.filter(i => i.process_status === "DEPLOYED")}
                                               get_colour={this.getClassName}/>
                        </Right>
                    </AlignedDivs>
                </Modal.Body>
                <Modal.Footer>
                    <Button onClick={this.props.callback}>close</Button>
                </Modal.Footer>
            </Modal>
        )
    }
}