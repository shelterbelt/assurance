import { render, screen } from '@testing-library/react';

import NavBar from '../NavBar';

test('renders the Scan and History tabs', () => {
  render(<NavBar disabled={false} />);
  expect(screen.getByRole('button', { name: 'Scan' })).toBeInTheDocument();
  expect(screen.getByRole('button', { name: 'History' })).toBeInTheDocument();
});

test('passes the disabled prop through to both tabs', () => {
  render(<NavBar disabled={true} />);
  expect(screen.getByRole('button', { name: 'Scan' })).toHaveAttribute('aria-disabled', 'true');
  expect(screen.getByRole('button', { name: 'History' })).toHaveAttribute('aria-disabled', 'true');
});
