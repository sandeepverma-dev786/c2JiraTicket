const configuredBaseUrl = import.meta.env.VITE_API_BASE_URL || '/api';
const API_BASE_URL = configuredBaseUrl.replace(/\/$/, '');

export class ApiError extends Error {
  constructor({ status, code, errors }) {
    super(code || 'API_ERROR');
    this.name = 'ApiError';
    this.status = status;
    this.code = code;
    this.errors = errors || {};
  }
}

async function request(path, options = {}) {
  let response;

  try {
    response = await fetch(`${API_BASE_URL}${path}`, {
      ...options,
      headers: {
        ...(options.body ? { 'Content-Type': 'application/json' } : {}),
        ...options.headers,
      },
    });
  } catch {
    throw new ApiError({ status: 0, code: 'NETWORK_ERROR' });
  }

  const responseText = await response.text();
  let payload = null;
  if (responseText) {
    try {
      payload = JSON.parse(responseText);
    } catch {
      payload = null;
    }
  }

  if (!response.ok) {
    throw new ApiError({
      status: response.status,
      code: payload?.code || 'API_ERROR',
      errors: payload?.errors,
    });
  }

  return payload;
}

export function listTickets({ keyword = '', status = '' } = {}) {
  const params = new URLSearchParams();
  if (keyword.trim()) params.set('keyword', keyword.trim());
  if (status) params.set('status', status);
  const query = params.toString();
  return request(`/tickets${query ? `?${query}` : ''}`);
}

export function getTicket(ticketId) {
  return request(`/tickets/${encodeURIComponent(ticketId)}`);
}

export function createTicket(fields) {
  return request('/tickets', { method: 'POST', body: JSON.stringify(fields) });
}

export function updateTicket(ticketId, fields) {
  return request(`/tickets/${encodeURIComponent(ticketId)}`, {
    method: 'PUT',
    body: JSON.stringify(fields),
  });
}

export function changeTicketStatus(ticketId, status) {
  return request(`/tickets/${encodeURIComponent(ticketId)}/status`, {
    method: 'PATCH',
    body: JSON.stringify({ status }),
  });
}

export function addComment(ticketId, fields) {
  return request(`/tickets/${encodeURIComponent(ticketId)}/comments`, {
    method: 'POST',
    body: JSON.stringify(fields),
  });
}

export function userMessageFor(error, fallback = 'The request could not be completed. Please try again.') {
  switch (error?.code) {
    case 'VALIDATION_ERROR':
      return 'Please review the highlighted fields and try again.';
    case 'NOT_FOUND':
      return 'This ticket is no longer available.';
    case 'INVALID_STATUS_TRANSITION':
      return 'That status change is no longer available. The ticket has been refreshed.';
    case 'NETWORK_ERROR':
      return 'Cannot reach the support service. Check the connection and try again.';
    default:
      return fallback;
  }
}
