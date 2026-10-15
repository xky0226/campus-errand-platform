<template>
  <div>
    <div class="section-title">实名认证审核</div>

    <el-alert type="info" :closable="false" show-icon class="workflow-tip">
      <template #title>
        学生在小程序「个人中心 → 实名认证」提交姓名和学生证照片后，状态变为<b>审核中</b>并出现在下面这个队列里。 通过后学生状态变为<b>已认证</b>；驳回会<b>清空已上传的学生证照片</b>，学生需要重新提交。
      </template>
    </el-alert>

    <div class="toolbar">
      <div class="pending-count">
        待审核 <b>{{ filteredUsers.length }}</b> 人
      </div>
      <div class="toolbar-right">
        <el-input
          v-model="keyword"
          class="search-input"
          placeholder="搜索昵称/姓名/学号"
          clearable
        />
        <el-button @click="loadList">刷新</el-button>
      </div>
    </div>

    <el-card>
      <el-table :data="filteredUsers" v-loading="loading" border>
        <el-table-column prop="id" label="ID" width="70" />
        <el-table-column prop="nickname" label="昵称" min-width="120" show-overflow-tooltip />
        <el-table-column prop="realName" label="真实姓名" width="110">
          <template #default="{ row }">{{ row.realName || "-" }}</template>
        </el-table-column>
        <el-table-column prop="studentId" label="学号" width="130">
          <template #default="{ row }">{{ row.studentId || "-" }}</template>
        </el-table-column>
        <el-table-column prop="phone" label="手机号" width="130">
          <template #default="{ row }">{{ row.phone || "-" }}</template>
        </el-table-column>
        <el-table-column prop="creditScore" label="信用分" width="80" />

        <!-- 学生证照片：这是审核的主要依据，做成缩略图可点开看大图 -->
        <el-table-column label="学生证照片" width="120">
          <template #default="{ row }">
            <el-image
              v-if="row.studentCertUrl"
              class="cert-thumb"
              :src="row.studentCertUrl"
              :preview-src-list="[row.studentCertUrl]"
              :initial-index="0"
              :preview-teleported="true"
              fit="cover"
            >
              <template #error>
                <div class="cert-broken">图片无法加载</div>
              </template>
            </el-image>
            <!-- 种子数据里有 4 个用户根本没传学生证图，这是正常情况，不显示裂图占位 -->
            <span v-else class="cert-missing">未上传</span>
          </template>
        </el-table-column>

        <el-table-column label="提交时间" width="150">
          <template #default="{ row }">{{ formatDateTime(row.updatedAt) }}</template>
        </el-table-column>

        <el-table-column label="操作" width="200" fixed="right">
          <template #default="{ row }">
            <el-button size="small" @click="openDetail(row)">详情</el-button>
            <el-button size="small" type="success" @click="approve(row)">通过</el-button>
            <el-button size="small" type="danger" @click="reject(row)">驳回</el-button>
          </template>
        </el-table-column>

        <template #empty>
          <div class="empty-tip">当前没有待审核的实名认证申请</div>
        </template>
      </el-table>
    </el-card>

    <!-- 详情弹窗：把学生证图放大看，审核时主要看这个 -->
    <el-dialog v-model="detailVisible" title="实名认证审核" width="720px">
      <div v-if="selected" class="detail-grid">
        <div><span>用户 ID：</span>{{ selected.id }}</div>
        <div><span>昵称：</span>{{ selected.nickname }}</div>
        <div><span>真实姓名：</span>{{ selected.realName || "-" }}</div>
        <div><span>学号：</span>{{ selected.studentId || "-" }}</div>
        <div><span>手机号：</span>{{ selected.phone || "-" }}</div>
        <div><span>信用分：</span>{{ selected.creditScore }}</div>
        <div><span>角色：</span>{{ roleTextMap[selected.role] || "-" }}</div>
        <div><span>账号状态：</span>{{ selected.status === 1 ? "正常" : "已禁用" }}</div>
        <div class="full">
          <span>注册时间：</span>{{ formatDateTime(selected.createdAt) }}
        </div>
      </div>

      <div class="cert-block">
        <div class="cert-block-title">学生证照片</div>
        <el-image
          v-if="selected && selected.studentCertUrl"
          class="cert-large"
          :src="selected.studentCertUrl"
          :preview-src-list="[selected.studentCertUrl]"
          :preview-teleported="true"
          fit="contain"
        >
          <template #error>
            <div class="cert-broken-large">
              图片无法加载<br />
              <span class="cert-broken-hint">{{ selected.studentCertUrl }}</span>
            </div>
          </template>
        </el-image>
        <div v-else class="cert-missing-large">
          该用户没有上传学生证照片，无法核验身份
        </div>
      </div>

      <template #footer>
        <el-button @click="detailVisible = false">关闭</el-button>
        <el-button type="danger" @click="reject(selected)">驳回</el-button>
        <el-button type="success" @click="approve(selected)">通过</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from "vue";
import { ElMessage, ElMessageBox } from "element-plus";
import { fetchPendingVerify, verifyUser } from "@/api/user";

const loading = ref(false);
const users = ref([]);
const keyword = ref("");
const selected = ref(null);
const detailVisible = ref(false);

//数值含义来自后端 VerifyStatus：0 未认证 / 1 已认证 / 2 审核中。
//这个页面拿到的全是 2，写全只是为了让详情弹窗兜底不出 undefined。
const roleTextMap = {
  0: "需求者",
  1: "跑腿员",
  2: "管理员",
};

const filteredUsers = computed(() => {
  const kw = keyword.value.trim().toLowerCase();
  if (!kw) return users.value;
  return users.value.filter(
    (item) =>
      String(item.nickname || "").toLowerCase().includes(kw) ||
      String(item.realName || "").toLowerCase().includes(kw) ||
      String(item.studentId || "").toLowerCase().includes(kw)
  );
});

onMounted(loadList);

async function loadList() {
  loading.value = true;
  try {
    users.value = (await fetchPendingVerify()) || [];
  } finally {
    loading.value = false;
  }
}

function openDetail(row) {
  selected.value = row;
  detailVisible.value = true;
}

//通过：后端会把 verified_status 从 2 改成 1
async function approve(row) {
  try {
    await ElMessageBox.confirm(
      `确认通过「${row.realName || row.nickname}」的实名认证吗？`,
      "审核通过",
      { type: "success" }
    );
  } catch {
    //点了取消
    return;
  }
  await verifyUser(row.id, true);
  ElMessage.success("已通过实名认证");
  detailVisible.value = false;
  await loadList();
}

//驳回：必须填理由，后端也会再校验一次
async function reject(row) {
  let reason = "";
  try {
    const res = await ElMessageBox.prompt(
      `驳回「${row.realName || row.nickname}」的实名认证申请。` +
        `驳回后该学生的学生证照片会被清空，需要重新提交。请填写驳回理由：`,
      "驳回认证",
      {
        type: "warning",
        inputPlaceholder: "例如：学生证照片看不清姓名",
        inputValidator: (value) => (value && value.trim() ? true : "请填写驳回理由"),
      }
    );
    reason = res.value;
  } catch {
    //点了取消
    return;
  }
  await verifyUser(row.id, false, reason.trim());
  ElMessage.success("已驳回，该学生需要重新提交资料");
  detailVisible.value = false;
  await loadList();
}

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

.workflow-tip {
  margin-bottom: 16px;
}

.toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 16px;
  flex-wrap: wrap;
}

.pending-count {
  color: #64748b;
  font-size: 14px;
}

.pending-count b {
  color: #f59e0b;
  font-size: 18px;
}

.toolbar-right {
  display: flex;
  align-items: center;
  gap: 12px;
}

.search-input {
  width: 280px;
}

.cert-thumb {
  width: 76px;
  height: 76px;
  border-radius: 6px;
  cursor: pointer;
  display: block;
}

.cert-broken {
  width: 76px;
  height: 76px;
  display: flex;
  align-items: center;
  justify-content: center;
  border: 1px dashed #fca5a5;
  border-radius: 6px;
  color: #ef4444;
  font-size: 12px;
  line-height: 1.3;
  text-align: center;
  padding: 4px;
  box-sizing: border-box;
}

.cert-missing {
  color: #94a3b8;
  font-size: 13px;
}

.empty-tip {
  padding: 30px 0;
  color: #94a3b8;
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

.cert-block {
  margin-top: 20px;
  padding-top: 16px;
  border-top: 1px solid #f1f5f9;
}

.cert-block-title {
  margin-bottom: 12px;
  color: #334155;
  font-weight: 700;
}

.cert-large {
  width: 100%;
  max-height: 380px;
  border-radius: 8px;
  background: #f8fafc;
}

.cert-broken-large {
  height: 220px;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 10px;
  border: 1px dashed #fca5a5;
  border-radius: 8px;
  color: #ef4444;
  font-size: 14px;
}

.cert-broken-hint {
  color: #94a3b8;
  font-size: 12px;
  word-break: break-all;
}

.cert-missing-large {
  padding: 50px 0;
  border: 1px dashed #cbd5e1;
  border-radius: 8px;
  color: #94a3b8;
  text-align: center;
}
</style>
