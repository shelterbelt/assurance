import { Row, Col, TabContent, TabPane, TabContainer, Container, Collapse } from 'react-bootstrap';
import { useState } from "react"

import NavBar from '../nav-bar/NavBar';
import ScanPanel from '../scan-panel/ScanPanel';
import HistoryPanel from '../history-panel/HistoryPanel';
import FeedbackPanel from '../feedback-panel/FeedbackPanel';
import ResultsPanel from '../results-panel/ResultsPanel';
import ScanDefinitionPanel from '../scan-definition-panel/ScanDefinitionPanel';

export default function MainWindow() {
  const [results, setResults] = useState([{id: 1, source:{name: "Source Result Item 1"}, target:{name: "Target Result Item 1"}}, {id: 2, source:{name: "Source Result Item 2"}, target:{name: "Target Result Item 2"}}]);
  const [definitionPanelActive, setDefinitionPanelActive] = useState(false);

  function dismissDefinitionPanel(saveDefinition) {
    if (saveDefinition) {
      console.log("Save scan requested.")
    }
    setDefinitionPanelActive(false);
  }
      
  function activateDefinitionPanel() {
    setDefinitionPanelActive(true);
  }

  return (
    <>
      <TabContainer defaultActiveKey="ScanContent" id='navController'>
        <Row>
          <Col>
            <NavBar disabled={definitionPanelActive}></NavBar>
          </Col>
        </Row>
        <TabContent className='tab-area'>
          <TabPane eventKey="ScanContent">
            <Col className='scan-tab-pane'>
              <Collapse in={definitionPanelActive}>
                <Row>
                  <ScanDefinitionPanel scanDefintion={ {} } onOKButtonClicked={() => {dismissDefinitionPanel(true)}} onCancelButtonClicked={() => {dismissDefinitionPanel(false)}}></ScanDefinitionPanel>
                </Row>
              </Collapse>
              <Collapse in={!definitionPanelActive}>
                <Row>
                  <ScanPanel onNewButtonClicked={activateDefinitionPanel}></ScanPanel>
                </Row>
              </Collapse>
            </Col>
          </TabPane>
          <TabPane eventKey="HistoryContent">
            <HistoryPanel></HistoryPanel>
          </TabPane>
        </TabContent>
      </TabContainer>
      <Container className='content-area'>
        <Row>
          <Col><FeedbackPanel></FeedbackPanel></Col>
        </Row>
        <Row className='flex-fill'>
          <Col><ResultsPanel results={results}></ResultsPanel></Col>
        </Row>
      </Container>
    </>
  );
}
