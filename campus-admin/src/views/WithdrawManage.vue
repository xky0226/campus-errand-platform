<template>
  <div>
    <div class="section-title">提现审核</div>

    <div class="toolbar">
      <div class="tabs">
        <el-tag
          v-for="item in statusTabs"
          :key="String(item.value)"
          :type="statusFilter === item.value ? 'primary' : 'info'"
          :effect="statusFilter === item.value ? 'dark' : 'plain'"
          class="tab"
          @click="
            statusFilter = item.value;
            loadWithdrawals();
          "
        >
          {{ item.label }}
        </el-tag>
      </div>

      <div class="toolbar-right">
        <span class="hint"
          >待审核 {{ pendingCount }} 笔 · 提现申请提交时钱已转入冻结，通过才真正扣掉</span
        >
      </div>
    </div>

    <el-card>
      <div class="table-header">
        <span>提现申请，共 {{ withdrawals.length }} 条</span>
        <el-button @click="loadWithdrawals">刷新</el-button>
      </div>

      <el-table :data="withdrawals" v-loading="loading" border>
        <el-table-column prop="id" label="ID" width="70" />
        <el-table-column prop="userId" label="用户ID" width="90" />
        <el-table-column prop="amount" label="提现金额" width="120">
          <template #default="{ row }">
            <span class="amount">¥{{ money(row.amount) }}</span>
          </template>
        </el-table-column>
        <el-table-column label="提现账号" min-width="180" show-overflow-tooltip>
          <template #default="{ row }">
            <span :class="{ 'legacy-account': !row.thirdPartyAccount }">{{
              accountOf(row) || "-"
            }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="status" label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="statusTagMap[row.status]">{{
              statusTextMap[row.status] || "-"
            }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createdAt" label="申请时间" width="160">
          <template #default="{ row }">{{ formatDateTime(row.createdAt) }}</template>
        </el-table-column>
        <el-table-column prop="remark" label="备注 / 处理结果" min-width="240">
          <template #default="{ row }">
            <span class="remark">{{ row.remark || "-" }}</span>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="160" fixed="right">
          <template #default="{ row }">
            <el-button
              size="small"
              type="success"
              :disabled="row.status !== 0"
              @click="approve(row)"
              >通过</el-button
            >
            <el-button
              size="small"
              type="danger"
              :disabled="row.status !== 0"
              @click="reject(row)"
              >驳回</el-button
            >
          </template>
        </el-table-column>
      </el-table>
    </el-card>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from "vue";
import { ElMessage, ElMessageBox } from "element-plus";
import { auditWithdraw, fetchWithdrawals } from "@/api/finance";

const loading = ref(false);
const withdrawals = ref([]);
// null = 全部；跟 el-tag 的 value 比较，所以不能写成空字符串
const statusFilter = ref(null);

//wallet_transactions.status：0 待审核、1 已通过、2 已驳回
//（同一个字段在别的流水类型里只表示「处理中/已完成」，提现多用了 2 = 驳回）
const statusTabs = [
  { value: null, label: "全部" },
  { value: 0, label: "待审核" },
  { value: 1, label: "已通过" },
  { value: 2, label: "已驳回" },
];

const statusTextMap = {
  0: "待审核",
  1: "已通过",
  2: "已驳回",
};

const statusTagMap = {
  0: "warning",
  1: "success",
  2: "danger",
};

const pendingCount = computed(
  () => withdrawals.value.filter((item) => item.status === 0).length
);

onMounted(loadWithdrawals);

async function loadWithdrawals() {
  loading.value = true;
  try {
    withdrawals.value = (await fetchWithdrawals(statusFilter.value)) || [];
  } finally {
    loading.value = false;
  }
}

//审核通过：钱真的转出去了，冻结余额扣掉，可用余额不变
async function approve(row) {
  try {
    await ElMessageBox.confirm(
      `确认通过用户 ${row.userId} 的 ¥${money(row.amount)} 提现申请吗？通过后冻结金额会被扣掉。`,
      "提示",
      { type: "warning" }
    );
  } catch {
    //点了取消
    return;
  }
  await auditWithdraw(row.id, true, "");
  ElMessage.success("已通过");
  await loadWithdrawals();
}

//审核驳回：钱退回用户的可用余额
async function reject(row) {
  let remark = "";
  try {
    const result = await ElMessageBox.prompt(
      `确认驳回用户 ${row.userId} 的 ¥${money(row.amount)} 提现申请吗？金额会退回其可用余额。`,
      "驳回提现",
      {
        type: "warning",
        inputPlaceholder: "请输入驳回理由（可留空）",
        inputValidator: () => true,
        confirmButtonText: "确认驳回",
      }
    );
    remark = result.value || "";
  } catch {
    //点了取消
    return;
  }
  await auditWithdraw(row.id, false, remark);
  ElMessage.success("已驳回");
  await loadWithdrawals();
}

//收款账号从 remark 里解析出来。
//为什么不直接读 third_party_account 那一列：它在 DDL 里是 varchar(10)，
//连手机号（11 位）都存不下，写进去会直接抛 Data too long 并把整笔提现回滚。
//所以 withdraw() 只把账号拼进备注，有两种历史格式：
//  代码生成的  record(): 「提现到134135161441，余额 180.00 元」
//  种子数据手写的:      「提现到支付宝: 134135161441」
//所以结束边界既要认「，」也要认结尾；「（」是 listWithdrawals 追加的昵称。
//实在解析不出来的（种子里那条「申请提现」）就显示 "-"，不硬编。
function accountOf(row) {
  if (row.thirdPartyAccount) return row.thirdPartyAccount;
  const matched = /提现到(.+?)(?:，|（|$)/.exec(row.remark || "");
  return matched ? matched[1] : "";
}

function money(value) {
  return Number(value || 0).toFixed(2);
}

function formatDateTime(value) {
  if (!value) return "-";
  const date = new Date(value);
  const month = String(date.getMonth() + 1).padStart(2, "0");
  const day = String(date.getDate()).padStart(2, "0");
  const hour = String(date.getHours()).padStart(2, "0");
  const minute = String(date.getMinutes()).padStart(2, "0");
  return `${date.getFullYear()}-${month}-${day} ${hour}:${minute}`;
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

.hint {
  color: #94a3b8;
  font-size: 13px;
}

.table-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 12px;
}

.amount {
  font-weight: 700;
  color: #f56c6c;
}

/* 兜底解析出来的账号标灰，跟真实落库的区分开 */
.legacy-account {
  color: #94a3b8;
}

.remark {
  color: #475569;
  font-size: 13px;
}
</style>
