<template>
  <div class="app">
    <nav>
      <RouterLink to="/">Dashboard</RouterLink>
      <RouterLink to="/releases">发布</RouterLink>
      <RouterLink to="/alerts">报警</RouterLink>
    </nav>
    <main>
      <RouterView />
    </main>
  </div>
</template>

<script setup lang="ts">
import { RouterLink, RouterView } from 'vue-router'
</script>
