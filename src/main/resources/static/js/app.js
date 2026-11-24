// Secure Task Manager - frontend
// Talks to the Spring Boot API on the SAME origin (served as static resources by the
// backend itself), so there is no CORS to configure - fetch() just works.

const state = {
  token: sessionStorage.getItem("taskapi_token") || null,
  role: sessionStorage.getItem("taskapi_role") || null,
  email: sessionStorage.getItem("taskapi_email") || null,
  page: 0,
  pageSize: 6,
  totalPages: 1,
  editingTaskId: null,
};

const el = (id) => document.getElementById(id);

// ---------------- API helper ----------------

async function api(path, options = {}) {
  const headers = { "Content-Type": "application/json", ...(options.headers || {}) };
  if (state.token) headers["Authorization"] = "Bearer " + state.token;

  const res = await fetch(path, { ...options, headers });

  if (res.status === 204) return null;

  let body = null;
  try { body = await res.json(); } catch (e) { /* no body */ }

  if (!res.ok) {
    if (res.status === 401 && state.token) {
      // Token expired/invalid mid-session - bounce back to login.
      clearSession();
      showAuthView();
      toast("Your session expired - please log in again.", "error");
    }
    const err = new Error(body?.message || "Request failed");
    err.status = res.status;
    err.fieldErrors = body?.fieldErrors || null;
    throw err;
  }
  return body;
}

// ---------------- Session ----------------

function saveSession(token, role, email) {
  state.token = token;
  state.role = role;
  state.email = email;
  sessionStorage.setItem("taskapi_token", token);
  sessionStorage.setItem("taskapi_role", role);
  sessionStorage.setItem("taskapi_email", email);
}

function clearSession() {
  state.token = null;
  state.role = null;
  state.email = null;
  sessionStorage.removeItem("taskapi_token");
  sessionStorage.removeItem("taskapi_role");
  sessionStorage.removeItem("taskapi_email");
}

// ---------------- View switching ----------------

function showAuthView() {
  el("authView").classList.remove("hidden");
  el("appView").classList.add("hidden");
  el("userBar").classList.add("hidden");
}

function showAppView() {
  el("authView").classList.add("hidden");
  el("appView").classList.remove("hidden");
  el("userBar").classList.remove("hidden");
  el("userLabel").textContent = `${state.email} · ${state.role}`;
  el("adminNavBtn").classList.toggle("hidden", state.role !== "ADMIN");
  switchNav("tasks");
  loadTasks();
}

function switchNav(view) {
  document.querySelectorAll(".nav-link").forEach((btn) =>
    btn.classList.toggle("active", btn.dataset.view === view));
  el("tasksPanel").classList.toggle("hidden", view !== "tasks");
  el("adminPanel").classList.toggle("hidden", view !== "admin");
  if (view === "admin") loadAdminData();
}

// ---------------- Toast ----------------

let toastTimer = null;
function toast(message, kind = "success") {
  const t = el("toast");
  t.textContent = message;
  t.className = "toast " + kind;
  clearTimeout(toastTimer);
  toastTimer = setTimeout(() => t.classList.add("hidden"), 3200);
}

// ---------------- Auth forms ----------------

document.querySelectorAll(".tab-btn").forEach((btn) => {
  btn.addEventListener("click", () => {
    document.querySelectorAll(".tab-btn").forEach((b) => b.classList.remove("active"));
    btn.classList.add("active");
    const isLogin = btn.dataset.tab === "login";
    el("loginForm").classList.toggle("hidden", !isLogin);
    el("registerForm").classList.toggle("hidden", isLogin);
  });
});

el("loginForm").addEventListener("submit", async (e) => {
  e.preventDefault();
  el("loginError").textContent = "";
  try {
    const data = await api("/api/auth/login", {
      method: "POST",
      body: JSON.stringify({
        email: el("loginEmail").value,
        password: el("loginPassword").value,
      }),
    });
    saveSession(data.token, data.role, el("loginEmail").value);
    showAppView();
    toast("Welcome back!");
  } catch (err) {
    el("loginError").textContent = err.message;
  }
});

el("registerForm").addEventListener("submit", async (e) => {
  e.preventDefault();
  el("registerError").textContent = "";
  try {
    await api("/api/auth/register", {
      method: "POST",
      body: JSON.stringify({
        name: el("registerName").value,
        email: el("registerEmail").value,
        password: el("registerPassword").value,
      }),
    });
    toast("Account created - log in now.");
    document.querySelector('.tab-btn[data-tab="login"]').click();
    el("loginEmail").value = el("registerEmail").value;
    el("registerForm").reset();
  } catch (err) {
    const fieldMsg = err.fieldErrors ? Object.values(err.fieldErrors).join(" / ") : null;
    el("registerError").textContent = fieldMsg || err.message;
  }
});

el("logoutBtn").addEventListener("click", () => {
  clearSession();
  showAuthView();
});

// ---------------- Nav ----------------

document.querySelectorAll(".nav-link").forEach((btn) =>
  btn.addEventListener("click", () => switchNav(btn.dataset.view)));

// ---------------- Tasks ----------------

async function loadTasks() {
  const status = el("statusFilter").value;
  const sort = el("sortOrder").value;
  const params = new URLSearchParams({ page: state.page, size: state.pageSize, sort });
  if (status) params.set("status", status);

  try {
    const data = await api(`/api/tasks?${params.toString()}`);
    state.totalPages = data.totalPages || 1;
    renderTasks(data.content || []);
    el("pageLabel").textContent = `Page ${data.number + 1} of ${Math.max(data.totalPages, 1)}`;
    el("prevPage").disabled = data.first;
    el("nextPage").disabled = data.last;
  } catch (err) {
    toast(err.message, "error");
  }
}

function renderTasks(tasks) {
  const grid = el("taskGrid");
  grid.innerHTML = "";
  el("taskEmpty").classList.toggle("hidden", tasks.length > 0);

  for (const t of tasks) {
    const card = document.createElement("div");
    card.className = "task-card glass";
    card.innerHTML = `
      <span class="status-pill status-${t.status}">${t.status.replace("_", " ")}</span>
      <h4></h4>
      <p></p>
      <span class="task-meta">Created ${new Date(t.createdAt).toLocaleString()}</span>
      <div class="task-actions">
        <button class="btn btn-ghost btn-small" data-action="edit">Edit</button>
        <button class="btn btn-danger-outline btn-small" data-action="delete">Delete</button>
      </div>
    `;
    card.querySelector("h4").textContent = t.title;
    card.querySelector("p").textContent = t.description || "(no description)";
    card.querySelector('[data-action="edit"]').addEventListener("click", () => openTaskModal(t));
    card.querySelector('[data-action="delete"]').addEventListener("click", () => deleteTask(t.id));
    grid.appendChild(card);
  }
}

el("statusFilter").addEventListener("change", () => { state.page = 0; loadTasks(); });
el("sortOrder").addEventListener("change", () => { state.page = 0; loadTasks(); });
el("prevPage").addEventListener("click", () => { if (state.page > 0) { state.page--; loadTasks(); } });
el("nextPage").addEventListener("click", () => { if (state.page < state.totalPages - 1) { state.page++; loadTasks(); } });

async function deleteTask(id) {
  if (!confirm("Delete this task?")) return;
  try {
    await api(`/api/tasks/${id}`, { method: "DELETE" });
    toast("Task deleted.");
    loadTasks();
  } catch (err) {
    toast(err.message, "error");
  }
}

// ---------------- Task modal ----------------

function openTaskModal(task = null) {
  state.editingTaskId = task ? task.id : null;
  el("modalTitle").textContent = task ? "Edit Task" : "New Task";
  el("taskTitle").value = task ? task.title : "";
  el("taskDescription").value = task ? (task.description || "") : "";
  el("statusFieldWrap").classList.toggle("hidden", !task);
  if (task) el("taskStatus").value = task.status;
  el("taskFormError").textContent = "";
  el("taskModal").classList.remove("hidden");
}

function closeTaskModal() {
  el("taskModal").classList.add("hidden");
}

el("newTaskBtn").addEventListener("click", () => openTaskModal(null));
el("cancelTaskBtn").addEventListener("click", closeTaskModal);

el("taskForm").addEventListener("submit", async (e) => {
  e.preventDefault();
  el("taskFormError").textContent = "";

  const title = el("taskTitle").value;
  const description = el("taskDescription").value;

  try {
    if (state.editingTaskId) {
      await api(`/api/tasks/${state.editingTaskId}`, {
        method: "PUT",
        body: JSON.stringify({ title, description, status: el("taskStatus").value }),
      });
      toast("Task updated.");
    } else {
      await api("/api/tasks", {
        method: "POST",
        body: JSON.stringify({ title, description }),
      });
      toast("Task created.");
    }
    closeTaskModal();
    loadTasks();
  } catch (err) {
    const fieldMsg = err.fieldErrors ? Object.values(err.fieldErrors).join(" / ") : null;
    el("taskFormError").textContent = fieldMsg || err.message;
  }
});

// ---------------- Admin ----------------

async function loadAdminData() {
  try {
    const [users, tasks] = await Promise.all([
      api("/api/admin/users?size=50"),
      api("/api/admin/tasks?size=50"),
    ]);
    renderAdminUsers(users.content || []);
    renderAdminTasks(tasks.content || []);
  } catch (err) {
    toast(err.message, "error");
  }
}

function renderAdminUsers(users) {
  const tbody = document.querySelector("#usersTable tbody");
  tbody.innerHTML = "";
  for (const u of users) {
    const tr = document.createElement("tr");
    tr.innerHTML = `<td>${u.id}</td><td></td><td></td><td>${u.role}</td><td>${new Date(u.createdAt).toLocaleDateString()}</td>`;
    tr.children[1].textContent = u.email;
    tr.children[2].textContent = u.name;
    tbody.appendChild(tr);
  }
}

function renderAdminTasks(tasks) {
  const tbody = document.querySelector("#allTasksTable tbody");
  tbody.innerHTML = "";
  for (const t of tasks) {
    const tr = document.createElement("tr");
    tr.innerHTML = `<td>${t.id}</td><td></td><td><span class="status-pill status-${t.status}">${t.status.replace("_", " ")}</span></td><td>${t.ownerId}</td><td>${new Date(t.createdAt).toLocaleDateString()}</td>`;
    tr.children[1].textContent = t.title;
    tbody.appendChild(tr);
  }
}

// ---------------- Init ----------------

if (state.token) {
  showAppView();
} else {
  showAuthView();
}
