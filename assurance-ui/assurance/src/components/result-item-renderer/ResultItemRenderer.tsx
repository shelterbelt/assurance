import { ListGroup, Col, Row, Button, Collapse } from "react-bootstrap"
import { useState } from "react"

export default function ResultItemRenderer({result, onResultItemSelected, onMergeButtonClicked}) {
    
    const [expanded, setExpanded] = useState(false);

    function handleExpandButtonClicked() {
        setExpanded(!expanded);
    }

    return (
        <ListGroup.Item className="scan-result-item" onClick={() => {onResultItemSelected(result.id)}}>
            <Row>
                <Col className="source-col">
                    <ResultItemDetails item={result.source} expanded={expanded} additionalClasses={["source-result-item"]}></ResultItemDetails>
                </Col>
                <Col className="merge-actions-col">
                    <div className="merge-actions-container">
                        <Button className="result-merge-button merge-source" variant="secondary" onClick={() => {onMergeButtonClicked("source")}}></Button>
                        <Button className="result-merge-button merge-target" variant="secondary" onClick={() => {onMergeButtonClicked("target")}}></Button>
                    </div>
                </Col>
                <Col className="target-col">
                    <ResultItemDetails item={result.target} expanded={expanded} additionalClasses={["target-result-item"]}></ResultItemDetails>
                </Col>
            </Row>
            <Row className="expand-button-row" onClick={handleExpandButtonClicked}>
                <span className="row-expand-button-indicator">...</span>
            </Row>
        </ListGroup.Item>
    );
}

function ResultItemDetails({item, expanded, additionalClasses}) {
    if (expanded) {
        additionalClasses.push("expanded");
    }
    additionalClasses.push("result-item-details");
    
    return (
        <div className={additionalClasses.join(" ")}>
            <p className="file-name-field">{item.name}</p>
            <p className="file-attribute-field">Creation Time: {item.created}</p>
            <p className="file-attribute-field">Last Modified Time: {item.modified}</p>
            <Collapse in={expanded}>
                <div className="additional-file-attributes">
                    <p className="file-attribute-field">Directory: {item.isDirectory}</p>
                    <p className="file-attribute-field">Other: {item.isOther}</p>
                    <p className="file-attribute-field">Regular File: {item.isFile}</p>
                    <p className="file-attribute-field">Symbolic Link: {item.isSymLink}</p>
                    <p className="file-attribute-field">Last Access Time: {item.accessed}</p>
                    <p className="file-attribute-field">File Size: {item.size}</p>
                    <p className="file-attribute-field">Archive: {item.isArchive}</p>
                    <p className="file-attribute-field">Hidden: {item.isHidden}</p>
                    <p className="file-attribute-field">Read Only: {item.isReadOnly}</p>
                    <p className="file-attribute-field">System File: {item.isSystemFile}</p>
                    <p className="file-attribute-field">Group Name: {item.group}</p>
                    <p className="file-attribute-field">Owner: {item.owner}</p>
                    <p className="file-attribute-field">Permissions: {item.permissions}</p>
                    <p className="file-attribute-field">File Owner: {item.fileOwner}</p>
                    <p className="file-attribute-field">ACLs: {item.acl}</p>
                    <p className="file-attribute-field">User-defined Attributes Hash: {item.userHash}</p>
                </div>
            </Collapse>
        </div>
    );
}
