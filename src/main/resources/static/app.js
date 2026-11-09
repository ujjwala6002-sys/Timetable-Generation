const API_URL = '/api/students';
const state = { students: [], filtered: false, editingId: null };
const tableBody = document.querySelector('#student-table-body');
const form = document.querySelector('#student-form');
const formError = document.querySelector('#form-error');
const toast = document.querySelector('#toast');
let toastTimer;

function escapeHtml(value = '') {
    return String(value).replace(/[&<>"']/g, (character) => ({
        '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;'
    })[character]);
}

async function request(url, options = {}) {
    const response = await fetch(url, {
        ...options,
        headers: { ...(options.body ? { 'Content-Type': 'application/json' } : {}), ...options.headers }
    });
    const text = await response.text();
    let body = null;
    if (text) {
        try { body = JSON.parse(text); } catch { body = text; }
    }
    if (!response.ok) {
        const error = new Error(typeof body === 'object' && body?.message ? body.message : `Request failed (${response.status})`);
        error.status = response.status;
        error.details = typeof body === 'object' ? body?.fieldErrors : null;
        throw error;
    }
    return body;
}

function showToast(message, isError = false) {
    toast.textContent = message;
    toast.classList.toggle('error', isError);
    toast.classList.add('show');
    window.clearTimeout(toastTimer);
    toastTimer = window.setTimeout(() => toast.classList.remove('show'), 3200);
}

function initials(name = '') {
    return name.trim().split(/\s+/).slice(0, 2).map((part) => part[0] || '').join('').toUpperCase() || 'ST';
}

function render() {
    const students = state.students;
    document.querySelector('#total-stat').textContent = state.filtered ? String(state.allStudents?.length ?? students.length) : String(students.length);
    document.querySelector('#courses-stat').textContent = String(new Set((state.allStudents || students).map((student) => student.course)).size);
    document.querySelector('#showing-stat').textContent = String(students.length);
    document.querySelector('#nav-count').textContent = String(state.allStudents?.length ?? students.length);
    document.querySelector('#footer-count').textContent = `${students.length} ${students.length === 1 ? 'student' : 'students'} shown`;
    document.querySelector('#result-label').textContent = state.filtered ? 'Search result' : 'All students';
    document.querySelector('#show-all-button').hidden = !state.filtered;

    if (!students.length) {
        tableBody.innerHTML = '<tr><td colspan="4"><div class="empty-state"><span class="empty-mark">✳</span>No students here yet. Add your first student to get started.</div></td></tr>';
        return;
    }

    tableBody.innerHTML = students.map((student, index) => `
        <tr>
            <td><div class="student-cell"><span class="student-avatar" style="background:${['#f0edff','#e8f7f0','#fff0e6','#eaf2ff'][index % 4]};color:${['#705fe2','#3b9b77','#d7864d','#5481c5'][index % 4]}">${escapeHtml(initials(student.name))}</span><span><strong class="student-name">${escapeHtml(student.name)}</strong><small class="student-email">${escapeHtml(student.email)}</small></span></div></td>
            <td><span class="course-badge" title="${escapeHtml(student.course)}">${escapeHtml(student.course)}</span></td>
            <td class="phone-cell">${escapeHtml(student.phoneNumber ?? '')}</td>
            <td><div class="row-actions"><button class="row-action" type="button" data-action="edit" data-id="${student.id}" aria-label="Edit ${escapeHtml(student.name)}" title="Edit">✎</button><button class="row-action delete" type="button" data-action="delete" data-id="${student.id}" aria-label="Delete ${escapeHtml(student.name)}" title="Delete">×</button></div></td>
        </tr>`).join('');
}

async function loadStudents() {
    tableBody.innerHTML = '<tr><td class="loading-cell" colspan="4"><span class="loader"></span> Loading your directory…</td></tr>';
    try {
        state.allStudents = await request(API_URL);
        state.students = state.filtered ? state.students : state.allStudents;
        render();
    } catch (error) {
        tableBody.innerHTML = `<tr><td colspan="4" class="empty-state">${escapeHtml(error.message)}. Try refreshing the page.</td></tr>`;
        showToast(error.message, true);
    }
}

function resetForm() {
    form.reset();
    state.editingId = null;
    document.querySelector('#student-id').value = '';
    document.querySelector('#form-title').textContent = 'Add a student';
    document.querySelector('#form-eyebrow').textContent = 'GET STARTED';
    document.querySelector('#form-intro').textContent = 'Add someone new to your campus community.';
    document.querySelector('#submit-button').innerHTML = '<span class="plus-icon">+</span> Save student';
    document.querySelector('#cancel-edit-button').hidden = true;
    formError.hidden = true;
    formError.textContent = '';
}

function showFormError(error) {
    formError.replaceChildren();
    const message = document.createElement('span');
    message.textContent = error.message;
    formError.append(message);
    if (error.details && Object.keys(error.details).length) {
        const list = document.createElement('ul');
        Object.values(error.details).forEach((detail) => {
            const item = document.createElement('li');
            item.textContent = detail;
            list.append(item);
        });
        formError.append(list);
    }
    formError.hidden = false;
}

async function editStudent(id) {
    try {
        const student = await request(`${API_URL}/${id}`);
        state.editingId = student.id;
        document.querySelector('#student-id').value = student.id;
        form.elements.name.value = student.name;
        form.elements.email.value = student.email;
        form.elements.course.value = student.course;
        form.elements.phoneNumber.value = student.phoneNumber;
        document.querySelector('#form-title').textContent = 'Edit student';
        document.querySelector('#form-eyebrow').textContent = `STUDENT #${student.id}`;
        document.querySelector('#form-intro').textContent = 'Update this student’s directory details.';
        document.querySelector('#submit-button').textContent = 'Save changes';
        document.querySelector('#cancel-edit-button').hidden = false;
        formError.hidden = true;
        document.querySelector('#student-form-panel').scrollIntoView({ behavior: 'smooth', block: 'center' });
        form.elements.name.focus({ preventScroll: true });
    } catch (error) {
        showToast(error.message, true);
    }
}

form.addEventListener('submit', async (event) => {
    event.preventDefault();
    formError.hidden = true;
    if (!form.reportValidity()) return;
    const payload = {
        name: form.elements.name.value.trim(),
        email: form.elements.email.value.trim(),
        course: form.elements.course.value.trim(),
        phoneNumber: form.elements.phoneNumber.value.trim()
    };
    const submitButton = document.querySelector('#submit-button');
    submitButton.disabled = true;
    submitButton.textContent = state.editingId ? 'Saving…' : 'Adding…';
    try {
        const editingId = state.editingId;
        await request(editingId ? `${API_URL}/${editingId}` : API_URL, {
            method: editingId ? 'PUT' : 'POST',
            body: JSON.stringify(payload)
        });
        resetForm();
        state.filtered = false;
        await loadStudents();
        showToast(editingId ? 'Student details updated.' : 'Student added to your directory.');
    } catch (error) {
        showFormError(error);
    } finally {
        submitButton.disabled = false;
        if (state.editingId) submitButton.textContent = 'Save changes';
        else submitButton.innerHTML = '<span class="plus-icon">+</span> Save student';
    }
});

tableBody.addEventListener('click', async (event) => {
    const button = event.target.closest('button[data-action]');
    if (!button) return;
    const id = button.dataset.id;
    if (button.dataset.action === 'edit') {
        await editStudent(id);
        return;
    }
    const student = state.students.find((item) => String(item.id) === id);
    if (!student || !window.confirm(`Delete ${student.name} from the student directory?`)) return;
    try {
        await request(`${API_URL}/${id}`, { method: 'DELETE' });
        showToast('Student deleted.');
        state.filtered = false;
        await loadStudents();
    } catch (error) {
        showToast(error.message, true);
    }
});

document.querySelector('#search-form').addEventListener('submit', async (event) => {
    event.preventDefault();
    const id = document.querySelector('#search-id').value.trim();
    if (!id) {
        showToast('Enter a student ID to search.', true);
        return;
    }
    try {
        const student = await request(`${API_URL}/${encodeURIComponent(id)}`);
        state.students = [student];
        state.filtered = true;
        render();
    } catch (error) {
        state.students = [];
        state.filtered = true;
        render();
        showToast(error.message, true);
    }
});

document.querySelector('#show-all-button').addEventListener('click', () => {
    state.filtered = false;
    state.students = state.allStudents || [];
    document.querySelector('#search-id').value = '';
    render();
});
document.querySelector('#refresh-button').addEventListener('click', () => {
    state.filtered = false;
    document.querySelector('#search-id').value = '';
    loadStudents();
});
document.querySelector('#cancel-edit-button').addEventListener('click', resetForm);
document.querySelector('#add-student-shortcut').addEventListener('click', () => {
    resetForm();
    document.querySelector('#name').focus();
    document.querySelector('#student-form-panel').scrollIntoView({ behavior: 'smooth', block: 'center' });
});
document.querySelector('#today-label').textContent = new Intl.DateTimeFormat(undefined, {
    weekday: 'short', month: 'short', day: 'numeric'
}).format(new Date());

loadStudents();
