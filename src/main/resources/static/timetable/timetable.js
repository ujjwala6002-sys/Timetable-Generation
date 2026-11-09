const API = '/api';
const DAYS = ['MON', 'TUE', 'WED', 'THU', 'FRI'];
const COLORS = ['lilac', 'mint', 'peach', 'blue'];
const $ = (selector) => document.querySelector(selector);
const toast = $('#toast');
let toastTimer;

async function api(path, options = {}) {
  const response = await fetch(`${API}${path}`, options);
  const text = await response.text();
  let body;
  try { body = text ? JSON.parse(text) : null; } catch { body = text; }
  if (!response.ok) throw new Error(body?.message || `Request failed (${response.status})`);
  return body;
}
function notify(message, error = false) {
  toast.textContent = message;
  toast.classList.toggle('error', error);
  toast.classList.add('show');
  clearTimeout(toastTimer);
  toastTimer = setTimeout(() => toast.classList.remove('show'), 3200);
}
function escapeHtml(value = '') {
  return String(value).replace(/[&<>"']/g, (ch) => ({ '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;' })[ch]);
}
async function loadClasses() {
  try {
    const classes = await api('/classes');
    const select = $('#class-select');
    select.innerHTML = classes.length ? classes.map((item) => `<option value="${item.id}">${escapeHtml(item.name)}</option>`).join('') : '<option value="">No classes available</option>';
    await loadSchedule();
  } catch (error) {
    $('#schedule-body').innerHTML = `<tr><td class="loading" colspan="6">${escapeHtml(error.message)}</td></tr>`;
    notify(error.message, true);
  }
}
function renderSchedule(entries) {
  const bySlot = new Map();
  for (const entry of entries) bySlot.set(`${entry.timeSlot.day}:${entry.timeSlot.period}`, entry);
  const rows = [];
  for (let period = 1; period <= 6; period++) {
    const cells = DAYS.map((day) => {
      const entry = bySlot.get(`${day}:${period}`);
      if (!entry) return '<td><div class="empty-slot">—</div></td>';
      const color = COLORS[(entry.subject.id - 1) % COLORS.length];
      return `<td><div class="lesson ${color}"><b>${escapeHtml(entry.subject.name)}</b><small>${escapeHtml(entry.teacher.name)}</small><em>${escapeHtml(entry.room.name)}</em></div></td>`;
    }).join('');
    rows.push(`<tr><td class="period-cell">PERIOD ${period}</td>${cells}</tr>`);
  }
  $('#schedule-body').innerHTML = rows.join('');
  $('#schedule-count').textContent = `${entries.length} ${entries.length === 1 ? 'lesson' : 'lessons'}`;
}
async function loadSchedule() {
  const id = $('#class-select').value;
  const name = $('#class-select').selectedOptions[0]?.textContent || 'Class timetable';
  $('#schedule-title').textContent = `${name} timetable`;
  if (!id) {
    $('#schedule-body').innerHTML = '<tr><td class="loading" colspan="6">Add a class to get started.</td></tr>';
    $('#schedule-count').textContent = '0 lessons';
    return;
  }
  $('#schedule-body').innerHTML = '<tr><td class="loading" colspan="6">Loading schedule…</td></tr>';
  try {
    const entries = await api(`/timetable/class/${encodeURIComponent(id)}`);
    renderSchedule(entries);
  } catch (error) {
    $('#schedule-body').innerHTML = `<tr><td class="loading" colspan="6">${escapeHtml(error.message)}</td></tr>`;
    notify(error.message, true);
  }
}
$('#class-select').addEventListener('change', loadSchedule);
$('#refresh').addEventListener('click', loadSchedule);
$('#generate').addEventListener('click', async () => {
  const button = $('#generate');
  button.disabled = true;
  try {
    const result = await api('/timetable/generate', { method: 'POST' });
    notify(`Generated ${result.scheduledCount}; ${result.unscheduledCount} unscheduled.`, result.unscheduledCount > 0);
    await loadSchedule();
  } catch (error) {
    notify(error.message, true);
  } finally { button.disabled = false; }
});
loadClasses();
