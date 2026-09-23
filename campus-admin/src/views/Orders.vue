<template>
  <div>
    <div class="section-title">订单管理</div>

    <div class="toolbar">
      <div class="tabs">
        <!-- 展示订单所有的状态、可以进行筛选 -->
        <el-tag
          v-for="item in tabs"
          :key="item.value"
          :type="statusFilter === item.value ? 'primary' : 'info'"
          :effect="statusFilter === item.value ? 'dark' : 'plain'"
          class="tab"
          @click="
            statusFilter = item.value;
            currentPage = 1;
          "
        >
          {{ item.label }}
        </el-tag>
      </div>

      <div class="toolbar-right">
        <!-- 新增和搜索输入框 -->
        <el-button type="primary" @click="openCreate">新增订单</el-button>
        <el-input
          v-model="keyword"
          class="search-input"
          placeholder="搜索订单号/类型/发布者"
          clearable
          @input="currentPage = 1"
        />
      </div>
    </div>

    <!-- 卡片 -->
    <el-card>
      <div class="table-header">
        <span>订单列表，共 {{ filteredOrders.length }} 条</span>
        <el-button @click="loadOrders">刷新</el-button>
      </div>
      <!-- 表格组件展示订单列表 -->
      <el-table :data="pagedOrders" v-loading="loading" border>
        <el-table-column prop="orderNo" label="订单号" width="170" />
        <el-table-column prop="taskType" label="类型" width="110" />
        <el-table-column prop="reward" label="报酬" width="100">
          <template #default="{ row }">¥{{ row.reward }}</template>
        </el-table-column>
        <el-table-column prop="publisherName" label="发布者" width="120" />
        <el-table-column prop="runnerName" label="跑腿员" width="120">
          <template #default="{ row }">{{ row.runnerName || "未接单" }}</template>
        </el-table-column>
        <el-table-column
          prop="pickupLocation"
          label="起点"
          min-width="160"
          show-overflow-tooltip
        />
        <el-table-column
          prop="deliveryLocation"
          label="终点"
          min-width="160"
          show-overflow-tooltip
        />
        <el-table-column prop="status" label="状态" width="110">
          <template #default="{ row }">
            <el-tag :type="statusTagMap[row.status]">{{
              statusTextMap[row.status]
            }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createdAt" label="创建时间" width="170">
          <template #default="{ row }">{{ formatDateTime(row.createdAt) }}</template>
        </el-table-column>
        <!-- 操作按钮 -->
        <el-table-column label="操作" width="240" fixed="right">
          <template #default="{ row }">
            <el-button size="small" @click="openDetail(row)">详情</el-button>
            <el-button
              size="small"
              type="primary"
              :disabled="row.status > 1"
              @click="openEdit(row)"
              >编辑</el-button
            >
            <el-button
              size="small"
              type="warning"
              :disabled="row.status === 4 || row.status === 5"
              @click="cancelOrder(row)"
              >取消</el-button
            >
            <el-button
              size="small"
              type="danger"
              :disabled="row.status >= 2 && row.status <= 4"
              @click="removeOrder(row)"
              >删除</el-button
            >
          </template>
        </el-table-column>
      </el-table>
      <!-- 分页组件 -->
      <div class="pagination-wrap">
        <el-pagination
          v-model:current-page="currentPage"
          :page-size="pageSize"
          :total="filteredOrders.length"
          layout="prev, pager, next, jumper, ->, total"
        />
      </div>
    </el-card>

    <!--  对话框-订单详情  -->
    <el-dialog v-model="detailVisible" title="订单详情" width="700px">
      <div v-if="selected" class="detail-grid">
        <div><span>订单号：</span>{{ selected.orderNo }}</div>
        <div><span>类型：</span>{{ selected.taskType }}</div>
        <div><span>报酬：</span>¥{{ selected.reward }}</div>
        <div><span>状态：</span>{{ statusTextMap[selected.status] }}</div>
        <div><span>发布者：</span>{{ selected.publisherName || "-" }}</div>
        <div><span>跑腿员：</span>{{ selected.runnerName || "未接单" }}</div>
        <div class="full"><span>取货地点：</span>{{ selected.pickupLocation }}</div>
        <div class="full"><span>送达地点：</span>{{ selected.deliveryLocation }}</div>
        <div class="full"><span>任务描述：</span>{{ selected.description || "-" }}</div>
      </div>
      <template #footer>
        <el-button @click="detailVisible = false">关闭</el-button>
      </template>
    </el-dialog>

    <!--  对话框-订单新增修改  -->
    <el-dialog
      v-model="formVisible"
      :title="isEdit ? '编辑订单' : '新增订单'"
      width="650px"
    >
      <!-- 表单 -->
      <el-form ref="formRef" :model="form" :rules="rules" label-width="110px">
        <el-form-item label="订单类型" prop="taskType">
          <el-select v-model="form.taskType" style="width: 100%">
            <el-option
              v-for="item in taskTypes"
              :key="item"
              :label="item"
              :value="item"
            />
          </el-select>
        </el-form-item>

        <el-form-item label="悬赏金额" prop="reward">
          <el-input-number
            v-model="form.reward"
            :min="1"
            :precision="2"
            style="width: 100%"
          />
        </el-form-item>

        <el-form-item label="取货地点" prop="pickupLocation">
          <el-input v-model="form.pickupLocation" placeholder="请输入取货地点" />
        </el-form-item>

        <el-form-item label="送达地点" prop="deliveryLocation">
          <el-input v-model="form.deliveryLocation" placeholder="请输入送达地点" />
        </el-form-item>

        <el-form-item label="预期时间" prop="expectedTime">
          <el-date-picker
            v-model="form.expectedTime"
            type="datetime"
            value-format="YYYY-MM-DDTHH:mm:ss"
            placeholder="请选择预期完成时间"
            style="width: 100%"
          />
        </el-form-item>

        <el-form-item label="任务描述">
          <el-input v-model="form.description" type="textarea" :rows="3" />
        </el-form-item>
      </el-form>

      <template #footer>
        <el-button @click="formVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="submitOrder"
          >保存</el-button
        >
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
// 导入组件
import { computed, onMounted, ref } from "vue";
import { ElMessage, ElMessageBox } from "element-plus";
// 请求处理函数
import {
  fetchOrders,
  createOrder,
  updateOrder,
  forceCancelOrder,
  deleteOrder,
} from "@/api/order";
//定义对应绑定的数据变量
const loading = ref(false);
const submitting = ref(false);
const orders = ref([]);
const selected = ref(null);
const detailVisible = ref(false);
const formVisible = ref(false);
const isEdit = ref(false);
const formRef = ref();
const keyword = ref("");
const statusFilter = ref(null);
const currentPage = ref(1);
const pageSize = 10;
//任务类型
const taskTypes = ["代取快递", "代购", "代办", "送物", "其他"];
//订单状态
const tabs = [
  { value: null, label: "全部" },
  { value: 0, label: "待支付" },
  { value: 1, label: "待接单" },
  { value: 2, label: "进行中" },
  { value: 3, label: "待确认" },
  { value: 4, label: "已完成" },
  { value: 5, label: "已取消" },
  { value: 6, label: "申诉中" },
];

const statusTextMap = {
  0: "待支付",
  1: "待接单",
  2: "进行中",
  3: "待确认",
  4: "已完成",
  5: "已取消",
  6: "申诉中",
};

const statusTagMap = {
  0: "info",
  1: "warning",
  2: "primary",
  3: "primary",
  4: "success",
  5: "danger",
  6: "danger",
};

const form = ref(createDefaultForm());

//提供校验规则
const rules = {
  taskType: [{ required: true, message: "请选择订单类型", trigger: "change" }],
  reward: [{ required: true, message: "请输入悬赏金额", trigger: "blur" }],
  pickupLocation: [{ required: true, message: "请输入取货地点", trigger: "blur" }],
  deliveryLocation: [{ required: true, message: "请输入送达地点", trigger: "blur" }],
  expectedTime: [{ required: true, message: "请选择预期完成时间", trigger: "change" }],
};

//根据订单状态进行筛选
const filteredOrders = computed(() => {
  const kw = keyword.value.trim().toLowerCase();
  //使用filter进行条件过滤
  return orders.value.filter((item) => {
    const matchStatus = statusFilter.value === null || item.status === statusFilter.value;
    const matchKeyword =
      !kw ||
      String(item.orderNo || "")
        .toLowerCase()
        .includes(kw) ||
      String(item.taskType || "")
        .toLowerCase()
        .includes(kw) ||
      String(item.publisherName || "")
        .toLowerCase()
        .includes(kw);
    return matchStatus && matchKeyword;
  });
});

//分页查询
const pagedOrders = computed(() => {
  const start = (currentPage.value - 1) * pageSize;
  return filteredOrders.value.slice(start, start + pageSize);
});

//页面加载后的查询调用
onMounted(loadOrders);

//订单查询请求处理
async function loadOrders() {
  loading.value = true;
  try {
    orders.value = (await fetchOrders()) || [];
  } finally {
    loading.value = false;
  }
}

function createDefaultForm() {
  return {
    id: null,
    taskType: "代取快递",
    reward: 8,
    pickupLocation: "",
    pickupLatitude: "",
    pickupLongitude: "",
    deliveryLocation: "",
    deliveryLatitude: "",
    deliveryLongitude: "",
    expectedTime: "",
    description: "",
    deliveryDistance: 0,
  };
}

function openDetail(row) {
  selected.value = row;
  detailVisible.value = true;
}

function openCreate() {
  isEdit.value = false;
  form.value = createDefaultForm();
  formVisible.value = true;
}

//打开编辑
function openEdit(row) {
  isEdit.value = true;
  form.value = {
    id: row.id,
    taskType: row.taskType,
    reward: row.reward,
    pickupLocation: row.pickupLocation,
    pickupLatitude: row.pickupLatitude || "",
    pickupLongitude: row.pickupLongitude || "",
    deliveryLocation: row.deliveryLocation,
    deliveryLatitude: row.deliveryLatitude || "",
    deliveryLongitude: row.deliveryLongitude || "",
    expectedTime: row.expectedTime || "",
    description: row.description || "",
    deliveryDistance: row.deliveryDistance || 0,
  };
  formVisible.value = true;
}

//新增-修改提交的函数
async function submitOrder() {
  await formRef.value.validate();
  submitting.value = true;
  try {
    const data = { ...form.value };
    //判读是新增还是修改
    if (isEdit.value) {
      await updateOrder(data.id, data);
      ElMessage.success("订单更新成功");
    } else {
      await createOrder(data);
      ElMessage.success("订单创建成功");
    }
    formVisible.value = false;
    await loadOrders();
  } finally {
    submitting.value = false;
  }
}

//取消订单
async function cancelOrder(row) {
  await ElMessageBox.confirm("确认强制取消该订单吗？", "提示", { type: "warning" });
  await forceCancelOrder(row.id);
  ElMessage.success("订单已取消");
  await loadOrders();
}

//删除订单
async function removeOrder(row) {
  await ElMessageBox.confirm("确认删除该订单吗？删除后列表中将不再显示。", "提示", {
    type: "warning",
  });
  await deleteOrder(row.id);
  ElMessage.success("订单已删除");
  await loadOrders();
}

//时间格式化处理函数
function formatDateTime(value) {
  if (!value) return "-";
  const date = new Date(value);
  const year = date.getFullYear();
  const month = String(date.getMonth() + 1).padStart(2, "0");
  const day = String(date.getDate()).padStart(2, "0");
  const hour = String(date.getHours()).padStart(2, "0");
  const minute = String(date.getMinutes()).padStart(2, "0");
  return `${year}-${month}-${day} ${hour}:${minute}`;
}
</script>

<style scoped>
.section-title {
  font-size: 20px;
  font-weight: 700;
  margin-bottom: 20px;
}

.toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 16px;
  flex-wrap: wrap;
}

.tabs {
  display: flex;
  gap: 6px;
  flex-wrap: wrap;
}

.tab {
  cursor: pointer;
}

.toolbar-right {
  display: flex;
  align-items: center;
  gap: 12px;
}

.search-input {
  width: 260px;
}

.table-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 12px;
}

.pagination-wrap {
  margin-top: 16px;
  display: flex;
  justify-content: flex-end;
}

.detail-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 12px;
}

.detail-grid .full {
  grid-column: 1 / -1;
}

.detail-grid span {
  color: #64748b;
}
</style>
