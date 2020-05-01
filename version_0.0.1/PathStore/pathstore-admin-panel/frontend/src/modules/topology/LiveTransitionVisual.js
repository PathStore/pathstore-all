import React, {Component} from "react";
import Modal from "react-modal";
import Tree from "react-tree-graph";
import Button from "react-bootstrap/Button";

/**
 * This class will load a list of buttons based on all applications installed on the system
 *
 * Props:
 * Applications
 * Topology
 * Refresh:
 *
 */
export default class LiveTransitionVisual extends Component {

    /**
     * State:
     * showModal: Display the live transition modal
     * dataModal: which keyspace to display the modal for
     *
     * @param props
     */
    constructor(props) {
        super(props);
        this.state = {
            showModal: false,
            dataModal: null
        }
    }

    /**
     * When the user clicks a button load the data needed into the state
     *
     * @param event
     */
    onButtonClick = (event) => this.setState({showModal: true, dataModal: event.target.value});

    /**
     * Callback for modal to close the modal
     */
    callBack = () => this.setState({showModal: false});

    /**
     * Renders the buttons and optionally the modal if a user has clicked on a button
     *
     * @returns {*}
     */
    render() {

        const buttons = [];

        for (let i = 0; i < this.props.applications.length; i++)
            buttons.push(<Button variant="primary" value={this.props.applications[i].application}
                                 onClick={this.onButtonClick}>{this.props.applications[i].application}</Button>);

        if (buttons.length === 0) buttons.push(<p>No Applications are installed on the system</p>);

        const modal = this.state.showModal ?
            <LiveTransitionVisualModal show={this.state.showModal} topology={this.props.topology}
                                       keyspace={this.state.dataModal}
                                       callback={this.callBack}/> : null;

        return (
            <div>
                {modal}
                {buttons}
            </div>
        );
    }
}

/**
 * This class is used to display the live transition modal based on the button you hit on previous class
 *
 * Props:
 * show: state of the modal (open/closed)
 * topology: topology from api
 * keyspace: keyspace selected
 * callback: to close the modal
 */
class LiveTransitionVisualModal extends Component {

    /**
     * State:
     * waiting list of waiting id's
     * installing: list of installing id's
     * installed: list of installed id's
     * data: tree generated data with proper css classes to represent node state
     *
     * @param props
     */
    constructor(props) {
        super(props);
        this.state = {
            waiting: [],
            installing: [],
            installed: [],
            data: {}
        };
    }


    /**
     * Set up component to be refreshed every 2.5 seconds to update network state
     */
    componentDidMount() {
        this.loadData();

        this.setState({timer: setInterval(this.loadData, 2500)});
    }

    /**
     * Garbage collect timer
     */
    componentWillUnmount() {
        clearInterval(this.state.timer);
    }

    /**
     * Function to load data in from api and parse the nodeids into categories
     */
    loadData = () => {
        fetch('/api/v1/application_management')
            .then(response => response.json())
            .then(response => {
                console.log("Update");
                const keyspace_filtered = response.filter(i => i.keyspace_name === this.props.keyspace);
                this.setState({
                        waiting: keyspace_filtered.filter(i => i.process_status === "WAITING_INSTALL").map(i => parseInt(i.nodeid)),
                        installing: keyspace_filtered.filter(i => i.process_status === "INSTALLING").map(i => parseInt(i.nodeid)),
                        installed: keyspace_filtered.filter(i => i.process_status === "INSTALLED").map(i => parseInt(i.nodeid))
                    }, () => this.setState({data: this.createTree(this.props.topology, -1)})
                );
            });
    };

    /**
     * Checks if value is in array
     *
     * @param array
     * @param value
     * @returns {boolean}
     */
    contains = (array, value) => {
        if (array == null) return false;

        for (let i = 0; i < array.length; i++)
            if (array[i] === value) return true;
        return false;
    };

    /**
     * Creates interpretable json object for the Tree package
     *
     * @param array from parent
     * @param parentId -1 for initial call to look for the root node and work way down
     * @returns {{textProps: {x: number, y: number}, children: [], name: *}|[]}
     */
    createTree = (array, parentId) => {
        let children = [];

        for (let i = 0; i < array.length; i++)
            if (array[i].processStatus === "DEPLOYED") {
                if (parentId === -1) {
                    if (array[i].parentid === parentId) return this.createTreeObject(array[i].id, array);
                } else {
                    if (array[i].parentid === parentId) children.push(this.createTreeObject(array[i].id, array));
                }
            }

        return children;
    };


    /**
     * Name is the node id, textProps is the location of the text associated with the node, children is a list of children
     *
     * @param name current node
     * @param array topology array
     * @returns {{textProps: {x: number, y: number}, children: ({textProps: {x: number, y: number}, children: *[], name: *}|*[]), name: *}}
     */
    createTreeObject = (name, array) => {
        return {
            name: name,
            textProps: {x: -20, y: 25},
            pathProps: {className: 'installation_path'},
            gProps: {className: this.getClassName(parseInt(name))},
            children: this.createTree(array, name)
        }
    };

    /**
     * Based on name and what group it is in we set which css class to give it.
     *
     * @param name nodeid
     * @returns {string} css class to load for that node
     */
    getClassName = (name) => {
        if (this.contains(this.state.installed, name)) return 'installed_node';
        else if (this.contains(this.state.installing, name)) return 'installing_node';
        else if (this.contains(this.state.waiting, name)) return 'waiting_node';
        else return 'not_set_node';
    };

    render() {
        return (
            <Modal isOpen={this.props.show} style={{overlay: {zIndex: 1}}}>
                <div>
                    <p>Live updates for: {this.props.keyspace}</p>
                    <p>Nodes installed are in green</p>
                    <p>Nodes installing are in blue</p>
                    <p>Nodes waiting are in orange</p>
                    <p>Nodes not set are black</p>
                    <Tree
                        data={this.state.data}
                        nodeRadius={15}
                        margins={{top: 20, bottom: 10, left: 20, right: 200}}
                        height={1000}
                        width={1080}
                        gProps={{className: 'node'}}/>
                </div>
                <button onClick={this.props.callback}>close</button>
            </Modal>
        )
    }
}