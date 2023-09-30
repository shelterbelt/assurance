import { Row, Col, Container, Collapse } from 'react-bootstrap';
import { useState } from "react"

import ScanList from '../scan-list/ScanList';
import ActionsPanel from '../actions-panel/ActionsPanel';

export default function ScanPanel({onNewButtonClicked}) {
    const [scans, setScans] = useState([{id: 1, name: "Test Scan 1"}, {id: 2, name: "Test Scan 2"}, {id: 3, name: "Test Scan 3"}, {id: 4, name: "Test Scan 4"}, {id: 5, name: "Test Scan 5"}]);
    const [selectedScan, setSelectedScan] = useState(0);

    function handleScanSelected(scanID) {
      setSelectedScan(scanID);
    }
    
    function handleDeleteButtonClick() {
      console.log("Delete clicked");
    }
    
    return (
      <Container className="scan-operations-container">
        <Row>
          <Col className="ps-0"><ScanList scans={scans} selectedScan={selectedScan} onScanSelected={handleScanSelected} onDeleteButtonClick={handleDeleteButtonClick} onNewButtonClick={onNewButtonClicked}></ScanList></Col>
          <Col className="pe-0"><ActionsPanel selectedScan={selectedScan}></ActionsPanel></Col>
        </Row>
      </Container>
    );
}
