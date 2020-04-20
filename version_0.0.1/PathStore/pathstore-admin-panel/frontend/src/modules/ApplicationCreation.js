import React, {Component} from "react";
import DisplayAvailableApplication from "./DisplayAvailableApplication";
import ApplicationLoader from "./ApplicationLoader";
import LoadingModal from "./LoadingModal";

/**
 * This model encapsulates multiple submodels and is used to allow the user to create applications
 * and install applications onto the pathstore network
 *
 * @see ApplicationLoader
 * @see DisplayAvailableApplication
 */
export default class ApplicationCreation extends Component {

    /**
     * State:
     *
     * applications: applications from backend
     * refresh: whether to refresh children components or node
     * loadingModal: whether to display the loading model or not
     *
     * @param props
     */
    constructor(props) {
        super(props);

        this.state = {
            applications: [],
            refresh: false,
            loadingModal: false
        };
    }

    /**
     * Update applications and force update the children components
     *
     * @param props
     * @param nextContext
     */
    componentWillReceiveProps(props, nextContext) {
        if (this.props.refresh !== props.refresh)
            this.setState({applications: props.applications, refresh: !this.state.refresh})
    }


    /**
     * This is a callback function for application loader to refresh all the application dependent models
     * if a user loads a new application in
     *
     * Refreshes parent components through forceRefresh and then updates state to refresh children components
     */
    refreshComponents = () => {
        this.props.forceRefresh();
        this.setState({refresh: !this.state.refresh, loadingModel: false});
    };

    /**
     * This is a callback function for application loader to spawn the loading modal if the user has made a request
     * to install an application
     */
    spawnLoadingModel = () => {
        this.setState({loadingModel: true});
    };

    /**
     * Display header, sub models and loading modal if required
     * @returns {*}
     */
    render() {
        return (
            <div>
                <h2>Application Creation</h2>
                <LoadingModal show={this.state.loadingModel}/>
                <DisplayAvailableApplication applications={this.state.applications} refresh={this.state.refresh}/>
                <ApplicationLoader applications={this.state.applications} refresh={this.state.refresh}
                                   forceRefresh={this.refreshComponents} spawnLoadingModel={this.spawnLoadingModel}/>
            </div>
        )
    }
}