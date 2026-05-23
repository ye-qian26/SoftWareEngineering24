<template>
  <section class="panel">
    <span class="tag">Ready</span>
    <h2>Project scaffold is ready</h2>
    <p>
      The preview system has created a valid Vue 3 + Vite project. The AI can now focus on
      pages, components, and business content without rewriting the core scaffold.
    </p>
  </section>
</template>

<style scoped>
.panel {
  display: grid;
  gap: 16px;
}

.tag {
  width: fit-content;
  padding: 6px 12px;
  border-radius: 999px;
  color: #166534;
  background: #dcfce7;
  font-size: 12px;
  font-weight: 700;
  letter-spacing: 0.08em;
  text-transform: uppercase;
}

h2 {
  margin: 0;
  font-size: 28px;
}

p {
  margin: 0;
  max-width: 720px;
  color: #475569;
  line-height: 1.7;
}
</style>
