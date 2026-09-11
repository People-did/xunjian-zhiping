<template>
  <div class="page-container" v-loading="parsing" element-loading-text="大模型正在深度解析教学大纲PDF并智能解构评分标准，请稍候...">
    <div class="page-header">
      <h2 class="page-title">{{ isEdit ? '编辑报告要求' : '发布报告要求' }}</h2>
      <div class="header-actions">
        <el-button @click="goBack">返回列表</el-button>
      </div>
    </div>
    
    <div class="form-container">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="120px">
        <el-form-item label="选择课程" prop="courseId">
          <el-select v-model="form.courseId" placeholder="请选择课程" style="width: 100%; max-width: 400px;">
            <el-option v-for="c in teacherCourses" :key="c.id" :label="c.courseName" :value="c.id" />
          </el-select>
        </el-form-item>
        
        <el-form-item label="报告标题" prop="title">
          <el-input 
            v-model="form.title" 
            placeholder="请输入报告要求标题" 
            style="width: 100%; max-width: 600px;"
            maxlength="200"
            show-word-limit
          />
        </el-form-item>
        
        <el-form-item label="截止时间" prop="deadline">
          <el-date-picker
            v-model="form.deadline"
            type="datetime"
            placeholder="选择截止时间"
            style="width: 100%; max-width: 300px;"
            format="YYYY-MM-DD HH:mm"
            value-format="YYYY-MM-DD HH:mm:ss"
          />
        </el-form-item>

        <el-form-item label="评分标准设定" v-if="!isEdit">
          <div style="width: 100%; max-width: 900px; border: 1px solid #e4e7ed; border-radius: 6px; padding: 16px; background: #fafafa;">
            <div style="display: flex; justify-content: space-between; align-items: center; margin-bottom: 12px;">
              <el-radio-group v-model="useCustomCriterion" size="small">
                <el-radio-button :label="false">使用默认三大指标(3:3:4)</el-radio-button>
                <el-radio-button :label="true">启用大模型自定义评分标准</el-radio-button>
              </el-radio-group>

              <el-upload
                v-if="useCustomCriterion"
                action="#"
                :auto-upload="false"
                :show-file-list="false"
                :on-change="handlePdfUpload"
                accept=".pdf,.doc,.docx"
              >
                <el-button type="success" size="small" icon="Cpu">上传大纲PDF一键智能生成标准</el-button>
              </el-upload>
            </div>

            <div v-if="useCustomCriterion" style="margin-top: 12px;">
              <el-table :data="form.criteria" size="small" border stripe style="background: #fff;">
                <el-table-column label="考核指标名称" width="180">
                  <template #default="{ row }">
                    <el-input v-model="row.name" placeholder="如:代码健壮性" size="small" />
                  </template>
                </el-table-column>
                <el-table-column label="权重占比 (%)" width="110" align="center">
                  <template #default="{ row }">
                    <el-input-number v-model="row.weight" :min="1" :max="100" :controls="false" style="width: 80px;" size="small" @change="syncMaxScore(row)" />
                  </template>
                </el-table-column>
                <el-table-column label="打分考核细则描述 (供大模型审阅核心要点)">
                  <template #default="{ row }">
                    <el-input v-model="row.description" type="textarea" :rows="1" placeholder="请输入具体打分细则或大模型裁量提示词..." size="small" />
                  </template>
                </el-table-column>
                <el-table-column label="操作" width="80" align="center">
                  <template #default="{ $index }">
                    <el-button type="danger" icon="Delete" circle size="small" @click="removeCriterion($index)" />
                  </template>
                </el-table-column>
              </el-table>
              
              <div style="display: flex; justify-content: space-between; align-items: center; margin-top: 10px; padding: 0 4px;">
                <el-button type="primary" icon="Plus" size="small" plain @click="addCriterion">手动新增一行指标</el-button>
                <div style="font-size: 13px; color: #606266;">
                  当前权重总计：
                  <span :style="{ color: totalWeight === 100 ? '#67c23a' : '#f56c6c', fontWeight: 'bold' }">
                    {{ totalWeight }}%
                  </span>
                  <span v-if="totalWeight !== 100" style="color: #f56c6c; margin-left: 6px;">(必须等于100%)</span>
                </div>
              </div>
            </div>
            <div v-else style="font-size: 13px; color: #909399; padding: 4px 0;">
              系统默认评分标准：完整性得分 (30分) + 规范性得分 (30分) + 知识点覆盖得分 (40分)，综合大总分100分。
            </div>
          </div>
        </el-form-item>
        
        <el-form-item label="报告正文要求" prop="content">
          <el-input
            v-model="form.content"
            type="textarea"
            :rows="10"
            placeholder="请输入报告要求的具体描述大纲内容..."
            style="width: 100%; max-width: 900px;"
          />
        </el-form-item>
        
        <el-form-item>
          <el-button type="primary" @click="handleSubmit" :loading="submitting" size="large">
            {{ isEdit ? '保存修改' : '发布报告要求' }}
          </el-button>
          <el-button @click="goBack" size="large">取消</el-button>
        </el-form-item>
      </el-form>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getMyCourses } from '../api/course'
import { 
  getReportRequirement, 
  createReportRequirement, 
  updateReportRequirement,
  parseCriterionPdf // 导入刚刚接通的PDF提取血管API
} from '../api/reportRequirement'

const route = useRoute()
const router = useRouter()
const loading = ref(false)
const submitting = ref(false)
const parsing = ref(false) // 大模型识别文件流的异步等待开关
const useCustomCriterion = ref(false) // 切换开关
const formRef = ref()

const form = reactive({
  id: null,
  courseId: null,
  title: '',
  deadline: null,
  content: '',
  criteria: [] // 预留给后端CustomRequirementDTO级联接收标准的口袋
})

const teacherCourses = ref([])
const isEdit = computed(() => !!route.query.id)

// 计算当前多单项权重的累计数值
const totalWeight = computed(() => {
  return form.criteria.reduce((sum, item) => sum + (parseInt(item.weight) || 0), 0)
})

const rules = {
  courseId: [{ required: true, message: '请选择课程', trigger: 'change' }],
  title: [{ required: true, message: '请输入报告标题', trigger: 'blur' }],
  content: [{ required: true, message: '请输入报告内容', trigger: 'blur' }]
}

const syncMaxScore = (row) => {
  row.maxScore = row.weight // 指标上限分数强对齐它的权重值
}

const addCriterion = () => {
  form.criteria.push({ name: '', weight: 10, maxScore: 10, description: '' })
}

const removeCriterion = (index) => {
  form.criteria.splice(index, 1)
}

// 【硬核创新：一键上传大纲PDF触发AI智能提取标准】
const handlePdfUpload = async (uploadFile) => {
  const rawFile = uploadFile.raw
  if (!rawFile) return
  
  parsing.value = true
  try {
    const res = await parseCriterionPdf(rawFile)
    if (res.code === 200 && res.data) {
      form.criteria = res.data.map(item => ({
        name: item.name || '',
        weight: item.weight || 0,
        maxScore: item.maxScore || item.weight,
        description: item.description || ''
      }))
      ElMessage.success('大模型已经成功从业界/大纲文档中精准提取出实训指标考核矩阵！')
    } else {
      ElMessage.error(res.message || '大模型未能成功解析此文件，请确保文件内容完整。')
    }
  } catch (error) {
    console.error(error)
    ElMessage.error('调用国产大模型解析引擎超时，请稍后重试。')
  } finally {
    parsing.value = false
  }
}

const loadTeacherCourses = async () => {
  try {
    const res = await getMyCourses()
    teacherCourses.value = res.data || []
  } catch (error) {
    console.error(error)
  }
}

const loadData = async () => {
  if (!route.query.id) return
  loading.value = true
  try {
    const res = await getReportRequirement(route.query.id)
    const data = res.data
    if (data) {
      form.id = data.id
      form.courseId = data.courseId
      form.title = data.title
      form.deadline = data.deadline
      form.content = data.content || ''
    }
  } catch (error) {
    console.error(error)
  } finally {
    loading.value = false
  }
}

const handleSubmit = async () => {
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return

  // 核心校验：如果老师启用了自定义评分指标，必须卡住累计权重不能溢出或残缺
  if (!isEdit.value && useCustomCriterion.value) {
    if (form.criteria.length === 0) {
      ElMessage.warning('请至少上传大纲生成或手动指定一条实训评分指标！')
      return
    }
    if (totalWeight.value !== 100) {
      ElMessage.warning(`当前定制的指标累计权重为 ${totalWeight.value}%，必须恰好调整等于 100% 才能执行发布！`)
      return
    }
  }

  submitting.value = true
  try {
    // 打包递交对象数据
    const submitData = { ...form }
    // 如果没有勾选自定义，清空 criteria 数据包，让后端宿主走传统三大核心维度
    if (!useCustomCriterion.value) {
      submitData.criteria = []
    }

    if (isEdit.value) {
      await updateReportRequirement(submitData)
      ElMessage.success('保存成功')
    } else {
      await createReportRequirement(submitData)
      ElMessage.success('发布作业成功，已级联启动自定义AI打分引擎并同步通知全体学生！')
    }
    goBack()
  } catch (error) {
    console.error(error)
  } finally {
    submitting.value = false
  }
}

const goBack = () => {
  router.push('/report-requirement')
}

onMounted(() => {
  loadTeacherCourses().then(() => {
    if (isEdit.value) {
      loadData()
    }
  })
})
</script>

<style scoped>
.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 24px;
  padding-bottom: 16px;
  border-bottom: 1px solid #e6e6e6;
}
.form-container {
  background: #fff;
  padding: 24px;
  border-radius: 8px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.1);
}
</style>