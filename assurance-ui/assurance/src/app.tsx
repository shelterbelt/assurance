import { createRoot } from 'react-dom/client';
import { Row, Col, TabContent, TabPane, TabContainer } from 'react-bootstrap';

import NavBar from './components/nav-bar/NavBar';
import ScanList from './components/scan-list/ScanList';
import ActionsPanel from './components/actions-panel/ActionsPanel';
import HistoryPanel from './components/history-panel/HistoryPanel';
import FeedbackPanel from './components/feedback-panel/FeedbackPanel';
import ResultsPanel from './components/results-panel/ResultsPanel';

function render() {
  const container = document.getElementById("app");
  const root = createRoot(container);
  root.render(
    <TabContainer defaultActiveKey="ScanContent" id='navController'>
      <Row>
        <Col>
          <NavBar></NavBar>
        </Col>
      </Row>
      <TabContent className='content-area'>
        <TabPane eventKey="ScanContent">
          <Row>
            <Col><ScanList></ScanList></Col>
            <Col><ActionsPanel></ActionsPanel></Col>
          </Row>
        </TabPane>
        <TabPane eventKey="HistoryContent">
          <HistoryPanel></HistoryPanel>
        </TabPane>
        <Row>
          <Col><FeedbackPanel></FeedbackPanel></Col>
        </Row>
        <Row className='flex-fill'>
          <Col><ResultsPanel></ResultsPanel></Col>
        </Row>
      </TabContent>
    </TabContainer>
  );
}

render();
