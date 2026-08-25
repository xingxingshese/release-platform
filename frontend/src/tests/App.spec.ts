import { describe, it, expect } from 'vitest'
import { mount } from '@vue/test-utils'
import App from '../App.vue'
import { createMemoryHistory, createRouter } from 'vue-router'

function makeApp() {
  const router = createRouter({
    history: createMemoryHistory(),
    routes: [
      { path: '/', component: { template: '<p>dashboard</p>' } },
      { path: '/releases', component: { template: '<p>releases</p>' } },
      { path: '/alerts', component: { template: '<p>alerts</p>' } }
    ]
  })
  return { App, router }
}

describe('App', () => {
  it('renders navigation links', async () => {
    const { App, router } = makeApp()
    router.push('/')
    await router.isReady()
    const wrapper = mount(App, { global: { plugins: [router] } })
    expect(wrapper.findAll('a').map(a => a.text())).toEqual(
      expect.arrayContaining(['Dashboard', '发布', '报警'])
    )
  })
})
