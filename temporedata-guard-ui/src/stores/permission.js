import { defineStore } from 'pinia'

/**
 * 权限状态(管理员/用户双工作台权限模型):
 * roles / permissions / dataScopes / securityLevel / projectScopes
 */
export const usePermissionStore = defineStore('permission', {
  state: () => ({
    roles: [],
    permissions: [],
    dataScopes: [],
    securityLevel: 'L4',
    projectScopes: [],
    loaded: false
  }),

  getters: {
    isAdminWorkspace: (s) => {
      const roles = s.roles.length ? s.roles : JSON.parse(localStorage.getItem('zqy_user') || '{}').roles || []
      return roles.some(r => /ADMIN|DEVELOPER|AUDITOR|SECURITY/i.test(String(r)))
    }
  },

  actions: {
    async load() {
      try {
        const { userApi } = await import('@/api/modules/user')
        const me = await userApi.me()
        const perms = await userApi.permissions()
        if (me) {
          this.roles = me.roles || []
          this.securityLevel = me.securityLevel || 'L4'
          this.dataScopes = me.dataScopes || []
          this.projectScopes = me.projectScopes || []
        }
        this.permissions = perms || []
        this.loaded = true
      } catch (e) { /* 离线兜底 */ }
    },

    loadFromUser(user) {
      if (!user) return
      this.roles = user.roles || []
      this.dataScopes = user.dataScopes || []
      this.securityLevel = user.securityLevel || 'L4'
      this.loaded = true
    },

    hasPermission(code) {
      return this.permissions.includes('*:*') || this.permissions.includes(code)
    },

    hasRole(role) {
      return this.roles.includes(role)
    },

    canAccessLevel(level) {
      const rank = { L1: 1, L2: 2, L3: 3, L4: 4 }
      return (rank[level] || 4) <= (rank[this.securityLevel] || 4)
    }
  }
})
