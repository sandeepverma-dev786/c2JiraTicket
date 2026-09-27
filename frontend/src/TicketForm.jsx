import { useState } from 'react';
import { ArrowLeft, Save } from 'lucide-react';

const PRIORITIES = ['LOW', 'MEDIUM', 'HIGH', 'CRITICAL'];

const EMPTY_TICKET = {
  title: '',
  description: '',
  priority: '',
  assignee: '',
};

export default function TicketForm({
  mode,
  initialValue = EMPTY_TICKET,
  busy = false,
  errorMessage = '',
  fieldErrors = {},
  onSubmit,
  onCancel,
}) {
  const isEditing = mode === 'edit';
  const [values, setValues] = useState(() => ({ ...EMPTY_TICKET, ...initialValue }));

  function changeField(event) {
    const { name, value } = event.target;
    setValues((current) => ({ ...current, [name]: value }));
  }

  function handleSubmit(event) {
    event.preventDefault();
    onSubmit({ ...values, assignee: values.assignee ?? '' });
  }

  return (
    <form className="ticket-form" onSubmit={handleSubmit}>
      {errorMessage && (
        <div className="notice notice-error" role="alert">
          {errorMessage}
        </div>
      )}

      <div className="form-grid">
        <label className="field field-wide">
          <span>Title <span className="required-mark">Required</span></span>
          <input
            name="title"
            value={values.title}
            onChange={changeField}
            maxLength={200}
            required
            autoFocus
          />
          {fieldErrors.title && <span className="field-error">{fieldErrors.title}</span>}
        </label>

        <label className="field field-wide">
          <span>Description <span className="required-mark">Required</span></span>
          <textarea
            name="description"
            value={values.description}
            onChange={changeField}
            maxLength={2000}
            rows={5}
            required
          />
          {fieldErrors.description && <span className="field-error">{fieldErrors.description}</span>}
          <span className="field-hint">{values.description.length}/2000</span>
        </label>

        <label className="field">
          <span>Priority <span className="required-mark">Required</span></span>
          <select name="priority" value={values.priority} onChange={changeField} required>
            <option value="" disabled>Select priority</option>
            {PRIORITIES.map((priority) => <option key={priority} value={priority}>{priority}</option>)}
          </select>
          {fieldErrors.priority && <span className="field-error">{fieldErrors.priority}</span>}
        </label>

        <label className="field">
          <span>Assignee <span className="optional-mark">Optional</span></span>
          <input
            name="assignee"
            value={values.assignee ?? ''}
            onChange={changeField}
            placeholder="Team or person"
          />
          {fieldErrors.assignee && <span className="field-error">{fieldErrors.assignee}</span>}
        </label>
      </div>

      <div className="form-actions">
        <button className="button button-quiet" type="button" onClick={onCancel} disabled={busy}>
          <ArrowLeft size={16} aria-hidden="true" />
          Cancel
        </button>
        <button className="button button-primary" type="submit" disabled={busy}>
          <Save size={16} aria-hidden="true" />
          {busy ? 'Saving…' : isEditing ? 'Save changes' : 'Create ticket'}
        </button>
      </div>
    </form>
  );
}
