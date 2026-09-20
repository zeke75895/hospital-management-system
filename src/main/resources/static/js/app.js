'use strict';

/* ==========================================================================
   Ocean Bay Clinic - vanilla JS front end for the Hospital Management API.
   No build step, no framework: everything below is plain DOM + fetch().
   ========================================================================== */

const TOKEN_KEY = 'hms_token';

const state = {
  token: null,
  user: null, // { username, role, patientId, doctorId }
  doctors: [],
  patients: [],
  appointments: [],
};

/* ---- small helpers ------------------------------------------------------ */

function escapeHtml(value) {
  if (value === null || value === undefined) return '';
  return String(value)
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
    .replace(/"/g, '&quot;')
    .replace(/'/g, '&#39;');
}

function formatDateTime(iso) {
  if (!iso) return '';
  const d = new Date(iso);
  if (isNaN(d.getTime())) return iso;
  return d.toLocaleString(undefined, { dateStyle: 'medium', timeStyle: 'short' });
}

function decodeJwtPayload(token) {
  try {
    const base64Url = token.split('.')[1];
    const base64 = base64Url.replace(/-/g, '+').replace(/_/g, '/');
    const padded = base64 + '='.repeat((4 - (base64.length % 4)) % 4);
    const json = decodeURIComponent(
      atob(padded)
        .split('')
        .map((c) => '%' + c.charCodeAt(0).toString(16).padStart(2, '0'))
        .join('')
    );
    return JSON.parse(json);
  } catch (e) {
    return null;
  }
}

/* ---- session -------------------------------------------------------------*/

function saveSession(token) {
  localStorage.setItem(TOKEN_KEY, token);
  return applySession(token);
}

function applySession(token) {
  const payload = decodeJwtPayload(token);
  if (!payload || (payload.exp && payload.exp * 1000 < Date.now())) {
    clearSession();
    return false;
  }
  state.token = token;
  state.user = {
    username: payload.sub,
    role: payload.role,
    patientId: payload.patientId ?? null,
    doctorId: payload.doctorId ?? null,
  };
  return true;
}

function clearSession() {
  localStorage.removeItem(TOKEN_KEY);
  state.token = null;
  state.user = null;
}

/* ---- API client ------------------------------------------------------------*/

class ApiError extends Error {
  constructor(message, status, body) {
    super(message);
    this.status = status;
    this.body = body;
  }
}

function extractErrorMessage(body) {
  if (!body) return null;
  if (typeof body === 'string') return body;
  if (Array.isArray(body.errors) && body.errors.length) {
    return body.errors.map((e) => `${e.field}: ${e.message}`).join('; ');
  }
  if (body.message) return body.message;
  return null;
}

async function apiFetch(path, options = {}) {
  const headers = Object.assign({ 'Content-Type': 'application/json' }, options.headers || {});
  if (state.token) {
    headers.Authorization = 'Bearer ' + state.token;
  }

  const res = await fetch(path, Object.assign({}, options, { headers }));

  if (res.status === 401 && state.token) {
    clearSession();
    showAuthScreen();
    showToast('Your session expired - please log in again.', 'error');
    throw new ApiError('Session expired', 401, null);
  }

  const text = await res.text();
  let body = null;
  if (text) {
    try {
      body = JSON.parse(text);
    } catch (e) {
      body = text;
    }
  }

  if (!res.ok) {
    throw new ApiError(extractErrorMessage(body) || `Request failed (${res.status})`, res.status, body);
  }

  return body;
}

/* ---- toast -------------------------------------------------------------- */

let toastTimer = null;
function showToast(message, type = 'success') {
  const toast = document.getElementById('toast');
  toast.textContent = message;
  toast.className = 'toast show ' + type;
  clearTimeout(toastTimer);
  toastTimer = setTimeout(() => toast.classList.remove('show'), 4200);
}

/* ---- modal ---------------------------------------------------------------*/

let lastFocusedElement = null;

function openModal(html) {
  lastFocusedElement = document.activeElement;
  document.getElementById('modalBody').innerHTML = html;
  document.getElementById('modalBackdrop').classList.remove('hidden');
  document.getElementById('closeModalBtn').focus();
}

function closeModal() {
  document.getElementById('modalBackdrop').classList.add('hidden');
  document.getElementById('modalBody').innerHTML = '';
  if (lastFocusedElement) lastFocusedElement.focus();
}

document.getElementById('closeModalBtn').addEventListener('click', closeModal);
document.getElementById('modalBackdrop').addEventListener('click', (e) => {
  if (e.target.id === 'modalBackdrop') closeModal();
});
document.addEventListener('keydown', (e) => {
  if (e.key === 'Escape' && !document.getElementById('modalBackdrop').classList.contains('hidden')) {
    closeModal();
  }
});

/* ---- screen / tab switching ----------------------------------------------*/

function showAuthScreen() {
  document.getElementById('appScreen').classList.add('hidden');
  document.getElementById('userBadge').classList.add('hidden');
  document.getElementById('authScreen').classList.remove('hidden');
}

function switchAuthTab(tab) {
  const isLogin = tab === 'login';
  document.getElementById('loginTabBtn').classList.toggle('active', isLogin);
  document.getElementById('loginTabBtn').setAttribute('aria-selected', String(isLogin));
  document.getElementById('registerTabBtn').classList.toggle('active', !isLogin);
  document.getElementById('registerTabBtn').setAttribute('aria-selected', String(!isLogin));
  document.getElementById('loginForm').classList.toggle('hidden', !isLogin);
  document.getElementById('registerForm').classList.toggle('hidden', isLogin);
}

document.getElementById('loginTabBtn').addEventListener('click', () => switchAuthTab('login'));
document.getElementById('registerTabBtn').addEventListener('click', () => switchAuthTab('register'));

document.getElementById('registerRole').addEventListener('change', (e) => {
  const role = e.target.value;
  document.getElementById('patientIdField').classList.toggle('hidden', role !== 'PATIENT');
  document.getElementById('doctorIdField').classList.toggle('hidden', role !== 'DOCTOR');
});

document.getElementById('dashboardTabs').addEventListener('click', (e) => {
  const btn = e.target.closest('.tab-btn');
  if (!btn) return;
  const panel = btn.dataset.panel;
  document.querySelectorAll('#dashboardTabs .tab-btn').forEach((b) => {
    b.classList.toggle('active', b === btn);
    b.setAttribute('aria-selected', b === btn ? 'true' : 'false');
  });
  document.querySelectorAll('#appScreen .panel').forEach((p) => {
    p.classList.toggle('hidden', p.id !== `panel-${panel}`);
  });
  if (panel === 'appointments') loadAppointments(currentFilters());
});

document.querySelectorAll('[data-close]').forEach((btn) => {
  btn.addEventListener('click', () => document.getElementById(btn.dataset.close).classList.add('hidden'));
});

document.getElementById('showAddDoctorBtn').addEventListener('click', () => {
  document.getElementById('addDoctorForm').classList.toggle('hidden');
});
document.getElementById('showAddPatientBtn').addEventListener('click', () => {
  document.getElementById('addPatientForm').classList.toggle('hidden');
});

/* ---- login / register / logout --------------------------------------------*/

document.getElementById('loginForm').addEventListener('submit', async (e) => {
  e.preventDefault();
  const username = document.getElementById('loginUsername').value;
  const password = document.getElementById('loginPassword').value;
  try {
    const res = await apiFetch('/api/auth/login', {
      method: 'POST',
      body: JSON.stringify({ username, password }),
    });
    saveSession(res.token);
    onLoginSuccess();
  } catch (err) {
    showToast(err.message, 'error');
  }
});

document.getElementById('registerForm').addEventListener('submit', async (e) => {
  e.preventDefault();
  const role = document.getElementById('registerRole').value;
  const payload = {
    username: document.getElementById('registerUsername').value,
    password: document.getElementById('registerPassword').value,
    role,
  };
  if (role === 'PATIENT') payload.patientId = Number(document.getElementById('registerPatientId').value);
  if (role === 'DOCTOR') payload.doctorId = Number(document.getElementById('registerDoctorId').value);

  try {
    await apiFetch('/api/auth/register', { method: 'POST', body: JSON.stringify(payload) });
    showToast('Account created - log in below.', 'success');
    document.getElementById('loginUsername').value = payload.username;
    e.target.reset();
    switchAuthTab('login');
  } catch (err) {
    showToast(err.message, 'error');
  }
});

document.getElementById('logoutBtn').addEventListener('click', () => {
  clearSession();
  showAuthScreen();
  showToast('Logged out.', 'success');
});

function onLoginSuccess() {
  document.getElementById('authScreen').classList.add('hidden');
  document.getElementById('appScreen').classList.remove('hidden');
  document.getElementById('userBadge').classList.remove('hidden');
  document.getElementById('userName').textContent = state.user.username;

  const pill = document.getElementById('userRolePill');
  pill.textContent = state.user.role;
  pill.className = 'role-pill role-' + state.user.role;

  document.body.classList.remove('role-patient', 'role-doctor', 'role-admin');
  document.body.classList.add('role-' + state.user.role.toLowerCase());

  setMinBookingDateTime();
  loadDoctors();
  if (state.user.role !== 'PATIENT') loadPatients();
}

function setMinBookingDateTime() {
  const input = document.getElementById('bookScheduledAt');
  const soon = new Date(Date.now() + 5 * 60000);
  input.min = soon.toISOString().slice(0, 16);
}

/* ---- doctors ---------------------------------------------------------------*/

async function loadDoctors() {
  try {
    const page = await apiFetch('/api/doctors?size=50');
    state.doctors = page.content || [];
    renderDoctorsList();
    populateDoctorSelect();
  } catch (e) {
    showToast(e.message, 'error');
  }
}

function renderDoctorsList() {
  const container = document.getElementById('doctorsList');
  if (!state.doctors.length) {
    container.innerHTML = '<p class="empty-state">No doctors yet - staff can add one above.</p>';
    return;
  }
  container.innerHTML = state.doctors.map(renderDoctorCard).join('');
}

function renderDoctorCard(doc) {
  const altShape = doc.id % 2 === 0 ? ' sketch-alt' : '';
  const name = `Dr. ${escapeHtml(doc.firstName)} ${escapeHtml(doc.lastName)}`;
  return `
    <article class="card sketch${altShape}" data-doctor-id="${doc.id}">
      <h3>${name}</h3>
      <p>${escapeHtml(doc.specialty)}</p>
      <p class="meta">${escapeHtml(doc.email)}</p>
      <p class="meta">License ${escapeHtml(doc.licenseNumber)} &middot; ID ${doc.id}</p>
      <div class="card-actions staff-only">
        <button class="btn btn-small" data-action="add-schedule" data-doctor-id="${doc.id}" data-doctor-name="${escapeHtml(name)}">+ Schedule</button>
      </div>
    </article>`;
}

function populateDoctorSelect() {
  const select = document.getElementById('bookDoctorSelect');
  select.innerHTML = state.doctors
    .map((d) => `<option value="${d.id}">Dr. ${escapeHtml(d.firstName)} ${escapeHtml(d.lastName)} (${escapeHtml(d.specialty)})</option>`)
    .join('');
}

document.getElementById('doctorsList').addEventListener('click', (e) => {
  const scheduleBtn = e.target.closest('[data-action="add-schedule"]');
  if (scheduleBtn) {
    document.getElementById('scheduleDoctorId').value = scheduleBtn.dataset.doctorId;
    document.getElementById('scheduleDoctorLabel').textContent = 'For ' + scheduleBtn.dataset.doctorName;
    const form = document.getElementById('addScheduleForm');
    form.classList.remove('hidden');
    form.scrollIntoView({ behavior: 'smooth', block: 'center' });
    return;
  }
  const card = e.target.closest('.card');
  if (card) {
    const doc = state.doctors.find((d) => d.id === Number(card.dataset.doctorId));
    if (doc) showDoctorModal(doc);
  }
});

document.getElementById('addDoctorForm').addEventListener('submit', async (e) => {
  e.preventDefault();
  const payload = {
    firstName: document.getElementById('docFirstName').value,
    lastName: document.getElementById('docLastName').value,
    specialty: document.getElementById('docSpecialty').value,
    email: document.getElementById('docEmail').value,
    licenseNumber: document.getElementById('docLicense').value,
  };
  try {
    const doc = await apiFetch('/api/doctors', { method: 'POST', body: JSON.stringify(payload) });
    showToast(`Doctor created - id ${doc.id}. You'll need this id to register their login.`, 'success');
    e.target.reset();
    e.target.classList.add('hidden');
    loadDoctors();
  } catch (err) {
    showToast(err.message, 'error');
  }
});

document.getElementById('addScheduleForm').addEventListener('submit', async (e) => {
  e.preventDefault();
  const doctorId = document.getElementById('scheduleDoctorId').value;
  const payload = {
    dayOfWeek: document.getElementById('scheduleDayOfWeek').value,
    startTime: document.getElementById('scheduleStart').value,
    endTime: document.getElementById('scheduleEnd').value,
  };
  try {
    await apiFetch(`/api/doctors/${doctorId}/schedules`, { method: 'POST', body: JSON.stringify(payload) });
    showToast('Schedule added.', 'success');
    e.target.reset();
    e.target.classList.add('hidden');
  } catch (err) {
    showToast(err.message, 'error');
  }
});

function showDoctorModal(doc) {
  openModal(`
    <h2 id="modalTitle">Dr. ${escapeHtml(doc.firstName)} ${escapeHtml(doc.lastName)}</h2>
    <dl>
      <dt>Specialty</dt><dd>${escapeHtml(doc.specialty)}</dd>
      <dt>Email</dt><dd>${escapeHtml(doc.email)}</dd>
      <dt>License number</dt><dd>${escapeHtml(doc.licenseNumber)}</dd>
      <dt>Doctor ID</dt><dd>${doc.id}</dd>
    </dl>
  `);
}

/* ---- patients (staff only) -----------------------------------------------*/

async function loadPatients() {
  try {
    const page = await apiFetch('/api/patients?size=50');
    state.patients = page.content || [];
    renderPatientsList();
    populatePatientSelect();
  } catch (e) {
    showToast(e.message, 'error');
  }
}

function renderPatientsList() {
  const container = document.getElementById('patientsList');
  if (!state.patients.length) {
    container.innerHTML = '<p class="empty-state">No patients yet - add one above.</p>';
    return;
  }
  container.innerHTML = state.patients.map(renderPatientCard).join('');
}

function renderPatientCard(p) {
  const altShape = p.id % 2 === 0 ? ' sketch-alt' : '';
  return `
    <article class="card sketch${altShape}" data-patient-id="${p.id}">
      <h3>${escapeHtml(p.firstName)} ${escapeHtml(p.lastName)}</h3>
      <p class="meta">${escapeHtml(p.email)}</p>
      <p class="meta">MRN ${escapeHtml(p.medicalRecordNumber)} &middot; ID ${p.id}</p>
    </article>`;
}

function populatePatientSelect() {
  const select = document.getElementById('bookPatientSelect');
  select.innerHTML = state.patients
    .map((p) => `<option value="${p.id}">${escapeHtml(p.firstName)} ${escapeHtml(p.lastName)} (#${p.id})</option>`)
    .join('');
}

document.getElementById('patientsList').addEventListener('click', (e) => {
  const card = e.target.closest('.card');
  if (card) {
    const p = state.patients.find((x) => x.id === Number(card.dataset.patientId));
    if (p) showPatientModal(p);
  }
});

document.getElementById('addPatientForm').addEventListener('submit', async (e) => {
  e.preventDefault();
  const payload = {
    firstName: document.getElementById('patFirstName').value,
    lastName: document.getElementById('patLastName').value,
    dateOfBirth: document.getElementById('patDob').value,
    phone: document.getElementById('patPhone').value,
    email: document.getElementById('patEmail').value,
    medicalRecordNumber: document.getElementById('patMrn').value,
  };
  try {
    const p = await apiFetch('/api/patients', { method: 'POST', body: JSON.stringify(payload) });
    showToast(`Patient created - id ${p.id}. You'll need this id to register their login.`, 'success');
    e.target.reset();
    e.target.classList.add('hidden');
    loadPatients();
  } catch (err) {
    showToast(err.message, 'error');
  }
});

function showPatientModal(p) {
  openModal(`
    <h2 id="modalTitle">${escapeHtml(p.firstName)} ${escapeHtml(p.lastName)}</h2>
    <dl>
      <dt>Date of birth</dt><dd>${escapeHtml(p.dateOfBirth)}</dd>
      <dt>Email</dt><dd>${escapeHtml(p.email)}</dd>
      <dt>Phone</dt><dd>${escapeHtml(p.phone)}</dd>
      <dt>Medical record #</dt><dd>${escapeHtml(p.medicalRecordNumber)}</dd>
      <dt>Patient ID</dt><dd>${p.id}</dd>
    </dl>
  `);
}

/* ---- booking ---------------------------------------------------------------*/

document.getElementById('bookForm').addEventListener('submit', async (e) => {
  e.preventDefault();
  const appt = await bookOnce().catch((err) => {
    showToast(err.message, 'error');
    return null;
  });
  if (appt) {
    showToast(`Booked! Appointment #${appt.id} at ${formatDateTime(appt.scheduledAt)}`, 'success');
    document.getElementById('bookReason').value = '';
    loadAppointments(currentFilters());
  }
});

function currentBookingPayload() {
  const patientId =
    state.user.role === 'PATIENT' ? state.user.patientId : Number(document.getElementById('bookPatientSelect').value);
  return {
    patientId,
    doctorId: Number(document.getElementById('bookDoctorSelect').value),
    scheduledAt: document.getElementById('bookScheduledAt').value,
    reason: document.getElementById('bookReason').value,
  };
}

function bookOnce() {
  return apiFetch('/api/appointments', { method: 'POST', body: JSON.stringify(currentBookingPayload()) });
}

document.getElementById('concurrencyForm').addEventListener('submit', async (e) => {
  e.preventDefault();
  const count = Math.min(10, Math.max(2, Number(document.getElementById('concurrencyCount').value) || 5));
  const payload = currentBookingPayload();

  if (!payload.doctorId || !payload.scheduledAt) {
    showToast('Pick a doctor and a date/time in the form above first.', 'error');
    return;
  }

  showToast(`Firing ${count} simultaneous booking requests for the same slot...`, 'success');

  const attempts = Array.from({ length: count }, () =>
    apiFetch('/api/appointments', { method: 'POST', body: JSON.stringify(payload) })
      .then(() => 'SUCCESS')
      .catch((err) => (err.status === 409 ? 'CONFLICT' : 'ERROR'))
  );

  const results = await Promise.all(attempts);
  const successCount = results.filter((r) => r === 'SUCCESS').length;
  const conflictCount = results.filter((r) => r === 'CONFLICT').length;
  const otherCount = results.length - successCount - conflictCount;

  openModal(`
    <h2 id="modalTitle">Concurrency test results</h2>
    <dl>
      <dt>Requests fired</dt><dd>${count}</dd>
      <dt>Succeeded</dt><dd>${successCount}</dd>
      <dt>Rejected as double-booking (409)</dt><dd>${conflictCount}</dd>
      ${otherCount ? `<dt>Other errors</dt><dd>${otherCount}</dd>` : ''}
    </dl>
    <p class="hint">${
      successCount === 1
        ? 'Exactly one booking won the race, as expected - the pessimistic doctor-row lock did its job.'
        : 'Unexpected result - check the Appointments tab for what actually landed.'
    }</p>
  `);
  loadAppointments(currentFilters());
});

/* ---- appointments -----------------------------------------------------------*/

function currentFilters() {
  return {
    doctorId: document.getElementById('filterDoctorId').value || undefined,
    patientId: document.getElementById('filterPatientId').value || undefined,
    date: document.getElementById('filterDate').value || undefined,
  };
}

async function loadAppointments(filters = {}) {
  try {
    const params = new URLSearchParams();
    if (filters.doctorId) params.set('doctorId', filters.doctorId);
    if (filters.patientId) params.set('patientId', filters.patientId);
    if (filters.date) params.set('date', filters.date);
    const qs = params.toString();
    const list = await apiFetch('/api/appointments' + (qs ? '?' + qs : ''));
    state.appointments = list || [];
    renderAppointmentsList();
  } catch (e) {
    showToast(e.message, 'error');
  }
}

function renderAppointmentsList() {
  const container = document.getElementById('appointmentsList');
  if (!state.appointments.length) {
    container.innerHTML = '<p class="empty-state">No appointments to show.</p>';
    return;
  }
  container.innerHTML = state.appointments.map(renderAppointmentCard).join('');
}

function renderAppointmentCard(appt) {
  const isStaff = state.user.role !== 'PATIENT';
  const canCancel = appt.status === 'BOOKED';
  const canComplete = isStaff && appt.status === 'BOOKED';
  const altShape = appt.id % 2 === 0 ? ' sketch-alt' : '';
  return `
    <article class="card sketch${altShape}" data-appt-id="${appt.id}">
      <h3>${escapeHtml(appt.reason)}</h3>
      <p>${escapeHtml(appt.patientName)} with Dr. ${escapeHtml(appt.doctorName)}</p>
      <p class="meta">${formatDateTime(appt.scheduledAt)}</p>
      <span class="status-pill ${appt.status}">${appt.status}</span>
      <div class="card-actions">
        ${canCancel ? `<button class="btn btn-small btn-danger" data-action="cancel" data-appt-id="${appt.id}">Cancel</button>` : ''}
        ${canComplete ? `<button class="btn btn-small btn-success" data-action="complete" data-appt-id="${appt.id}">Complete</button>` : ''}
      </div>
    </article>`;
}

document.getElementById('appointmentsList').addEventListener('click', (e) => {
  const cancelBtn = e.target.closest('[data-action="cancel"]');
  if (cancelBtn) {
    confirmCancel(Number(cancelBtn.dataset.apptId));
    return;
  }
  const completeBtn = e.target.closest('[data-action="complete"]');
  if (completeBtn) {
    completeAppointment(Number(completeBtn.dataset.apptId));
    return;
  }
  const card = e.target.closest('.card');
  if (card) {
    const appt = state.appointments.find((a) => a.id === Number(card.dataset.apptId));
    if (appt) showAppointmentModal(appt);
  }
});

function showAppointmentModal(a) {
  openModal(`
    <h2 id="modalTitle">${escapeHtml(a.reason)}</h2>
    <dl>
      <dt>Patient</dt><dd>${escapeHtml(a.patientName)} (#${a.patientId})</dd>
      <dt>Doctor</dt><dd>Dr. ${escapeHtml(a.doctorName)} (#${a.doctorId})</dd>
      <dt>When</dt><dd>${formatDateTime(a.scheduledAt)}</dd>
      <dt>Status</dt><dd><span class="status-pill ${a.status}">${a.status}</span></dd>
    </dl>
  `);
}

function confirmCancel(id) {
  openModal(`
    <h2 id="modalTitle">Cancel this appointment?</h2>
    <p>This can't be undone.</p>
    <div class="card-actions">
      <button class="btn btn-danger" id="confirmCancelBtn">Yes, cancel it</button>
      <button class="btn btn-ghost" id="declineCancelBtn">No, keep it</button>
    </div>
  `);
  document.getElementById('confirmCancelBtn').addEventListener('click', async () => {
    closeModal();
    try {
      await apiFetch(`/api/appointments/${id}`, { method: 'DELETE' });
      showToast('Appointment cancelled.', 'success');
      loadAppointments(currentFilters());
    } catch (e) {
      showToast(e.message, 'error');
    }
  });
  document.getElementById('declineCancelBtn').addEventListener('click', closeModal);
}

async function completeAppointment(id) {
  try {
    await apiFetch(`/api/appointments/${id}/complete`, { method: 'PATCH' });
    showToast('Marked as completed.', 'success');
    loadAppointments(currentFilters());
  } catch (e) {
    showToast(e.message, 'error');
  }
}

document.getElementById('applyFiltersBtn').addEventListener('click', () => loadAppointments(currentFilters()));
document.getElementById('clearFiltersBtn').addEventListener('click', () => {
  document.getElementById('filterDoctorId').value = '';
  document.getElementById('filterPatientId').value = '';
  document.getElementById('filterDate').value = '';
  loadAppointments({});
});

/* ---- init ------------------------------------------------------------------*/

document.addEventListener('DOMContentLoaded', () => {
  const stored = localStorage.getItem(TOKEN_KEY);
  if (stored && applySession(stored)) {
    onLoginSuccess();
  } else {
    showAuthScreen();
  }
});
