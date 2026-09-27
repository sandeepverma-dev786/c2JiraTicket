import { useEffect, useState } from 'react';
import { ArrowUpRight, Plus, RotateCw, Search } from 'lucide-react';
import { listTickets, userMessageFor } from './api.js';

const STATUSES = ['OPEN', 'IN_PROGRESS', 'RESOLVED', 'CLOSED', 'CANCELLED'];

function formatDate(value) {
  if (!value) return '—';
  const date = new Date(value);
  return Number.isNaN(date.getTime()) ? '—' : new Intl.DateTimeFormat(undefined, {
    month: 'short', day: 'numeric', year: 'numeric',
  }).format(date);
}

export default function TicketListPage({ onCreate }) {
  const [tickets, setTickets] = useState([]);
  const [draftKeyword, setDraftKeyword] = useState('');
  const [draftStatus, setDraftStatus] = useState('');
  const [filters, setFilters] = useState({ keyword: '', status: '' });
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [refreshKey, setRefreshKey] = useState(0);

  useEffect(() => {
    let active = true;
    setLoading(true);
    setError(null);

    listTickets(filters)
      .then((results) => { if (active) setTickets(results); })
      .catch((requestError) => { if (active) setError(requestError); })
      .finally(() => { if (active) setLoading(false); });

    return () => { active = false; };
  }, [filters, refreshKey]);

  function applyFilters(event) {
    event.preventDefault();
    setFilters({ keyword: draftKeyword, status: draftStatus });
  }

  function clearFilters() {
    setDraftKeyword('');
    setDraftStatus('');
    setFilters({ keyword: '', status: '' });
  }

  return (
    <section className="page-section" aria-labelledby="tickets-heading">
      <div className="page-heading-row">
        <div>
          <p className="eyebrow">QUEUE / ALL TICKETS</p>
          <h1 id="tickets-heading">Tickets</h1>
          <p className="page-subtitle">Track requests and move work forward.</p>
        </div>
        <button className="button button-primary" onClick={onCreate}>
          <Plus size={17} aria-hidden="true" /> New ticket
        </button>
      </div>

      <form className="filter-bar" onSubmit={applyFilters}>
        <label className="search-field">
          <Search size={17} aria-hidden="true" />
          <span className="visually-hidden">Search tickets</span>
          <input
            value={draftKeyword}
            onChange={(event) => setDraftKeyword(event.target.value)}
            placeholder="Search title or description"
          />
        </label>
        <label className="filter-status">
          <span className="visually-hidden">Filter by status</span>
          <select value={draftStatus} onChange={(event) => setDraftStatus(event.target.value)}>
            <option value="">All statuses</option>
            {STATUSES.map((status) => <option value={status} key={status}>{status.replace('_', ' ')}</option>)}
          </select>
        </label>
        <button className="button button-dark" type="submit">Apply filters</button>
        {(filters.keyword || filters.status) && (
          <button className="button button-quiet clear-filter" type="button" onClick={clearFilters}>Clear</button>
        )}
        <span className="result-count" aria-live="polite">
          {loading ? 'Updating…' : `${tickets.length} ${tickets.length === 1 ? 'ticket' : 'tickets'}`}
        </span>
      </form>

      {error && (
        <div className="notice notice-error" role="alert">
          <span>{userMessageFor(error, 'Tickets could not be loaded. Please try again.')}</span>
          <button className="button button-small button-quiet" onClick={() => setRefreshKey((key) => key + 1)}>
            <RotateCw size={15} aria-hidden="true" /> Retry
          </button>
        </div>
      )}

      <div className="ticket-table-wrap">
        <div className="ticket-table ticket-table-header" role="row">
          <span>Ticket</span><span>Priority</span><span>Status</span><span>Assignee</span><span>Created</span><span aria-hidden="true" />
        </div>
        {loading ? (
          <div className="table-state" role="status"><span className="loading-dot" /> Loading tickets…</div>
        ) : !error && tickets.length === 0 ? (
          <div className="table-state empty-state">
            <span className="empty-mark"><Search size={20} aria-hidden="true" /></span>
            <strong>No tickets found</strong>
            <span>Try a different search or create a ticket.</span>
            <button className="button button-secondary" onClick={onCreate}><Plus size={16} aria-hidden="true" /> New ticket</button>
          </div>
        ) : (
          tickets.map((ticket) => (
            <a className="ticket-table ticket-row" role="row" href={`#/tickets/${ticket.id}`} key={ticket.id}>
              <span className="ticket-name-cell">
                <strong>{ticket.title}</strong>
                <code>{ticket.id}</code>
              </span>
              <span><span className={`priority-tag priority-${ticket.priority.toLowerCase()}`}>{ticket.priority}</span></span>
              <span><span className={`status-tag status-${ticket.status.toLowerCase()}`}>{ticket.status.replace('_', ' ')}</span></span>
              <span className="assignee-cell">{ticket.assignee || <span className="muted">Unassigned</span>}</span>
              <span className="date-cell">{formatDate(ticket.createdAt)}</span>
              <span className="row-arrow"><ArrowUpRight size={16} aria-hidden="true" /></span>
            </a>
          ))
        )}
      </div>
      <div className="table-footnote"><span className="live-indicator" /> Live from the support queue</div>
    </section>
  );
}
