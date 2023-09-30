import { Button, ListGroup, Container, Row, Col } from "react-bootstrap"
import { useState } from "react"

export default function HistoryPanel() {
    const [scanResults, setScanResults] = useState([{id: 1, name: "Test Results 1"}, {id: 2, name: "Test Results 2"}, {id: 3, name: "Test Results 3"}, {id: 4, name: "Test Results 4"}, {id: 5, name: "Test Results 5"}]);
    const [selectedResult, setSelectedResult] = useState(0);
    
    function handleResultSelected(resultID) {
        setSelectedResult(resultID);
    }
    
    function handleResolveButtonClick() {
        console.log("Resolve clicked");
      }
      
    function handleDeleteButtonClick() {
      console.log("Delete clicked");
    }

    function loadScanResults(scanResults, onResultSelected) {
        const listItems = scanResults.map(result => 
            <ListGroup.Item className="scan-list-item" action key={result.id} onClick={() => {onResultSelected(result.id)}} href={"#" + result.id}>
                {result.name}
            </ListGroup.Item>);
        return (
            <>
                {listItems}
            </>
        );
    }
    
    return (
        <Container className="previous-scans-list-container">
            <Row>
                <Col className="px-0">
                    <ListGroup className="previous-scans-list-group">
                        {loadScanResults(scanResults, handleResultSelected)}
                    </ListGroup>
                </Col>
            </Row>
            <Row className="spacer-row" />
            <Row className="actions-row">
                <Col className="px-0">
                    <Button className="results-manage-button" variant="secondary" disabled={!selectedResult} onClick={handleDeleteButtonClick}>Delete</Button>
                </Col>
                <Col className="spacer-col"/>
                <Col className="px-0">
                    <Button className="results-manage-button" variant="secondary" disabled={!selectedResult} onClick={handleResolveButtonClick}>Resolve</Button>
                </Col>
            </Row>        
        </Container>
    );
}
