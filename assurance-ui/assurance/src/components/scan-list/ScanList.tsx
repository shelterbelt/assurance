import { Button, ListGroup, Container, Row, Col } from "react-bootstrap"

export default function ScanList({scans, selectedScan, onScanSelected, onNewButtonClick, onDeleteButtonClick}) {

    function loadScanDefinitions(scans, onScanSelected) {
        const listItems = scans.map(scan => 
            <ListGroup.Item className="scan-list-item" action key={scan.id} onClick={() => {onScanSelected(scan.id)}} href={"#" + scan.id}>
                {scan.name}
            </ListGroup.Item>);
        return (<>{listItems}</>);
    }
    
    return (
        <Container className="scan-list-container">
            <Row>
                <Col className="px-0">
                    <ListGroup className="scan-list-group">
                        {loadScanDefinitions(scans, onScanSelected)}
                    </ListGroup>
                </Col>
            </Row>
            <Row className="spacer-row" />
            <Row className="actions-row">
                <Col className="px-0">
                    <Button className="scan-manage-button" variant="secondary" onClick={onNewButtonClick}>New</Button>
                </Col>
                <Col className="spacer-col"/>
                <Col className="px-0">
                    <Button className="scan-manage-button" variant="secondary" disabled={!selectedScan} onClick={onDeleteButtonClick}>Delete</Button>
                </Col>
            </Row>        
        </Container>
    );
}
