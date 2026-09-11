<template>
  <div class="page-container">
    <h2 class="page-title">系统设置</h2>
    
    <el-tabs v-model="activeTab">
      <!-- AI配置 -->
      <el-tab-pane label="AI配置" name="ai">
        <el-card>
          <el-form ref="aiFormRef" :model="aiForm" label-width="120px">
            <el-form-item label="API地址">
              <el-input v-model="aiForm['ai_api_url']" placeholder="请输入大模型API地址" />
              <div class="form-tip">示例: https://api.deepseek.com/v1/chat/completions</div>
            </el-form-item>
            <el-form-item label="API密钥">
              <el-input v-model="aiForm['ai_api_key']" type="password" show-password placeholder="请输入API密钥" />
            </el-form-item>
            <el-form-item label="模型名称">
              <el-input v-model="aiForm['ai_model']" placeholder="请输入模型名称" />
              <div class="form-tip">示例: deepseek-chat</div>
            </el-form-item>
            <el-form-item>
              <el-button type="primary" @click="handleSaveAi">保存配置</el-button>
            </el-form-item>
          </el-form>
        </el-card>
      </el-tab-pane>
      
      <!-- 评分权重配置 -->
      <el-tab-pane label="评分权重" name="weight">
        <el-card>
          <el-form ref="weightFormRef" :model="weightForm" label-width="120px">
            <el-form-item label="完整性权重">
              <el-input-number v-model="weightForm.completeness" :min="0" :max="100" />
              <span class="weight-suffix">%</span>
              <div class="form-tip">评估报告结构完整性的权重（建议30%）</div>
            </el-form-item>
            <el-form-item label="规范性权重">
              <el-input-number v-model="weightForm.specification" :min="0" :max="100" />
              <span class="weight-suffix">%</span>
              <div class="form-tip">评估格式规范性的权重（建议30%）</div>
            </el-form-item>
            <el-form-item label="知识点权重">
              <el-input-number v-model="weightForm.knowledge" :min="0" :max="100" />
              <span class="weight-suffix">%</span>
              <div class="form-tip">评估知识点覆盖的权重（建议40%）</div>
            </el-form-item>
            <el-form-item>
              <el-button type="primary" @click="handleSaveWeight">保存配置</el-button>
            </el-form-item>
          </el-form>
        </el-card>
      </el-tab-pane>
      
      <!-- 系统信息 -->
      <el-tab-pane label="系统信息" name="info">
        <el-card>
          <el-descriptions title="系统信息" :column="2" border>
            <el-descriptions-item label="系统名称">
              实训教学评价系统
            </el-descriptions-item>
            <el-descriptions-item label="版本号">
              v1.0.0
            </el-descriptions-item>
            <el-descriptions-item label="技术栈">
              Spring Boot + Vue3
            </el-descriptions-item>
            <el-descriptions-item label="AI模型">
              {{ aiForm['ai_model'] || '未配置' }}
            </el-descriptions-item>
          </el-descriptions>
          
          <el-divider />
          
          <h4>使用说明</h4>
          <el-alert type="info" :closable="false">
            <template #title>
              <div>
                <p><strong>1. 管理员功能：</strong>管理用户、班级，配置系统参数</p>
                <p><strong>2. 教师功能：</strong>查看学生报告，进行AI评价或手动评分</p>
                <p><strong>3. 学生功能：</strong>上传实训报告，查看评价结果</p>
                <p><strong>4. AI评价：</strong>系统调用大模型API自动分析报告内容，给出评分和改进建议</p>
              </div>
            </template>
          </el-alert>
        </el-card>
      </el-tab-pane>
    </el-tabs>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { getAllConfigs, batchUpdateConfigs } from '../api/config'

const activeTab = ref('ai')
const aiFormRef = ref()
const weightFormRef = ref()

const aiForm = reactive({
  'ai_api_url': '',
  'ai_api_key': '',
  'ai_model': ''
})

const weightForm = reactive({
  completeness: 30,
  specification: 30,
  knowledge: 40
})

const loadConfigs = async () => {
  try {
    const res = await getAllConfigs()
    const configs = res.data || {}
    
    // AI配置
    aiForm['ai_api_url'] = configs['ai_api_url'] || ''
    aiForm['ai_api_key'] = configs['ai_api_key'] || ''
    aiForm['ai_model'] = configs['ai_model'] || 'deepseek-chat'
    
    // 权重配置
    weightForm.completeness = parseInt(configs['completeness_weight']) || 30
    weightForm.specification = parseInt(configs['specification_weight']) || 30
    weightForm.knowledge = parseInt(configs['knowledge_weight']) || 40
  } catch (error) {
    console.error(error)
  }
}

const handleSaveAi = async () => {
  try {
    await batchUpdateConfigs({
      'ai_api_url': aiForm['ai_api_url'],
      'ai_api_key': aiForm['ai_api_key'],
      'ai_model': aiForm['ai_model']
    })
    ElMessage.success('保存成功')
  } catch (error) {
    console.error(error)
  }
}

const handleSaveWeight = async () => {
  const total = weightForm.completeness + weightForm.specification + weightForm.knowledge
  if (total !== 100) {
    ElMessage.warning('权重总和必须等于100%')
    return
  }
  
  try {
    await batchUpdateConfigs({
      'completeness_weight': weightForm.completeness.toString(),
      'specification_weight': weightForm.specification.toString(),
      'knowledge_weight': weightForm.knowledge.toString()
    })
    ElMessage.success('保存成功')
  } catch (error) {
    console.error(error)
  }
}

onMounted(() => {
  loadConfigs()
})
</script>

<style scoped>
.form-tip {
  font-size: 12px;
  color: #909399;
  margin-top: 4px;
}

.weight-suffix {
  margin-left: 8px;
  color: #606266;
}

h4 {
  margin: 16px 0;
  color: #303133;
}
</style>
