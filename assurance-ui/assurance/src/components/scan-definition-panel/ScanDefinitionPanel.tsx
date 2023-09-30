import { Col, Row, Container, Form, Button, FloatingLabel } from "react-bootstrap";

import PathSelector from '../path-selector/PathSelector';

export default function ScanDefinitionPanel({scanDefintion, onOKButtonClicked, onCancelButtonClicked}) {
    const titleAction = (scanDefintion.id) ? "Edit" : "Add New";

    return (
        <Container className="scan-definition-panel">
            <Row>
                <span className="scan-definition-panel-title">{titleAction} Scan Defition</span>
            </Row>
            <Row>
                <Form>
                    <Form.Group controlId="scanDefNameField">
                        <FloatingLabel label="Scan Name">
                            <Form.Control className="scan-name-field" placeholder="Scan Name" />
                        </FloatingLabel>
                    </Form.Group>
                    <Container className="scan-locations-group">
                        <Form.Group controlId="scanDefSourceLocationField">
                            <PathSelector label="Source Location" additionalClasses={["source-location"]} dialogOptions={{title: "Select Source"}} />
                        </Form.Group>
                        <Form.Group controlId="scanDefTargetLocationField">
                            {/* <FloatingLabel label="Target Location">
                                <Form.Control type="file" className="scan-location-field target-location" placeholder="Target Location" directory="true" webkitdirectory="true"/>
                            </FloatingLabel> */}
                            <PathSelector label="Target Location" additionalClasses={["target-location"]} dialogOptions={{title: "Select Target"}} />
                        </Form.Group>
                    </Container>
                    <Container className="scan-merge-options-group">
                        <Form.Group controlId="scanDefMergeStrategyField">
                            <FloatingLabel label="Merge Strategy">
                                <Form.Select className="scan-merge-strategy-field" size="sm">
                                    <option>Source</option>
                                    <option>Target</option>
                                    <option>Both</option>
                                </Form.Select>
                            </FloatingLabel>
                        </Form.Group> 
                        <Form.Group controlId="scanDefAutoMergeField">
                            <Form.Check type="checkbox" className="scan-auto-merge-field" label="Automatically Merge" />
                        </Form.Group> 
                        <Form.Group controlId="scanDefIncludeTimestampsField">
                            <Form.Check type="checkbox" className="scan-include-timestamps-field" label="Include Timestamps Other Than Create Date" />
                        </Form.Group> 
                        <Form.Group controlId="scanDefIncludeAdvancedAttributesField">
                            <Form.Check type="checkbox" className="scan-include-advanced-attributes-field" label="Include Advanced Attributes" />
                        </Form.Group> 
                    </Container>
                </Form>
            </Row>
            <Row className="spacer-row" />
            <Row className="actions-row">
                <Col className="px-0">
                    <Button className="scan-definition-manage-button" variant="secondary" onClick={onOKButtonClicked}>OK</Button>
                </Col>
                <Col className="spacer-col"/>
                <Col className="px-0">
                    <Button className="scan-definition-manage-button" variant="secondary" onClick={onCancelButtonClicked}>Cancel</Button>
                </Col>
            </Row>        
        </Container>
    );
}
