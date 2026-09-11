<template>
  <div class="dashboard">
    <h2 class="page-title">数据概览</h2>
    
    <!-- 统计卡片 -->
    <el-row :gutter="20" class="stat-row">
      <el-col :span="6">
        <div class="stat-card">
          <div class="stat-icon" style="background: #409eff">
            <el-icon><User /></el-icon>
          </div>
          <div class="stat-info">
            <p class="stat-label">用户总数</p>
            <p class="stat-value">{{ stats.totalUsers }}</p>
          </div>
        </div>
      </el-col>
      <el-col :span="6">
        <div class="stat-card">
          <div class="stat-icon" style="background: #67c23a">
            <el-icon><Document /></el-icon>
          </div>
          <div class="stat-info">
            <p class="stat-label">报告总数</p>
            <p class="stat-value">{{ stats.totalReports }}</p>
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
            <p class="stat-value">{{ stats.evaluatedReports }}</p>
          </div>
        </div>
      </el-col>
      <el-col :span="6">
        <div class="stat-card">
          <div class="stat-icon" style="background: #f56c6c">
            <el-icon><DataLine /></el-icon>
          </div>
          <div class="stat-info">
            <p class="stat-label">平均分</p>
            <p class="stat-value">{{ stats.avgScore }}</p>
          </div>
        </div>
      </el-col>
    </el-row>
    
    <!-- 图表 -->
    <el-row :gutter="20" style="margin-top: 20px">
      <el-col :span="12">
        <div class="chart-container">
          <div class="chart-title">班级平均分对比</div>
          <div ref="barChartRef" style="height: 300px"></div>
        </div>
      </el-col>
      <el-col :span="12">
        <div class="chart-container">
          <div class="chart-title">分数段分布</div>
          <div ref="pieChartRef" style="height: 300px"></div>
        </div>
      </el-col>
    </el-row>
    
    <!-- 最近评价 -->
    <el-row :gutter="20" style="margin-top: 20px">
      <el-col :span="24">
        <div class="table-container">
          <div class="table-title">最近评价记录</div>
          <el-table :data="recentEvaluations" stripe>
            <el-table-column prop="studentName" label="学生" />
            <el-table-column prop="className" label="班级" />
            <el-table-column prop="reportTitle" label="报告" show-overflow-tooltip />
            <el-table-column prop="totalScore" label="总分">
              <template #default="{ row }">
                <el-tag :type="getScoreType(row.totalScore)">{{ row.totalScore }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="evaluateTime" label="评价时间" />
          </el-table>
        </div>
      </el-col>
    </el-row>
  </div>
</template>

<script setup>
import { ref, onMounted, onUnmounted } from 'vue'
import * as echarts from 'echarts'
import { getAllClassesStatistics } from '../api/statistics'
import { getEvaluationList } from '../api/evaluation'
import { getAllClasses } from '../api/class'

const barChartRef = ref()
const pieChartRef = ref()
let barChart = null
let pieChart = null

const stats = ref({
  totalUsers: 0,
  totalReports: 0,
  evaluatedReports: 0,
  avgScore: 0
})

const recentEvaluations = ref([])

const initBarChart = (data) => {
  if (!barChartRef.value) return
  
  barChart = echarts.init(barChartRef.value)
  const option = {
    tooltip: { trigger: 'axis' },
    xAxis: {
      type: 'category',
      data: data.map(item => item.className)
    },
    yAxis: { type: 'value', name: '平均分', max: 100 },
    series: [{
      type: 'bar',
      data: data.map(item => item.avgScore),
      itemStyle: {
        color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
          { offset: 0, color: '#409eff' },
          { offset: 1, color: '#66b1ff' }
        ])
      },
      barWidth: '50%',
      label: { show: true, position: 'top' }
    }]
  }
  barChart.setOption(option)
}

const initPieChart = () => {
  if (!pieChartRef.value) return
  
  pieChart = echarts.init(pieChartRef.value)
  const option = {
    tooltip: { trigger: 'item' },
    legend: { bottom: '5%', left: 'center' },
    series: [{
      type: 'pie',
      radius: ['40%', '70%'],
      avoidLabelOverlap: false,
      label: { show: true, formatter: '{b}: {c}人' },
      data: [
        { value: 5, name: '0-60分', itemStyle: { color: '#f56c6c' } },
        { value: 10, name: '60-70分', itemStyle: { color: '#e6a23c' } },
        { value: 20, name: '70-80分', itemStyle: { color: '#409eff' } },
        { value: 15, name: '80-90分', itemStyle: { color: '#67c23a' } },
        { value: 10, name: '90-100分', itemStyle: { color: '#909399' } }
      ]
    }]
  }
  pieChart.setOption(option)
}

const getScoreType = (score) => {
  if (score >= 90) return 'success'
  if (score >= 80) return ''
  if (score >= 60) return 'warning'
  return 'danger'
}

const loadData = async () => {
  try {
    const [classRes, evalRes] = await Promise.all([
      getAllClassesStatistics(),
      getEvaluationList({ pageNum: 1, pageSize: 10 })
    ])
    
    if (classRes.data) {
      const data = classRes.data
      let totalReports = 0
      let totalScore = 0
      let totalEvaluated = 0
      
      data.forEach(item => {
        totalReports += item.totalReports || 0
        totalEvaluated += item.evaluatedReports || 0
        if (item.avgScore) {
          totalScore += parseFloat(item.avgScore) * (item.evaluatedReports || 0)
        }
      })
      
      stats.value = {
        totalUsers: 10,
        totalReports,
        evaluatedReports: totalEvaluated,
        avgScore: totalEvaluated > 0 ? (totalScore / totalEvaluated).toFixed(1) : 0
      }
      
      initBarChart(data)
    }
    
    if (evalRes.data?.records) {
      recentEvaluations.value = evalRes.data.records
    }
    
    initPieChart()
  } catch (error) {
    console.error(error)
  }
}

onMounted(() => {
  loadData()
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
.dashboard {
  padding: 20px;
}

.page-title {
  font-size: 22px;
  margin-bottom: 20px;
  color: #303133;
}

.stat-row {
  margin-bottom: 10px;
}

.stat-card {
  background: #fff;
  padding: 20px;
  border-radius: 8px;
  display: flex;
  align-items: center;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.1);
}

.stat-icon {
  width: 60px;
  height: 60px;
  border-radius: 8px;
  display: flex;
  align-items: center;
  justify-content: center;
  margin-right: 16px;
  
  .el-icon {
    font-size: 28px;
    color: #fff;
  }
}

.stat-info {
  .stat-label {
    font-size: 14px;
    color: #909399;
    margin-bottom: 8px;
  }
  
  .stat-value {
    font-size: 24px;
    font-weight: bold;
    color: #303133;
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
