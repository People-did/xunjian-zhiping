<template>
  <div class="page-container">
    <h2 class="page-title">班级管理</h2>
    
    <!-- 搜索 -->
    <div class="search-form">
      <el-form :inline="true" :model="searchForm">
        <el-form-item label="班级名称">
          <el-input v-model="searchForm.keyword" placeholder="请输入班级名称" clearable />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleSearch">查询</el-button>
          <el-button @click="handleReset">重置</el-button>
        </el-form-item>
      </el-form>
    </div>
    
    <!-- 表格 -->
    <div class="table-container">
      <div class="toolbar">
        <el-button type="primary" @click="handleAdd">新增班级</el-button>
      </div>
      
      <el-table :data="tableData" stripe v-loading="loading">
        <el-table-column prop="id" label="ID" width="80" />
        <el-table-column prop="className" label="班级名称" />
        <el-table-column label="班主任" prop="description" />
        <el-table-column prop="description" label="班级描述" show-overflow-tooltip />
        <el-table-column prop="createTime" label="创建时间" />
        <el-table-column label="操作" width="180" align="center" fixed="right">
          <template #default="{ row }">
            <div style="display: flex; align-items: center; justify-content: center; gap: 10px;">
              <el-button 
                type="primary" 
                size="small"
                plain
                icon="Edit" 
                @click="handleEdit(row)"
              >编辑</el-button>
              
              <el-button 
                type="danger" 
                size="small"
                plain
                icon="Delete" 
                @click="handleDelete(row)"
              >删除</el-button>
            </div>
          </template>
        </el-table-column>
      </el-table>
      
      <div class="pagination-container">
        <el-pagination
          v-model:current-page="pagination.pageNum"
          v-model:page-size="pagination.pageSize"
          :total="pagination.total"
          :page-sizes="[10, 20, 50]"
          layout="total, sizes, prev, pager, next"
          @size-change="loadData"
          @current-change="loadData"
        />
      </div>
    </div>
    
    <!-- 编辑对话框 -->
    <el-dialog v-model="dialogVisible" :title="dialogTitle" width="500px">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="100px">
        <el-form-item label="班级名称" prop="className">
          <el-input v-model="form.className" placeholder="请输入班级名称" />
        </el-form-item>
        <el-form-item label="班主任" prop="teacherId">
          <el-select v-model="form.teacherId" placeholder="请选择班主任" clearable>
            <el-option v-for="t in teachers" :key="t.id" :label="t.realName" :value="t.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="班级描述">
          <el-input v-model="form.description" type="textarea" :rows="3" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSubmit">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getClassList, addClass, updateClass, deleteClass } from '../api/class'
import { getTeachers } from '../api/user'

const loading = ref(false)
const dialogVisible = ref(false)
const dialogTitle = ref('新增班级')
const formRef = ref()
const teachers = ref([])

const searchForm = reactive({ keyword: '' })
const pagination = reactive({ pageNum: 1, pageSize: 10, total: 0 })
const tableData = ref([])

const form = reactive({
  id: null,
  className: '',
  teacherId: null,
  description: ''
})

const rules = {
  className: [{ required: true, message: '请输入班级名称', trigger: 'blur' }]
}

const loadData = async () => {
  loading.value = true
  try {
    const res = await getClassList({ ...searchForm, ...pagination })
    tableData.value = res.data.records
    pagination.total = res.data.total
  } catch (error) {
    console.error(error)
  } finally {
    loading.value = false
  }
}

const loadTeachers = async () => {
  try {
    const res = await getTeachers()
    teachers.value = res.data || []
  } catch (error) {
    console.error(error)
  }
}

const handleSearch = () => {
  pagination.pageNum = 1
  loadData()
}

const handleReset = () => {
  searchForm.keyword = ''
  handleSearch()
}

const handleAdd = () => {
  dialogTitle.value = '新增班级'
  Object.assign(form, { id: null, className: '', teacherId: null, description: '' })
  dialogVisible.value = true
}

const handleEdit = (row) => {
  dialogTitle.value = '编辑班级'
  Object.assign(form, {
    id: row.id,
    className: row.className,
    teacherId: row.teacherId,
    description: row.description
  })
  dialogVisible.value = true
}

const handleSubmit = async () => {
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return
  
  try {
    if (form.id) {
      await updateClass(form)
      ElMessage.success('更新成功')
    } else {
      await addClass(form)
      ElMessage.success('添加成功')
    }
    dialogVisible.value = false
    loadData()
  } catch (error) {
    console.error(error)
  }
}

const handleDelete = async (row) => {
  await ElMessageBox.confirm('确定删除该班级？', '提示', { type: 'warning' })
  try {
    await deleteClass(row.id)
    ElMessage.success('删除成功')
    loadData()
  } catch (error) {
    ElMessage.error(error.message || '删除失败')
  }
}

onMounted(() => {
  loadData()
  loadTeachers()
})
</script>
