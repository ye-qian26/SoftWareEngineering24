<template>
  <section class="panel">
    <div>
      <p class="label">Preview Upload</p>
      <h2>Temporary upload form</h2>
    </div>
    <label class="field">
      <span>Project name</span>
      <input v-model="projectName" type="text" placeholder="Enter a preview project name" />
    </label>
    <label class="field">
      <span>Requirement summary</span>
      <textarea
        v-model="requirement"
        rows="5"
        placeholder="Describe the page or feature you want to generate"
      />
    </label>
    <button type="button" @click="submitForm">Generate Preview</button>
    <p v-if="message" class="message">{{ message }}</p>
  </section>
</template>

<script setup>
import { ref } from 'vue'

const projectName = ref('')
const requirement = ref('')
const message = ref('')

const submitForm = () => {
  if (!projectName.value.trim() || !requirement.value.trim()) {
    message.value = 'Please fill in both fields before generating.'
    return
  }
  message.value = `Preview request queued for ${projectName.value.trim()}.`
}
</script>

<style scoped>
.panel {
  display: grid;
  gap: 18px;
}

.label {
  margin: 0 0 8px;
  color: #2563eb;
  font-size: 12px;
  font-weight: 700;
  letter-spacing: 0.08em;
  text-transform: uppercase;
}

h2 {
  margin: 0;
  font-size: 28px;
}

.field {
  display: grid;
  gap: 8px;
}

.field span {
  font-weight: 600;
}

input,
textarea {
  width: 100%;
  padding: 12px 14px;
  border: 1px solid #cbd5e1;
  border-radius: 14px;
  font: inherit;
  box-sizing: border-box;
}

button {
  width: fit-content;
  padding: 12px 18px;
  border: 0;
  border-radius: 14px;
  color: #ffffff;
  background: #0f172a;
  font: inherit;
  cursor: pointer;
}

.message {
  margin: 0;
  color: #475569;
}
</style>
