<template>
  <main class="state-page card">
    <h1>404 - 页面不存在</h1>
    <p>Page not found.</p>
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
