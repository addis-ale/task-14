<template>
  <main class="state-page card">
    <h1>403 - 无权限访问</h1>
    <p>Access denied for your active role.</p>
    <RouterLink to="/dashboard">返回主页 Back to dashboard</RouterLink>
  </main>
</template>

<style scoped>
.state-page {
  max-width: 620px;
  margin: 20px auto;
  padding: 24px;
  display: grid;
  gap: 10px;
}

h1 {
  margin: 0;
}

a {
  color: var(--color-primary);
}
</style>
