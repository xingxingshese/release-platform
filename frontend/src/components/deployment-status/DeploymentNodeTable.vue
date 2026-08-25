<template>
  <el-table :data="nodes" size="small" border v-loading="loading">
    <el-table-column prop="serviceName" label="服务" width="140" />
    <el-table-column prop="nodeName" label="实例/节点" width="160" />
    <el-table-column label="类型" width="90">
      <template #default="{ row }">{{ row.deploymentType }}</template>
    </el-table-column>
    <el-table-column label="副本 (期望/就绪/可用)" width="170">
      <template #default="{ row }">
        <span v-if="row.replicaDesired !== null">
          {{ row.replicaDesired }} / {{ row.replicaReady }} / {{ row.replicaAvailable }}
        </span>
        <span v-else>-</span>
      </template>
    </el-table-column>
    <el-table-column label="Health" width="90">
      <template #default="{ row }">
        <el-tag v-if="row.healthPassed === true" type="success" size="small">PASS</el-tag>
        <el-tag v-else-if="row.healthPassed === false" type="danger" size="small">FAIL</el-tag>
        <span v-else>-</span>
      </template>
    </el-table-column>
    <el-table-column label="Version" min-width="150">
      <template #default="{ row }">
        <span v-if="row.versionExpected">
          期望 {{ row.versionExpected }} / 实际 {{ row.versionActual ?? '未知' }}
        </span>
        <span v-else>-</span>
      </template>
    </el-table-column>
    <el-table-column label="判定" width="130">
      <template #default="{ row }">
        <StatusTag :status="row.result" />
      </template>
    </el-table-column>
    <el-table-column prop="message" label="信息" min-width="160" show-overflow-tooltip />
  </el-table>
</template>

<script setup lang="ts">
import type { DeploymentNode } from '../../types/deployment'
import StatusTag from '../status-tag/StatusTag.vue'

/** Pod / 部署节点状态表（规范 §三十一：每步可展开查看实例明细）。 */
defineProps<{ nodes: DeploymentNode[]; loading?: boolean }>()
</script>
