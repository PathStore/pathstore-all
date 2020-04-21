import React, {Component} from "react";
import Modal from "react-modal";
import Tree from "react-tree-graph";
import Button from "react-bootstrap/Button";

export default class LiveTransitionVisual extends Component {

    constructor(props) {
        super(props);
        this.state = {
            showModal: false,
            dataModal: null
        }
    }

    onButtonClick = (event) => this.setState({showModal: true, dataModal: event.target.value});


    callBack = () => this.setState({showModal: false});

    render() {

        const buttons = [];

        for (let i = 0; i < this.props.applications.length; i++)
            buttons.push(<Button variant="primary" value={this.props.applications[i].application}
                                 onClick={this.onButtonClick}>{this.props.applications[i].application}</Button>);

        const modal = this.state.showModal ?
            <LiveTransitionVisualModal show={this.state.showModal} topology={this.props.topology}
                                       keyspace={this.state.dataModal}
                                       callback={this.callBack}/> : null;

        return (
            <div>
                {modal}
                {buttons}
            </div>)
    }
}

class LiveTransitionVisualModal extends Component {

    constructor(props) {
        super(props);
        this.state = {
            waiting: [],
            installing: [],
            installed: [],
            data: {},
            time: Date.now(),
            timer: null
        };
    }


    componentDidMount() {
        fetch('/api/v1/application_management')
            .then(response => response.json())
            .then(response => {
                const keyspace_filtered = response.filter(i => i.keyspace_name === this.props.keyspace);
                this.setState({
                    waiting: keyspace_filtered.filter(i => i.process_status === "WAITING_INSTALL").map(i => parseInt(i.nodeid)),
                    installing: keyspace_filtered.filter(i => i.process_status === "INSTALLING").map(i => parseInt(i.nodeid)),
                    installed: keyspace_filtered.filter(i => i.process_status === "INSTALLED").map(i => parseInt(i.nodeid))
                }, () =>
                    this.setState({
                        data: this.createTree(this.props.topology, -1),
                        timer: setInterval(() => this.setState({time: Date.now()}), 1000)
                    }));
            });
    }

    componentWillUnmount() {
        clearInterval(this.state.timer);
    }

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

    createTree = (array, parentId) => {
        let children = [];

        for (let i = 0; i < array.length; i++)
            if (parentId === -1) {
                if (array[i].parentid === parentId) return this.createTreeObject(array[i].id, array);
            } else {
                if (array[i].parentid === parentId) children.push(this.createTreeObject(array[i].id, array));
            }


        return children;
    };


    createTreeObject = (name, array) => {
        return {
            name: name,
            textProps: {x: -20, y: 25},
            pathProps: {className: 'installation_path'},
            gProps: {className: this.getClassName(parseInt(name))},
            children: this.createTree(array, name)
        }
    };

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