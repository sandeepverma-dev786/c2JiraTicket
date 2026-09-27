import { useEffect, useState } from 'react';
import { CircleHelp, LifeBuoy, Plus, TicketCheck } from 'lucide-react';
import { createTicket, getTicket, updateTicket, userMessageFor } from './api.js';
import TicketDetailPage from './TicketDetailPage.jsx';
import TicketForm from './TicketForm.jsx';
import TicketListPage from './TicketListPage.jsx';

function readRoute() {
  const hash = window.location.hash.replace(/^#\/?/, '');
  const parts = hash.split('/').filter(Boolean);
  if (parts[0] !== 'tickets' || parts.length === 1) return { page: 'list' };
  if (parts[1] === 'new') return { page: 'create' };
  if (parts[2] === 'edit') return { page: 'edit', ticketId: decodeURIComponent(parts[1]) };
  return { page: 'detail', ticketId: decodeURIComponent(parts[1]) };
}

function navigate(path) {
  window.location.hash = path;
}

function TicketEditorPage({ mode, ticketId, onCancel, onSaved }) {
  const [ticket, setTicket] = useState(null);
  const [loading, setLoading] = useState(mode === 'edit');
  const [loadError, setLoadError] = useState(null);
  const [saving, setSaving] = useState(false);
  const [errorMessage, setErrorMessage] = useState('');
  const [fieldErrors, setFieldErrors] = useState({});
  const [retryKey, setRetryKey] = useState(0);

  useEffect(() => {
    if (mode !== 'edit') return undefined;
    let active = true;
    setLoading(true);
    setLoadError(null);
    getTicket(ticketId)
      .then((result) => { if (active) setTicket(result); })
      .catch((error) => { if (active) setLoadError(error); })
      .finally(() => { if (active) setLoading(false); });
    return () => { active = false; };
  }, [mode, ticketId, retryKey]);

  async function handleSubmit(fields) {
    setSaving(true);
    setErrorMessage('');
    setFieldErrors({});
    try {
      const requestFields = mode === 'edit' && fields.assignee === ''
        ? { ...fields, assignee: null }
        : fields;
      const saved = mode === 'edit'
        ? await updateTicket(ticketId, requestFields)
        : await createTicket(requestFields);
      onSaved(saved.id);
    } catch (error) {
      setFieldErrors(error.errors || {});
      setErrorMessage(userMessageFor(error, mode === 'edit'
        ? 'Ticket changes could not be saved.'
        : 'Ticket could not be created.'));
    } finally {
      setSaving(false);
    }
  }

  if (loading) return <div className="page-loading" role="status">Loading ticket details…</div>;
  if (loadError) {
    return (
      <section className="page-section state-page">
        <div className="state-card">
          <span className="eyebrow">{loadError.status === 404 ? 'TICKET UNAVAILABLE' : 'LOAD ERROR'}</span>
          <h1>{loadError.status === 404 ? 'Ticket not found' : 'Ticket could not be loaded'}</h1>
          <p>{userMessageFor(loadError, 'Try loading the ticket again.')}</p>
          <div className="state-actions">
            <button className="button button-secondary" onClick={() => setRetryKey((key) => key + 1)}>Retry</button>
            <button className="button button-quiet" onClick={onCancel}>Back</button>
          </div>
        </div>
      </section>
    );
  }

  return (
    <section className="page-section editor-page">
      <div className="page-heading-row">
        <div>
          <p className="eyebrow">{mode === 'edit' ? 'TICKET / EDIT' : 'TICKETS / NEW'}</p>
          <h1>{mode === 'edit' ? 'Edit ticket' : 'Create ticket'}</h1>
          <p className="page-subtitle">{mode === 'edit' ? 'Update the request details.' : 'Capture the issue and route it to the right team.'}</p>
        </div>
      </div>
      <div className="form-sheet">
        <TicketForm
          mode={mode}
          initialValue={ticket || undefined}
          busy={saving}
          errorMessage={errorMessage}
          fieldErrors={fieldErrors}
          onSubmit={handleSubmit}
          onCancel={onCancel}
        />
      </div>
    </section>
  );
}

export default function App() {
  const [route, setRoute] = useState(readRoute);

  useEffect(() => {
    const updateRoute = () => setRoute(readRoute());
    window.addEventListener('hashchange', updateRoute);
    return () => window.removeEventListener('hashchange', updateRoute);
  }, []);

  function openTickets() { navigate('/tickets'); }

  return (
    <div className="app-shell">
      <header className="topbar">
        <a className="brand" href="#/tickets" aria-label="Support Desk tickets">
          <span className="brand-mark"><LifeBuoy size={21} aria-hidden="true" /></span>
          <span className="brand-name">Support<span>Desk</span></span>
        </a>
        <nav className="top-nav" aria-label="Main navigation">
          <a className={route.page === 'list' ? 'nav-link active' : 'nav-link'} href="#/tickets"><TicketCheck size={16} /> Tickets</a>
        </nav>
        <div className="topbar-right">
          <span className="workspace-label"><span className="workspace-dot" /> Support workspace</span>
          <button className="icon-button" aria-label="Help" title="Help"><CircleHelp size={18} /></button>
        </div>
      </header>

      <main className="main-shell">
        {route.page === 'list' && <TicketListPage onCreate={() => navigate('/tickets/new')} />}
        {route.page === 'create' && (
          <TicketEditorPage mode="create" onCancel={openTickets} onSaved={(id) => navigate(`/tickets/${id}`)} />
        )}
        {route.page === 'edit' && (
          <TicketEditorPage mode="edit" ticketId={route.ticketId} onCancel={() => navigate(`/tickets/${route.ticketId}`)} onSaved={(id) => navigate(`/tickets/${id}`)} />
        )}
        {route.page === 'detail' && (
          <TicketDetailPage ticketId={route.ticketId} onBack={openTickets} onEdit={() => navigate(`/tickets/${route.ticketId}/edit`)} />
        )}
      </main>

      <footer className="app-footer">
        <span>Support operations</span>
        <span className="footer-separator" />
        <span>Ticket management</span>
        {route.page === 'list' && <button onClick={() => navigate('/tickets/new')}><Plus size={14} /> New ticket</button>}
      </footer>
    </div>
  );
}
