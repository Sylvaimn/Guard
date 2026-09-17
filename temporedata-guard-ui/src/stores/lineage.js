import { defineStore } from 'pinia'
import { lineageApi } from '@/api/modules/lineage'

/**
 * Lineage module state: graph data, root node, filters, layout, selection.
 */
export const useLineageStore = defineStore('lineage', {
  state: () => ({
    rootNodeId: null,
    isOverview: false,
    graphData: null,
    selectedNode: null,
    layoutMode: 'layer',
    graphLevel: 'table',
    graphDirection: 'both',
    graphDepth: 3,
    nodeTypeFilter: null,
    graphLoading: false,
    nodeNameMap: {},
    wsEvent: null
  }),
  getters: {
    fieldLinks: (state) => {
      if (!state.selectedNode || !state.graphData) return []
      const nodeId = state.selectedNode.id
      return state.graphData.links
        .filter(l => (l.source === nodeId || l.target === nodeId) && l.fieldLinks?.length)
        .flatMap(l => l.fieldLinks || [])
    }
  },
  actions: {
    async loadGraph() {
      const tid = this.rootNodeId
      if (!tid) return
      this.graphLoading = true
      this.selectedNode = null
      try {
        const data = await lineageApi.getGraph({
          nodeId: tid,
          direction: this.graphDirection,
          depth: this.graphDepth,
          level: this.graphLevel,
          nodeTypes: this.nodeTypeFilter || undefined
        })
        if (this.rootNodeId !== tid) return
        this.graphData = data
        this.isOverview = false
        this.buildNameMap(data)
      } catch (e) {
        if (this.rootNodeId === tid) throw e
      } finally {
        if (this.rootNodeId === tid) this.graphLoading = false
      }
    },
    async loadOverview() {
      this.isOverview = true
      this.rootNodeId = null
      this.selectedNode = null
      this.graphLoading = true
      try {
        const data = await lineageApi.getOverview()
        this.graphData = data
        this.buildNameMap(data)
      } finally {
        this.graphLoading = false
      }
    },
    buildNameMap(data) {
      this.nodeNameMap = {}
      for (const n of data.nodes || []) this.nodeNameMap[n.id] = n.name
    },
    selectNode(id) {
      if (this.rootNodeId === id) return
      this.rootNodeId = id
      if (id) {
        this.loadGraph()
      } else {
        this.graphData = null
        this.selectedNode = null
        this.graphLoading = false
      }
    },
    refocus(id) {
      this.rootNodeId = id
      this.isOverview = false
      this.selectedNode = null
      return this.loadGraph()
    },
    clearSelection() {
      this.selectedNode = null
    }
  }
})