const API = '/api';
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
  toastTimer = setTimeout(() => toast.classList.remove('show'), 3500);
}
async function loadStats() {
  try {
    const stats = await api('/dashboard/stats');
    $('#teachers').textContent = stats.teachers;
    $('#classes').textContent = stats.classes;
    $('#rooms').textContent = stats.rooms;
    $('#subjects').textContent = stats.subjects;
    $('#entries').textContent = stats.entries;
    $('#conflicts').textContent = stats.conflicts;
    $('#utilization').textContent = `${stats.roomUtilizationPercent}%`;
    $('#utilization-bar').style.width = `${Math.min(100, stats.roomUtilizationPercent)}%`;
    $('#overview-note').textContent = stats.conflicts === 0 ? 'All lessons have a unique room, teacher, and class slot.' : 'Review your schedule for overlapping lessons.';
  } catch (error) {
    $('#overview-note').textContent = `Could not load dashboard: ${error.message}`;
  }
}
$('#generate').addEventListener('click', async () => {
  const button = $('#generate');
  button.disabled = true;
  $('#generate-status').textContent = 'Building your schedule…';
  try {
    const result = await api('/timetable/generate', { method: 'POST' });
    const message = `Generated: ${result.scheduledCount} scheduled, ${result.unscheduledCount} unscheduled.`;
    $('#generate-status').textContent = message;
    notify(message, result.unscheduledCount > 0);
    await loadStats();
  } catch (error) {
    $('#generate-status').textContent = error.message;
    notify(error.message, true);
  } finally {
    button.disabled = false;
  }
});
loadStats();
