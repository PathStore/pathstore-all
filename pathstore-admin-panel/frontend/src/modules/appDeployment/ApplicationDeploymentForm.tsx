import React, {FunctionComponent, ReactElement, useCallback, useContext, useEffect, useState} from "react";
import {ApplicationDeploymentModalDataContext} from "../../contexts/ApplicationDeploymentModalContext";
import {Button, Form} from "react-bootstrap";
import {
    Application,
    APPLICATION_STATE,
    ApplicationStatus,
    ApplicationUpdate,
    Deployment
} from "../../utilities/ApiDeclarations";
import {createMap, identity} from "../../utilities/Utils";
import {SubmissionErrorModalContext} from "../../contexts/SubmissionErrorModalContext";

/**
 * This component is used to hypothetically deploy an application on a set of nodes.
 *
 * @see ApplicationDeploymentModal for how these updates are sent to the api
 * @constructor
 */
export const ApplicationDeploymentForm: FunctionComponent = () => {

    // load data from context
    const {application, reducedDeployment, reducedApplicationStatus, additions, updateAdditions, deletions} = useContext(ApplicationDeploymentModalDataContext);

    // submission error modal context
    const submissionErrorModal = useContext(SubmissionErrorModalContext);

    // options for the form
    const [options, updateOptions] = useState<ReactElement[]>([]);

    // update the options of the form whenever the reduced Deployment list changes
    useEffect(() => {
        let tempOptions = [];
        if (reducedDeployment)
            for (let [index, item] of reducedDeployment.entries())
                tempOptions.push(
                    <option key={index}>{item.new_node_id}</option>
                );
        updateOptions(tempOptions);
    }, [reducedDeployment, updateOptions]);

    // update the addition entries on submission of the form
    const onFormSubmit = useCallback((event: any): void => {
        event.preventDefault();

        if (additions && updateAdditions && deletions && reducedApplicationStatus && reducedDeployment && application && submissionErrorModal.show) {

            const nodeId: number = parseInt(event.target.elements.nodeId.value);

            try {
                const update = deployApplication(additions, deletions, reducedApplicationStatus, reducedDeployment, application, nodeId);
                updateAdditions(update.additions);
            } catch (e) {
                submissionErrorModal.show(e.message);
            }
        }
    }, [additions, updateAdditions, deletions, reducedApplicationStatus, reducedDeployment, application, submissionErrorModal]);

    // render form
    return (
        <>
            <h2>Application Deployment</h2>
            <p>Application deployment form</p>
            <Form onSubmit={onFormSubmit}>
                <Form.Group controlId="nodeId">
                    <Form.Control as="select">
                        {options}
                    </Form.Control>
                </Form.Group>
                <Button variant="primary" type="submit">
                    Submit
                </Button>
            </Form>
        </>
    );
};

/**
 * This function will return a new list of updated addition entries based on what node id was submitted from the form.
 * There is a chance that the additions array returned is the same as before as the use as submitted a redundant request.
 *
 * @param additions list of additions
 * @param deletions list of deletions
 * @param applicationStatus reduced application status list. Filtered by the current keyspace
 * @param deployment reduced deployment list. Filtered by status of deployed
 * @param application application that is currently selected
 * @param node submitted by the form
 */
const deployApplication = (
    additions: ApplicationUpdate[],
    deletions: ApplicationUpdate[],
    applicationStatus: ApplicationStatus[],
    deployment: Deployment[],
    application: Application,
    node: number)
    : { additions: ApplicationUpdate[] } => {
    const deploymentMap: Map<number, Deployment> = createMap<number, Deployment>(v => v.new_node_id, identity, deployment);
    const statusMap: Map<number, ApplicationStatus> = createMap<number, ApplicationStatus>(v => v.node_id, identity, applicationStatus);
    const additionsMap: Map<number, ApplicationUpdate> = createMap<number, ApplicationUpdate>(v => v.nodeId, identity, additions);
    const deletionsMap: Map<number, ApplicationUpdate> = createMap<number, ApplicationUpdate>(v => v.nodeId, identity, deletions);

    deployApplicationHelper(deploymentMap, statusMap, additionsMap, deletionsMap, application, node);

    return (
        {
            additions: Array.from(additionsMap.values())
        }
    );
};

/**
 * This function will recursively update the additions map from in front to back order (root node down).
 *
 * Records are added iff the parent node is a valid node, if the status map doesn't already have a record for that node,
 * and if the additions map doesn't already contain this record.
 *
 * If the deletions map contains a given node an error is thrown as you cannot deploy an application if a parent node
 * is not available to do so (is scheduled for deletion)
 *
 * @param deploymentMap reduced deployment map
 * @param statusMap reduced application status map
 * @param additionsMap additions map
 * @param deletionsMap deletions map
 * @param application application selected
 * @param node node submitted from form or a parent of this node
 */
const deployApplicationHelper = (
    deploymentMap: Map<number, Deployment>,
    statusMap: Map<number, ApplicationStatus>,
    additionsMap: Map<number, ApplicationUpdate>,
    deletionsMap: Map<number, ApplicationUpdate>,
    application: Application,
    node: number): void => {

    if (node === -1) return;
    else {
        const parent = deploymentMap.get(node)?.parent_node_id;

        if (deletionsMap.has(node))
            throw new Error("You cannot perform an application addition on a node where a parent node is queued for deletion");

        if (parent && !statusMap.has(node) && !additionsMap.has(node)) {
            deployApplicationHelper(deploymentMap, statusMap, additionsMap, deletionsMap, application, parent);
            additionsMap.set(node, applicationUpdateFromInfo(application, node, [parent]));
        } else {
            switch (statusMap.get(node)?.process_status) {
                case APPLICATION_STATE[APPLICATION_STATE.INSTALLED]:
                case APPLICATION_STATE[APPLICATION_STATE.PROCESSING_INSTALLING]:
                case APPLICATION_STATE[APPLICATION_STATE.INSTALLING]:
                case APPLICATION_STATE[APPLICATION_STATE.WAITING_INSTALL]:
                case undefined: // case where it was already apart of the additions map
                    break;
                default:
                    throw new Error("You cannot perform an application addition on a node where a parent node in the path is removing");
            }
        }
    }
};

/**
 * Generate an application update record given the current application and a node id
 *
 * @param application current application
 * @param node node id
 * @param waitFor list to wait for
 */
export const applicationUpdateFromInfo = (application: Application, node: number, waitFor: number[]): ApplicationUpdate => {
    return (
        {
            nodeId: node,
            keyspaceName: application.keyspace_name,
            waitFor: waitFor
        }
    );
};