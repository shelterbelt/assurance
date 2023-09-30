import { createRoot } from 'react-dom/client';

import MainWindow from './components/main-window/MainWindow';

function render() {
  const container = document.getElementById("app");
  const root = createRoot(container);
  root.render(<MainWindow></MainWindow>);
}

render();
