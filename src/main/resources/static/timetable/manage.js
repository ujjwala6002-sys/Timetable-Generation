const API = '/api';
const TYPES = {
  teachers: { path: '/teachers', noun: 'teacher' },
  classes: { path: '/classes', noun: 'class' },
  rooms: { path: '/rooms', noun: 'room' },
  subjects: { path: '/subjects', noun: 'subject' }
};
const COLORS = ['lilac', 'mint', 'peach', 'blue'];
const $ = (selector) => document.querySelector(selector);
const toast = $('#toast');
let toastTimer;
async function api(path, options = {}) {
  const response = await fetch(`${API}${path}`, { ...options, headers: { ...(options.body ? { 'Content-Type': 'application/json' } : {}), ...options.headers } });
  const text = await response.text();
  let body;
  try { body = text ? JSON.parse(text) : null; } catch { body = text; }
  if (!response.ok) throw new Error(body?.message || `Request failed (${response.status})`);
  return body;
}
function escapeHtml(value = '') { return String(value).replace(/[&<>"']/g, (ch) => ({ '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;' })[ch]); }
function notify(message, error = false) {
  toast.textContent = message;
  toast.classList.toggle('error', error);
  toast.classList.add('show');
  clearTimeout(toastTimer);
  toastTimer = setTimeout(() => toast.classList.remove('show'), 3200);
}
async function loadTeachers() {
  try {
    const teachers = await api('/teachers');
    $('#teachers-count').textContent = teachers.length;
    $('#teachers-list').innerHTML = teachers.length ? teachers.map((item, index) => `<div class="data-row"><span class="manage-icon ${COLORS[index % COLORS.length]}">♙</span><b>${escapeHtml(item.name)}</b><small>#${item.id}</small><button type="button" data-delete="teachers" data-id="${item.id}" aria-label="Delete ${escapeHtml(item.name)}">Remove</button></div>`).join('') : '<div class="empty-list">No teachers yet. Add one above.</div>';
    const select = $('#teacher-select');
    const previous = select.value;
    select.innerHTML = '<option value="">Choose teacher</option>' + teachers.map((item) => `<option value="${item.id}">${escapeHtml(item.name)}</option>`).join('');
    if (teachers.some((item) => String(item.id) === previous)) select.value = previous;
  } catch (error) { notify(error.message, true); }
}
async function loadKind(kind) {
  try {
    const items = await api(TYPES[kind].path);
    $(`#${kind}-count`).textContent = items.length;
    $(`#${kind}-list`).innerHTML = items.length ? items.map((item, index) => {
      let detail = '';
      if (kind === 'rooms') detail = `${item.capacity} seats`;
      if (kind === 'subjects') detail = `${item.periodsPerWeek} periods · ${item.teacher?.name || 'No teacher'}`;
      return `<div class="data-row"><span class="manage-icon ${COLORS[index % COLORS.length]}">${kind === 'classes' ? '▦' : kind === 'rooms' ? '⌂' : '▤'}</span><b>${escapeHtml(item.name)}</b>${detail ? `<small>${escapeHtml(detail)}</small>` : `<small>#${item.id}</small>`}<button type="button" data-delete="${kind}" data-id="${item.id}" aria-label="Delete ${escapeHtml(item.name)}">Remove</button></div>`;
    }).join('') : `<div class="empty-list">No ${TYPES[kind].noun}s yet. Add one above.</div>`;
  } catch (error) { notify(error.message, true); }
}
async function loadAll() {
  await loadTeachers();
  await Promise.all([loadKind('classes'), loadKind('rooms'), loadKind('subjects'), loadSlots()]);
}
async function loadSlots() {
  try {
    const slots = await api('/time-slots');
    $('#slots-count').textContent = `${slots.length} slots`;
  } catch { $('#slots-count').textContent = '0 slots'; }
}
for (const [kind, config] of Object.entries(TYPES)) {
  $(`#${kind}-form`).addEventListener('submit', async (event) => {
    event.preventDefault();
    const form = event.currentTarget;
    const values = new FormData(form);
    const payload = { name: String(values.get('name') || '').trim() };
    if (kind === 'rooms') payload.capacity = Number(values.get('capacity'));
    if (kind === 'subjects') {
      payload.periodsPerWeek = Number(values.get('periodsPerWeek'));
      payload.teacher = { id: Number(values.get('teacherId')) };
    }
    const submit = form.querySelector('button[type="submit"]');
    submit.disabled = true;
    try {
      await api(config.path, { method: 'POST', body: JSON.stringify(payload) });
      form.reset();
      if (kind === 'teachers') await loadTeachers();
      else await loadKind(kind);
      notify(`${config.noun[0].toUpperCase()}${config.noun.slice(1)} added.`);
    } catch (error) { notify(error.message, true); }
    finally { submit.disabled = false; }
  });
}
document.addEventListener('click', async (event) => {
  const button = event.target.closest('button[data-delete]');
  if (!button) return;
  const kind = button.dataset.delete;
  if (!window.confirm(`Remove this ${TYPES[kind].noun}?`)) return;
  button.disabled = true;
  try {
    await api(`${TYPES[kind].path}/${button.dataset.id}`, { method: 'DELETE' });
    if (kind === 'teachers') await loadTeachers();
    else await loadKind(kind);
    notify(`${TYPES[kind].noun[0].toUpperCase()}${TYPES[kind].noun.slice(1)} removed.`);
  } catch (error) { notify(error.message, true); button.disabled = false; }
});
loadAll();
