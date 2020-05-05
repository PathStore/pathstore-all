import React, {Component} from "react";
import Tree from "react-tree-graph";
import NodeInfoModal from "./NodeInfoModal";

/**
 * This class is used to display a visual of the network topology of the network using the Tree package.
 */
export default class ViewTopology extends Component {

    /**
     * @param props sends refresh from parent module, and topology queried from parent module
     *
     * State:
     *  - topology: from props
     *  - info: module to load if the user clicks on a node
     *  - refreshInfo: to allow for creation of multiple info modals without refreshing viewtopology
     */
    constructor(props) {
        super(props);
        this.state = {
            topology: this.createTree(this.props.topology, -1),
            info: null,
            refreshInfo: false
        };
    }

    /**
     * Called when parent props change. If refresh has changed from the local copy then re-create the tree
     */
    componentWillReceiveProps(props, nextContext) {
        if (this.props.refresh !== props.refresh)
            this.setState({topology: this.createTree(props.topology, -1)});
    }

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
            if (parentId === -1) {
                if (array[i].parentid === parentId) return this.createTreeObject(array[i], array);
            } else {
                if (array[i].parentid === parentId) children.push(this.createTreeObject(array[i], array));
            }


        return children;
    };

    /**
     * Name is the node id, textProps is the location of the text associated with the node, children is a list of children
     *
     * @param object
     * @param array
     * @returns {{textProps: {x: number, y: number}, children: ({textProps: {x: number, y: number}, children: *[], name: *}|*[]), name: *}}
     */
    createTreeObject = (object, array) => {
        return {
            name: object.id,
            textProps: {x: -20, y: 25},
            gProps: {
                className: this.getClassName(object.processStatus),
                onClick: this.handleClick
            },
            children: this.createTree(array, object.id)
        }
    };

    getClassName = (status) => {
        switch (status) {
            case "WAITING_DEPLOYMENT":
                return 'waiting_node';
            case "DEPLOYING":
                return 'installing_node';
            default:
                return 'installed_node';
        }
    };

    /**
     * Function that is called when a node is clicked.
     *
     * @param event not used.
     * @param node node id
     */
    handleClick = (event, node) => {
        this.setState({
            info: <NodeInfoModal node={node} refresh={!this.state.refreshInfo} topology={this.props.topology}
                                 servers={this.props.servers}/>,
            refreshInfo: !this.state.refreshInfo
        })
    };

    /**
     * Renders a header and the tree. Also optional an info modal
     *
     * @returns {*}
     */
    render() {
        return (
            <div>
                <p>Click on a node to view its current applications</p>
                <Tree data={this.state.topology}
                      nodeRadius={15}
                      margins={{top: 20, bottom: 10, left: 20, right: 200}}
                      height={1000}
                      width={1080}
                />
                {this.state.info}
            </div>
        );
    }
}