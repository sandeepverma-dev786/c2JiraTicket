import { useCallback, useEffect, useState } from 'react';
import { ArrowLeft, Clock3, MessageCircle, Pencil, RotateCw, Send, Workflow } from 'lucide-react';
import { addComment, changeTicketStatus, getTicket, userMessageFor } from './api.js';

const NEXT_STATUSES = {
  OPEN: ['IN_PROGRESS', 'CANCELLED'],
  IN_PROGRESS: ['RESOLVED', 'CANCELLED'],
  RESOLVED: ['CLOSED'],
  CLOSED: [],
  CANCELLED: [],
};

function formatDateTime(value) {
  if (!value) return '—';
  const date = new Date(value);
  return Number.isNaN(date.getTime()) ? '—' : new Intl.DateTimeFormat(undefined, {
    dateStyle: 'medium', timeStyle: 'short',
  }).format(date);
}

export default function TicketDetailPage({ ticketId, onBack, onEdit }) {
  const [ticket, setTicket] = useState(null);
  const [loading, setLoading] = useState(true);
  const [loadError, setLoadError] = useState(null);
  const [actionError, setActionError] = useState('');
  const [transitionBusy, setTransitionBusy] = useState(false);
  const [comment, setComment] = useState({ author: '', content: '' });
  const [commentErrors, setCommentErrors] = useState({});
  const [commentBusy, setCommentBusy] = useState(false);

  const loadTicket = useCallback(async () => {
    setLoading(true);
    setLoadError(null);
    try {
      setTicket(await getTicket(ticketId));
    } catch (error) {
      setLoadError(error);
      setTicket(null);
    } finally {
      setLoading(false);
    }
  }, [ticketId]);

  useEffect(() => { loadTicket(); }, [loadTicket]);

  async function handleTransition(status) {
    setTransitionBusy(true);
    setActionError('');
    try {
      const updated = await changeTicketStatus(ticketId, status);
      setTicket((current) => ({ ...updated, comments: current?.comments || [] }));
    } catch (error) {
      setActionError(userMessageFor(error, 'The status could not be changed.'));
      if (error.code === 'INVALID_STATUS_TRANSITION') await loadTicket();
    } finally {
      setTransitionBusy(false);
    }
  }

  async function handleCommentSubmit(event) {
    event.preventDefault();
    setCommentBusy(true);
    setCommentErrors({});
    setActionError('');
    try {
      await addComment(ticketId, comment);
      setComment({ author: '', content: '' });
      await loadTicket();
    } catch (error) {
      setCommentErrors(error.errors || {});
      setActionError(userMessageFor(error, 'The comment could not be added.'));
      if (error.code === 'NOT_FOUND') await loadTicket();
    } finally {
      setCommentBusy(false);
    }
  }

  if (loading) {
    return <div className="page-loading" role="status"><span className="loading-dot" /> Loading ticket…</div>;
  }

  if (!ticket) {
    return (
      <section className="page-section state-page">
        <button className="button button-quiet" onClick={onBack}><ArrowLeft size={16} /> All tickets</button>
        <div className="state-card">
          <span className="eyebrow">TICKET UNAVAILABLE</span>
          <h1>{loadError?.status === 404 ? 'Ticket not found' : 'Ticket could not be loaded'}</h1>
          <p>{userMessageFor(loadError, 'Try loading the ticket again.')}</p>
          <div className="state-actions">
            <button className="button button-secondary" onClick={loadTicket}><RotateCw size={15} /> Retry</button>
            <button className="button button-quiet" onClick={onBack}>Back to tickets</button>
          </div>
        </div>
      </section>
    );
  }

  const transitions = NEXT_STATUSES[ticket.status] || [];

  return (
    <section className="page-section detail-page" aria-labelledby="detail-title">
      <div className="detail-topline">
        <button className="button button-quiet back-link" onClick={onBack}><ArrowLeft size={16} /> All tickets</button>
        <span className="detail-id">TICKET <code>{ticket.id}</code></span>
      </div>

      {actionError && <div className="notice notice-error" role="alert">{actionError}</div>}

      <header className="detail-heading">
        <div className="detail-heading-copy">
          <div className="detail-tags">
            <span className={`priority-tag priority-${ticket.priority.toLowerCase()}`}>{ticket.priority}</span>
            <span className={`status-tag status-${ticket.status.toLowerCase()}`}>{ticket.status.replace('_', ' ')}</span>
          </div>
          <h1 id="detail-title">{ticket.title}</h1>
        </div>
        <button className="button button-secondary" onClick={() => onEdit(ticket)}>
          <Pencil size={16} aria-hidden="true" /> Edit ticket
        </button>
      </header>

      <div className="detail-grid">
        <article className="detail-main">
          <section className="content-section">
            <h2>Description</h2>
            <p className="description-text">{ticket.description}</p>
          </section>

          <section className="content-section comments-section">
            <div className="section-heading-row">
              <div>
                <p className="eyebrow">CONVERSATION</p>
                <h2><MessageCircle size={18} aria-hidden="true" /> Comments <span className="section-count">{ticket.comments?.length || 0}</span></h2>
              </div>
            </div>

            <div className="comment-list">
              {ticket.comments?.length ? ticket.comments.map((item) => (
                <article className="comment-item" key={item.id}>
                  <div className="comment-marker" aria-hidden="true">{item.author.trim().charAt(0).toUpperCase() || '?'}</div>
                  <div className="comment-body">
                    <div className="comment-meta"><strong>{item.author}</strong><time dateTime={item.createdAt}>{formatDateTime(item.createdAt)}</time></div>
                    <p>{item.content}</p>
                  </div>
                </article>
              )) : (
                <div className="comments-empty">No comments yet. Add the first update below.</div>
              )}
            </div>

            <form className="comment-form" onSubmit={handleCommentSubmit}>
              <label className="field">
                <span>Author <span className="required-mark">Required</span></span>
                <input
                  value={comment.author}
                  onChange={(event) => setComment((current) => ({ ...current, author: event.target.value }))}
                  required
                  aria-invalid={Boolean(commentErrors.author)}
                />
                {commentErrors.author && <span className="field-error">{commentErrors.author}</span>}
              </label>
              <label className="field">
                <span>Comment <span className="required-mark">Required</span></span>
                <textarea
                  value={comment.content}
                  onChange={(event) => setComment((current) => ({ ...current, content: event.target.value }))}
                  maxLength={1000}
                  rows={3}
                  required
                  aria-invalid={Boolean(commentErrors.content)}
                />
                {commentErrors.content && <span className="field-error">{commentErrors.content}</span>}
                <span className="field-hint">{comment.content.length}/1000</span>
              </label>
              <div className="form-actions form-actions-end">
                <button className="button button-primary" type="submit" disabled={commentBusy}>
                  <Send size={15} aria-hidden="true" /> {commentBusy ? 'Sending…' : 'Add comment'}
                </button>
              </div>
            </form>
          </section>
        </article>

        <aside className="detail-aside">
          <section className="aside-section">
            <h2>Ticket information</h2>
            <dl className="ticket-facts">
              <div><dt>Assignee</dt><dd>{ticket.assignee || 'Unassigned'}</dd></div>
              <div><dt>Created</dt><dd><Clock3 size={14} /> {formatDateTime(ticket.createdAt)}</dd></div>
              <div><dt>Updated</dt><dd><Clock3 size={14} /> {formatDateTime(ticket.updatedAt)}</dd></div>
            </dl>
          </section>

          <section className="aside-section transition-section">
            <div className="aside-heading-icon"><Workflow size={17} aria-hidden="true" /><h2>Change status</h2></div>
            {transitions.length ? (
              <div className="transition-actions">
                {transitions.map((status) => (
                  <button key={status} className="transition-button" disabled={transitionBusy} onClick={() => handleTransition(status)}>
                    {transitionBusy ? 'Updating…' : status.replace('_', ' ')}
                  </button>
                ))}
              </div>
            ) : (
              <p className="terminal-note">This ticket is in a terminal status.</p>
            )}
            <p className="service-note">Status changes are checked by the support service.</p>
          </section>
        </aside>
      </div>
    </section>
  );
}
