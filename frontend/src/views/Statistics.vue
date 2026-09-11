<template>
  <div class="page-container">
    <h2 class="page-title">统计报表</h2>
    
    <!-- 班级选择 -->
    <div class="search-form">
      <el-form :inline="true" :model="searchForm">
        <el-form-item label="选择班级">
          <el-select v-model="searchForm.classId" placeholder="请选择班级" @change="handleClassChange">
            <el-option v-for="c in classes" :key="c.id" :label="c.className" :value="c.id" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="loadStatistics">查询</el-button>
          <el-button type="success" @click="handleExport" :loading="exporting">
            <el-icon><Download /></el-icon>
            导出成绩
          </el-button>
        </el-form-item>
      </el-form>
    </div>
    
    <!-- 统计卡片 -->
    <el-row :gutter="20" v-if="statistics">
      <el-col :span="6">
        <div class="stat-card">
          <div class="stat-icon" style="background: #409eff">
            <el-icon><User /></el-icon>
          </div>
          <div class="stat-info">
            <p class="stat-label">班级人数</p>
            <p class="stat-value">{{ statistics.totalStudents }}</p>
          </div>
        </div>
      </el-col>
      <el-col :span="6">
        <div class="stat-card">
          <div class="stat-icon" style="background: #67c23a">
            <el-icon><Document /></el-icon>
          </div>
          <div class="stat-info">
            <p class="stat-label">提交报告</p>
            <p class="stat-value">{{ statistics.totalReports }}</p>
          </div>
        </div>
      </el-col>
      <el-col :span="6">
        <div class="stat-card">
          <div class="stat-icon" style="background: #e6a23c">
            <el-icon><Star /></el-icon>
          </div>
          <div class="stat-info">
            <p class="stat-label">已评价</p>
            <p class="stat-value">{{ statistics.evaluatedReports }}</p>
          </div>
        </div>
      </el-col>
      <el-col :span="6">
        <div class="stat-card">
          <div class="stat-icon" style="background: #f56c6c">
            <el-icon><TrendCharts /></el-icon>
          </div>
          <div class="stat-info">
            <p class="stat-label">评价完成率</p>
            <p class="stat-value">{{ completionRate }}%</p>
          </div>
        </div>
      </el-col>
    </el-row>
    
    <!-- 分数统计 -->
    <el-row :gutter="20" style="margin-top: 20px" v-if="statistics">
      <el-col :span="6">
        <div class="score-stat-card">
          <p class="label">平均分</p>
          <p class="value primary">{{ statistics.avgScore }}</p>
        </div>
      </el-col>
      <el-col :span="6">
        <div class="score-stat-card">
          <p class="label">最高分</p>
          <p class="value success">{{ statistics.maxScore }}</p>
        </div>
      </el-col>
      <el-col :span="6">
        <div class="score-stat-card">
          <p class="label">最低分</p>
          <p class="value danger">{{ statistics.minScore }}</p>
        </div>
      </el-col>
      <el-col :span="6">
        <div class="score-stat-card">
          <p class="label">优良率</p>
          <p class="value warning">{{ excellentRate }}%</p>
        </div>
      </el-col>
    </el-row>
    
    <!-- 图表 -->
    <el-row :gutter="20" style="margin-top: 20px">
      <el-col :span="12">
        <div class="chart-container">
          <div class="chart-title">分数段分布</div>
          <div ref="barChartRef" style="height: 300px"></div>
        </div>
      </el-col>
      <el-col :span="12">
        <div class="chart-container">
          <div class="chart-title">成绩分布饼图</div>
          <div ref="pieChartRef" style="height: 300px"></div>
        </div>
      </el-col>
    </el-row>
    
    <!-- 成绩列表 -->
    <div class="table-container" style="margin-top: 20px" v-if="statistics">
      <div class="table-title">成绩明细</div>
      <el-table :data="scoreList" stripe>
        <el-table-column prop="studentName" label="学生" />
        <el-table-column prop="className" label="班级" />
        <el-table-column prop="reportTitle" label="报告" show-overflow-tooltip />
        <el-table-column label="完整性" prop="completeness_score" width="100" />
        <el-table-column label="规范性" prop="specification_score" width="100" />
        <el-table-column label="知识点" prop="knowledge_score" width="100" />
        <el-table-column label="总分" prop="total_score" width="100">
          <template #default="{ row }">
            <el-tag :type="getScoreType(row.total_score)">{{ row.total_score }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="evaluate_time" label="评价时间" width="180" />
      </el-table>
    </div>
    
    <el-empty v-if="!statistics" description="请选择班级查看统计信息" />
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted, onUnmounted, watch } from 'vue'
import * as echarts from 'echarts'
import { ElMessage } from 'element-plus'
import { getClassStatistics, exportScores } from '../api/statistics'
import { getAllClasses } from '../api/class'

const barChartRef = ref()
const pieChartRef = ref()
let barChart = null
let pieChart = null

const loading = ref(false)
const exporting = ref(false)
const classes = ref([])
const statistics = ref(null)
const scoreList = ref([])

const searchForm = reactive({ classId: null })

const completionRate = computed(() => {
  if (!statistics.value || !statistics.value.totalReports) return 0
  return Math.round(statistics.value.evaluatedReports / statistics.value.totalReports * 100)
})

const excellentRate = computed(() => {
  if (!statistics.value || !statistics.value.evaluatedReports) return 0
  const excellent = (statistics.value.scoreDistribution || [])
    .filter(d => ['80-90分', '90-100分'].includes(d.range))
    .reduce((sum, d) => sum + d.count, 0)
  return Math.round(excellent / statistics.value.evaluatedReports * 100)
})

const getScoreType = (score) => {
  if (!score) return 'info'
  if (score >= 90) return 'success'
  if (score >= 80) return ''
  if (score >= 60) return 'warning'
  return 'danger'
}

const initBarChart = () => {
  if (!barChartRef.value || !statistics.value) return
  
  barChart = echarts.init(barChartRef.value)
  const distribution = statistics.value.scoreDistribution || []
  
  const option = {
    tooltip: { trigger: 'axis' },
    xAxis: {
      type: 'category',
      data: distribution.map(d => d.range)
    },
    yAxis: { type: 'value', name: '人数' },
    series: [{
      type: 'bar',
      data: distribution.map(d => d.count),
      itemStyle: {
        color: (params) => {
          const colors = ['#f56c6c', '#e6a23c', '#409eff', '#67c23a', '#909399']
          return colors[params.dataIndex]
        }
      },
      barWidth: '50%',
      label: { show: true, position: 'top' }
    }]
  }
  barChart.setOption(option)
}

const initPieChart = () => {
  if (!pieChartRef.value || !statistics.value) return
  
  pieChart = echarts.init(pieChartRef.value)
  const distribution = statistics.value.scoreDistribution || []
  
  const option = {
    tooltip: { trigger: 'item', formatter: '{b}: {c}人 ({d}%)' },
    legend: { bottom: '5%', left: 'center' },
    series: [{
      type: 'pie',
      radius: ['40%', '70%'],
      avoidLabelOverlap: false,
      label: { show: true, formatter: '{b}\n{c}人' },
      data: distribution.map((d, i) => {
        const colors = ['#f56c6c', '#e6a23c', '#409eff', '#67c23a', '#909399']
        return { value: d.count, name: d.range, itemStyle: { color: colors[i] } }
      })
    }]
  }
  pieChart.setOption(option)
}

const loadStatistics = async () => {
  if (!searchForm.classId) {
    ElMessage.warning('请选择班级')
    return
  }
  
  loading.value = true
  try {
    const res = await getClassStatistics(searchForm.classId)
    statistics.value = res.data
    scoreList.value = res.data.scoreList || []
    
    setTimeout(() => {
      initBarChart()
      initPieChart()
    }, 100)
  } catch (error) {
    console.error(error)
  } finally {
    loading.value = false
  }
}

const handleClassChange = () => {
  if (searchForm.classId) {
    loadStatistics()
  }
}

const handleExport = async () => {
  if (!searchForm.classId) {
    ElMessage.warning('请先选择班级')
    return
  }
  
  exporting.value = true
  try {
    const res = await exportScores(searchForm.classId)
    const blob = new Blob([res], { type: 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet' })
    const url = window.URL.createObjectURL(blob)
    const link = document.createElement('a')
    link.href = url
    link.download = `${statistics.value.className}_成绩单.xlsx`
    link.click()
    window.URL.revokeObjectURL(url)
    ElMessage.success('导出成功')
  } catch (error) {
    ElMessage.error('导出失败')
  } finally {
    exporting.value = false
  }
}

const loadClasses = async () => {
  try {
    const res = await getAllClasses()
    classes.value = res.data || []
  } catch (error) {
    console.error(error)
  }
}

onMounted(() => {
  loadClasses()
  window.addEventListener('resize', () => {
    barChart?.resize()
    pieChart?.resize()
  })
})

onUnmounted(() => {
  barChart?.dispose()
  pieChart?.dispose()
})
</script>

<style scoped>
.stat-card {
  background: #fff;
  padding: 20px;
  border-radius: 8px;
  display: flex;
  align-items: center;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.1);
}

.stat-icon {
  width: 50px;
  height: 50px;
  border-radius: 8px;
  display: flex;
  align-items: center;
  justify-content: center;
  margin-right: 16px;
  
  .el-icon {
    font-size: 24px;
    color: #fff;
  }
}

.stat-info {
  .stat-label {
    font-size: 14px;
    color: #909399;
    margin-bottom: 4px;
  }
  
  .stat-value {
    font-size: 24px;
    font-weight: bold;
    color: #303133;
  }
}

.score-stat-card {
  background: #fff;
  padding: 20px;
  border-radius: 8px;
  text-align: center;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.1);
  
  .label {
    font-size: 14px;
    color: #909399;
    margin-bottom: 8px;
  }
  
  .value {
    font-size: 32px;
    font-weight: bold;
    
    &.primary { color: #409eff; }
    &.success { color: #67c23a; }
    &.warning { color: #e6a23c; }
    &.danger { color: #f56c6c; }
  }
}

.chart-container {
  background: #fff;
  padding: 20px;
  border-radius: 8px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.1);
}

.chart-title, .table-title {
  font-size: 16px;
  font-weight: 500;
  margin-bottom: 16px;
  color: #303133;
}

.table-container {
  background: #fff;
  padding: 20px;
  border-radius: 8px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.1);
}
</style>
