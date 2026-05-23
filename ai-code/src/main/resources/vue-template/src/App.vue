<template>
  <div class="app-shell">
    <header class="topbar">
      <div>
        <p class="eyebrow">AI Generated Preview</p>
        <h1>Vue Preview Workspace</h1>
      </div>
      <nav class="nav">
        <RouterLink to="/">Home</RouterLink>
        <RouterLink to="/upload">Upload</RouterLink>
      </nav>
    </header>
    <main class="content">
      <RouterView />
    </main>
  </div>
</template>

<style scoped>
.app-shell {
  min-height: 100vh;
  padding: 32px;
  background:
    radial-gradient(circle at top left, rgba(37, 99, 235, 0.16), transparent 32%),
    linear-gradient(180deg, #f8fafc 0%, #e2e8f0 100%);
  color: #0f172a;
  font-family: "Segoe UI", "PingFang SC", sans-serif;
}

.topbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 24px;
  margin-bottom: 28px;
}

.eyebrow {
  margin: 0 0 8px;
  color: #2563eb;
  font-size: 12px;
  font-weight: 700;
  letter-spacing: 0.12em;
  text-transform: uppercase;
}

h1 {
  margin: 0;
  font-size: 32px;
}

.nav {
  display: flex;
  gap: 12px;
}

.nav a {
  padding: 10px 16px;
  border-radius: 999px;
  color: #1e293b;
  background: rgba(255, 255, 255, 0.72);
  text-decoration: none;
}

.nav a.router-link-active {
  color: #ffffff;
  background: #2563eb;
}

.content {
  padding: 24px;
  border-radius: 24px;
  background: rgba(255, 255, 255, 0.85);
  box-shadow: 0 24px 60px rgba(15, 23, 42, 0.12);
}

@media (max-width: 720px) {
  .app-shell {
    padding: 20px;
  }

  .topbar {
    flex-direction: column;
    align-items: flex-start;
  }
}
</style>
