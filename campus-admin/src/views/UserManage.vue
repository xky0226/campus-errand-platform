<template>
  <div>
    <div class="section-title">用户管理</div>

    <div class="toolbar">
      <div class="tabs">
        <!-- 按角色筛选 -->
        <el-tag
          v-for="item in roleTabs"
          :key="String(item.value)"
          :type="roleFilter === item.value ? 'primary' : 'info'"
          :effect="roleFilter === item.value ? 'dark' : 'plain'"
          class="tab"
          @click="
            roleFilter = item.value;
            currentPage = 1;
          "
        >
          {{ item.label }}
        </el-tag>
      </div>

      <div class="toolbar-right">
        <el-input
          v-model="keyword"
          class="search-input"
          placeholder="搜索昵称/姓名/学号/手机号"
          clearable
          @input="currentPage = 1"
        />
      </div>
    </div>

    <el-card>
      <div class="table-header">
        <span>用户列表，共 {{ filteredUsers.length }} 条</span>
        <el-button @click="loadUsers">刷新</el-button>
      </div>
      <!-- 表格组件展示用户列表 -->
      <el-table :data="pagedUsers" v-loading="loading" border>
        <el-table-column prop="id" label="ID" width="70" />
        <el-table-column
          prop="nickname"
          label="昵称"
          min-width="130"
          show-overflow-tooltip
        />
        <el-table-column
          prop="realName"
          label="真实姓名"
          width="110"
          show-overflow-tooltip
        />
        <el-table-column prop="studentId" label="学号" width="130">
          <template #default="{ row }">{{ row.studentId || "-" }}</template>
        </el-table-column>
        <el-table-column prop="phone" label="手机号" width="130">
          <template #default="{ row }">{{ row.phone || "-" }}</template>
        </el-table-column>
        <el-table-column prop="role" label="角色" width="100">
          <template #default="{ row }">
            <el-tag :type="roleTagMap[row.role]">{{
              roleTextMap[row.role] || "-"
            }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="creditScore" label="信用分" width="90" />
        <el-table-column prop="verifiedStatus" label="学生认证" width="110">
          <template #default="{ row }">
            <el-tag :type="verifyTagMap[row.verifiedStatus]" effect="plain">{{
              verifyTextMap[row.verifiedStatus]
            }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="status" label="状态" width="90">
          <template #default="{ row }">
            <el-tag :type="statusTagMap[row.status]">{{
              statusTextMap[row.status]
            }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createdAt" label="注册时间" width="170">
          <template #default="{ row }">{{ formatDateTime(row.createdAt) }}</template>
        </el-table-column>
        <!-- 操作按钮 -->
        <el-table-column label="操作" width="230" fixed="right">
          <template #default="{ row }">
            <el-button size="small" @click="openDetail(row)">详情</el-button>
            <el-button size="small" type="primary" @click="openEdit(row)"
              >编辑</el-button
            >
            <el-button
              size="small"
              :type="row.status === 1 ? 'danger' : 'success'"
              :disabled="row.role === 2"
              @click="toggleStatus(row)"
              >{{ row.status === 1 ? "禁用" : "启用" }}</el-button
            >
          </template>
        </el-table-column>
      </el-table>
      <!-- 分页组件 -->
      <div class="pagination-wrap">
        <el-pagination
          v-model:current-page="currentPage"
          :page-size="pageSize"
          :total="filteredUsers.length"
          layout="prev, pager, next, jumper, ->, total"
        />
      </div>
    </el-card>

    <!--  对话框-用户详情  -->
    <el-dialog v-model="detailVisible" title="用户详情" width="700px">
      <div v-if="selected" class="detail-grid">
        <div><span>ID：</span>{{ selected.id }}</div>
        <div><span>昵称：</span>{{ selected.nickname }}</div>
        <div><span>真实姓名：</span>{{ selected.realName || "-" }}</div>
        <div><span>学号：</span>{{ selected.studentId || "-" }}</div>
        <div><span>手机号：</span>{{ selected.phone || "-" }}</div>
        <div><span>角色：</span>{{ roleTextMap[selected.role] || "-" }}</div>
        <div><span>信用分：</span>{{ selected.creditScore }}</div>
        <div>
          <span>学生认证：</span>{{ verifyTextMap[selected.verifiedStatus] }}
        </div>
        <div><span>账号状态：</span>{{ statusTextMap[selected.status] }}</div>
        <div><span>接单率：</span>{{ selected.acceptanceRate }}</div>
        <div><span>完成订单：</span>{{ selected.completedOrders }}</div>
        <div><span>钱包余额：</span>¥{{ selected.walletBalance }}</div>
        <div><span>冻结余额：</span>¥{{ selected.frozenBalance }}</div>
        <div><span>累计收益：</span>¥{{ selected.totalEarnings }}</div>
        <div class="full"><span>支付宝账号：</span>{{ selected.alipayAccount || "-" }}</div>
        <div class="full">
          <span>注册时间：</span>{{ formatDateTime(selected.createdAt) }}
        </div>
        <div class="full">
          <span>更新时间：</span>{{ formatDateTime(selected.updatedAt) }}
        </div>
      </div>
      <template #footer>
        <el-button @click="detailVisible = false">关闭</el-button>
      </template>
    </el-dialog>

    <!--  对话框-用户编辑  -->
    <el-dialog v-model="formVisible" title="编辑用户" width="600px">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="110px">
        <el-form-item label="昵称" prop="nickname">
          <el-input v-model="form.nickname" maxlength="64" show-word-limit />
        </el-form-item>

        <el-form-item label="真实姓名" prop="realName">
          <el-input v-model="form.realName" maxlength="32" show-word-limit />
        </el-form-item>

        <el-form-item label="手机号" prop="phone">
          <el-input v-model="form.phone" maxlength="16" />
        </el-form-item>

        <el-form-item label="角色" prop="role">
          <!-- 管理员账号的角色不给改，后端也会拒 -->
          <el-select
            v-model="form.role"
            :disabled="editingIsAdmin"
            style="width: 100%"
          >
            <el-option label="需求者" :value="0" />
            <el-option label="跑腿员" :value="1" />
          </el-select>
          <div v-if="editingIsAdmin" class="form-tip">
            管理员账号的角色不可修改
          </div>
        </el-form-item>

        <el-form-item label="信用分" prop="creditScore">
          <el-input-number
            v-model="form.creditScore"
            :min="0"
            :precision="0"
            style="width: 100%"
          />
        </el-form-item>
      </el-form>

      <template #footer>
        <el-button @click="formVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="submitUser"
          >保存</el-button
        >
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from "vue";
import { ElMessage, ElMessageBox } from "element-plus";
import { fetchUsers, updateUser } from "@/api/user";

const loading = ref(false);
const submitting = ref(false);
const users = ref([]);
const selected = ref(null);
const detailVisible = ref(false);
const formVisible = ref(false);
const editingIsAdmin = ref(false);
const formRef = ref();
const keyword = ref("");
const roleFilter = ref(null);
const currentPage = ref(1);
const pageSize = 10;

//角色：0 需求者、1 跑腿员、2 管理员（数值含义来自 AdminController 的注释）
const roleTabs = [
  { value: null, label: "全部" },
  { value: 0, label: "需求者" },
  { value: 1, label: "跑腿员" },
  { value: 2, label: "管理员" },
];

const roleTextMap = {
  0: "需求者",
  1: "跑腿员",
  2: "管理员",
};

const roleTagMap = {
  0: "info",
  1: "primary",
  2: "danger",
};

//认证状态：DDL 里 verified_status 没有 COMMENT，0/1/2/3 都有数据。
//按管理端「待审核」= 2 这一点反推，3 含义不明，兜底显示「状态3」而不是瞎编一个词。
const verifyTextMap = {
  0: "未认证",
  1: "已认证",
  2: "审核中",
  3: "状态3",
};

const verifyTagMap = {
  0: "info",
  1: "success",
  2: "warning",
  3: "info",
};

//账号状态：1 正常、0 已禁用（/auth/login 里 status != 1 会被拒绝登录）
const statusTextMap = {
  0: "已禁用",
  1: "正常",
};

const statusTagMap = {
  0: "danger",
  1: "success",
};

const form = ref(createDefaultForm());

//提供校验规则
const rules = {
  nickname: [{ required: true, message: "请输入昵称", trigger: "blur" }],
  creditScore: [{ required: true, message: "请输入信用分", trigger: "blur" }],
};

//根据角色和关键字筛选
const filteredUsers = computed(() => {
  const kw = keyword.value.trim().toLowerCase();
  return users.value.filter((item) => {
    const matchRole = roleFilter.value === null || item.role === roleFilter.value;
    const matchKeyword =
      !kw ||
      String(item.nickname || "")
        .toLowerCase()
        .includes(kw) ||
      String(item.realName || "")
        .toLowerCase()
        .includes(kw) ||
      String(item.studentId || "")
        .toLowerCase()
        .includes(kw) ||
      String(item.phone || "")
        .toLowerCase()
        .includes(kw);
    return matchRole && matchKeyword;
  });
});

//分页
const pagedUsers = computed(() => {
  const start = (currentPage.value - 1) * pageSize;
  return filteredUsers.value.slice(start, start + pageSize);
});

//页面加载后的查询调用
onMounted(loadUsers);

async function loadUsers() {
  loading.value = true;
  try {
    users.value = (await fetchUsers()) || [];
  } finally {
    loading.value = false;
  }
}

function createDefaultForm() {
  return {
    id: null,
    nickname: "",
    realName: "",
    phone: "",
    role: 0,
    creditScore: 100,
  };
}

function openDetail(row) {
  selected.value = row;
  detailVisible.value = true;
}

//打开编辑
function openEdit(row) {
  editingIsAdmin.value = row.role === 2;
  form.value = {
    id: row.id,
    nickname: row.nickname || "",
    realName: row.realName || "",
    phone: row.phone || "",
    role: row.role,
    creditScore: row.creditScore ?? 100,
  };
  formVisible.value = true;
}

//保存编辑
async function submitUser() {
  await formRef.value.validate();
  submitting.value = true;
  try {
    const data = {
      nickname: form.value.nickname,
      realName: form.value.realName,
      phone: form.value.phone,
      creditScore: form.value.creditScore,
    };
    //管理员账号不带 role，后端本来也只接受 0/1
    if (!editingIsAdmin.value) {
      data.role = form.value.role;
    }
    await updateUser(form.value.id, data);
    ElMessage.success("用户信息已更新");
    formVisible.value = false;
    await loadUsers();
  } finally {
    submitting.value = false;
  }
}

//禁用 / 启用
async function toggleStatus(row) {
  const nextStatus = row.status === 1 ? 0 : 1;
  const action = nextStatus === 0 ? "禁用" : "启用";
  try {
    await ElMessageBox.confirm(`确认${action}用户「${row.nickname}」吗？`, "提示", {
      type: "warning",
    });
  } catch {
    //点了取消
    return;
  }
  await updateUser(row.id, { status: nextStatus });
  ElMessage.success(`已${action}`);
  await loadUsers();
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
  width: 280px;
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

.form-tip {
  color: #94a3b8;
  font-size: 12px;
  line-height: 1.6;
}
</style>
