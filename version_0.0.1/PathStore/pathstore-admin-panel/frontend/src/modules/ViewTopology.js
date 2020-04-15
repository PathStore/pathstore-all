import React, {Component} from "react";
import Tree from "react-tree-graph";
import Info from "./Info";

export default class ViewTopology extends Component {
    constructor(props) {
        super(props);
        this.state = {
            topology: this.createTree(this.props.topology, -1),
            info: null,
            refreshInfo: false
        };
    }

    componentWillReceiveProps(props, nextContext) {
        if (this.props.refresh !== props.refresh) {
            this.setState({topology: this.createTree(props.topology, -1)})
        }
    }

    createTree = (array, parentId) => {
        let children = [];

        for (let i = 0; i < array.length; i++)
            if (parentId === -1) {
                if (array[i].parentid === parentId) {
                    return this.createTreeObject(array[i].id, array);
                }
            } else {
                if (array[i].parentid === parentId)
                    children.push(this.createTreeObject(array[i].id, array));
            }


        return children;
    };

    createTreeObject = (name, array) => {
        return {
            name: name,
            textProps: {x: -20, y: 25},
            children: this.createTree(array, name)
        }
    };

    handleClick = (event, node) => {
        this.setState({
            info: <Info node={node} refresh={!this.state.refreshInfo}/>,
            refreshInfo: !this.state.refreshInfo
        })
    };

    render() {
        return (
            <div>
                <p>Click on a node to view its current applications</p>
                <Tree data={this.state.topology}
                      nodeRadius={15}
                      margins={{top: 20, bottom: 10, left: 20, right: 200}}
                      height={1000}
                      width={1080}
                      gProps={{
                          className: 'node',
                          onClick: this.handleClick
                      }}
                />
                {this.state.info}
            </div>
        );
    }
}