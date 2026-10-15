<template>
  <div>
    <div class="section-title">纠纷仲裁中心</div>

    <div class="toolbar">
      <div class="tabs">
        <!-- 按申诉状态筛选。筛选交给后端做，因为列表可能被 keyword 收窄过，
             前端再切一刀就成了「在搜索结果里再筛」，容易和分页口径打架。 -->
        <el-tag
          v-for="item in statusTabs"
          :key="String(item.value)"
          :type="statusFilter === item.value ? 'primary' : 'info'"
          :effect="statusFilter === item.value ? 'dark' : 'plain'"
          class="tab"
          @click="
            statusFilter = item.value;
            loadAppeals();
          "
        >
          {{ item.label }}
        </el-tag>
      </div>

      <div class="toolbar-right">
        <el-input
          v-model="keyword"
          class="search-input"
          placeholder="搜索订单号/任务类型/双方昵称"
          clearable
          @keyup.enter="loadAppeals"
          @clear="loadAppeals"
        />
        <el-button type="primary" @click="loadAppeals">搜索</el-button>
      </div>
    </div>

    <el-card>
      <div class="table-header">
        <span>申诉列表，共 {{ appeals.length }} 条</span>
        <el-button @click="loadAppeals">刷新</el-button>
      </div>

      <el-table :data="appeals" v-loading="loading" border>
        <el-table-column prop="id" label="ID" width="70" />
        <el-table-column
          prop="orderNo"
          label="订单号"
          min-width="150"
          show-overflow-tooltip
        />
        <el-table-column
          prop="taskType"
          label="任务类型"
          width="110"
          show-overflow-tooltip
        />
        <el-table-column prop="reward" label="赏金" width="90">
          <template #default="{ row }">¥{{ money(row.reward) }}</template>
        </el-table-column>
        <el-table-column
          prop="appealerName"
          label="申诉人"
          width="110"
          show-overflow-tooltip
        >
          <template #default="{ row }">{{ row.appealerName || "-" }}</template>
        </el-table-column>
        <el-table-column
          prop="defendantName"
          label="被申诉人"
          width="110"
          show-overflow-tooltip
        >
          <template #default="{ row }">{{ row.defendantName || "-" }}</template>
        </el-table-column>
        <el-table-column prop="orderStatus" label="订单状态" width="100">
          <template #default="{ row }">
            <el-tag :type="orderStatusTag(row.orderStatus)" effect="plain">{{
              row.orderStatusName
            }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="status" label="申诉状态" width="100">
          <template #default="{ row }">
            <el-tag :type="row.status === 0 ? 'warning' : 'success'">{{
              row.statusName
            }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createdAt" label="申诉时间" width="160">
          <template #default="{ row }">{{ formatDateTime(row.createdAt) }}</template>
        </el-table-column>
        <el-table-column label="操作" width="150" fixed="right">
          <template #default="{ row }">
            <el-button size="small" @click="openDetail(row)">详情</el-button>
            <!-- 已处理的申诉不能再仲裁：后端用 status=0 做 CAS 占位，这里先把按钮灰掉 -->
            <el-button
              size="small"
              type="danger"
              :disabled="row.status !== 0"
              @click="openHandle(row)"
              >仲裁</el-button
            >
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <!--  对话框-申诉详情  -->
    <el-dialog v-model="detailVisible" title="申诉详情" width="760px">
      <div v-loading="detailLoading">
        <template v-if="current">
          <div class="block-title">订单信息</div>
          <div class="detail-grid">
            <div><span>订单号：</span>{{ current.orderNo }}</div>
            <div><span>任务类型：</span>{{ current.taskType }}</div>
            <div><span>赏金：</span>¥{{ money(current.reward) }}</div>
            <div>
              <span>订单状态：</span>{{ current.orderStatusName }}
            </div>
            <div><span>申诉人：</span>{{ current.appealerName || "-" }}</div>
            <div><span>被申诉人：</span>{{ current.defendantName || "-" }}</div>
            <div class="full">
              <span>申诉时间：</span>{{ formatDateTime(current.createdAt) }}
            </div>
          </div>

          <div class="block-title">申诉理由</div>
          <div class="reason-box">{{ current.reason || "-" }}</div>

          <div class="block-title">
            证据图片
            <span class="count">（{{ (current.evidenceUrls || []).length }} 张）</span>
          </div>
          <!-- preview-teleported 必须加：证据图在 el-dialog 里，不加的话大图会被
               弹窗的 z-index 和 overflow 裁掉，只能看到一小条。 -->
          <div v-if="(current.evidenceUrls || []).length" class="evidence-wrap">
            <el-image
              v-for="(url, index) in current.evidenceUrls"
              :key="url + index"
              class="evidence-img"
              :src="url"
              :preview-src-list="current.evidenceUrls"
              :initial-index="index"
              :preview-teleported="true"
              fit="cover"
            >
              <!-- campus.sql 里两条种子申诉的证据图存的是讲义时代的绝对地址
                   （http://localhost:8080/api/files/orders/...，8080 + /api 前缀 + 旧目录结构），
                   文件本身根本没落过盘，取不回来。给个「已失效」占位，
                   比浏览器那个裂图图标清楚。 -->
              <template #error>
                <div class="img-error">图片已失效</div>
              </template>
            </el-image>
          </div>
          <div v-else class="empty-tip">该申诉没有上传证据图片</div>

          <div class="block-title">处理结果</div>
          <div v-if="current.status === 1" class="detail-grid">
            <div><span>处理状态：</span>{{ current.statusName }}</div>
            <div><span>处理人：</span>{{ current.adminName || "-" }}</div>
            <div class="full">
              <span>处理说明：</span>{{ current.adminRemark || "-" }}
            </div>
            <div class="full">
              <span>处理时间：</span>{{ formatDateTime(current.updatedAt) }}
            </div>
          </div>
          <div v-else class="empty-tip">该申诉还没有被处理</div>

          <!-- 需求 3.5.3 的「查看聊天记录与证据」：证据图在上一块，
               这一块是双方当时的原始对话 —— 谁爽约、有没有提前说清楚，看这里最准 -->
          <div class="block-title">
            聊天记录
            <span class="count">（{{ appealMessages.length }} 条）</span>
          </div>
          <div v-if="chatLoading" class="empty-tip">聊天记录加载中…</div>
          <template v-else-if="appealMessages.length">
            <div class="chat-legend">
              <span class="legend-dot appealer"></span>申诉人
              <span class="legend-dot defendant"></span>被申诉人
            </div>
            <div class="chat-wrap">
              <div
                v-for="msg in appealMessages"
                :key="msg.id"
                class="chat-item"
                :class="msg.senderId === current.appealerId ? 'from-appealer' : 'from-defendant'"
              >
                <div class="chat-meta">
                  {{ msg.senderName || "-" }} · {{ formatDateTime(msg.createdAt) }}
                </div>
                <div class="chat-bubble">{{ msg.content }}</div>
              </div>
            </div>
          </template>
          <div v-else class="empty-tip">该订单无聊天记录，双方未在平台内沟通</div>
        </template>
      </div>
      <template #footer>
        <el-button @click="detailVisible = false">关闭</el-button>
        <el-button
          v-if="current && current.status === 0"
          type="danger"
          @click="
            detailVisible = false;
            openHandle(current);
          "
          >去仲裁</el-button
        >
      </template>
    </el-dialog>

    <!--  对话框-仲裁处理  -->
    <el-dialog v-model="handleVisible" title="仲裁处理" width="620px">
      <div v-if="current" class="handle-order">
        订单 {{ current.orderNo }} · 赏金 ¥{{ money(current.reward) }} ·
        {{ current.taskType }}<br />
        申诉人 {{ current.appealerName || "-" }} ／ 被申诉人
        {{ current.defendantName || "-" }}
      </div>

      <el-form label-width="90px">
        <el-form-item label="仲裁动作">
          <!-- 用 value 而不是 label 传选中值：Element Plus 2.6 起把「label 当值用」
               标成了弃用（3.0 移除），这里传 value 才是当前写法，控制台也不打警告 -->
          <el-checkbox-group v-model="handleForm.actions">
            <el-checkbox value="REFUND">
              强制退款
              <span class="opt-tip">把托管资金退回发布者，订单转「已取消」</span>
            </el-checkbox>
            <el-checkbox value="COMPENSATE">
              补偿
              <span class="opt-tip">从被申诉人余额扣款赔给申诉人，信用分 -5</span>
            </el-checkbox>
            <el-checkbox value="BAN">
              封号
              <span class="opt-tip">停用被申诉人账号（管理员账号不受影响）</span>
            </el-checkbox>
            <el-checkbox value="REJECT">
              驳回申诉
              <span class="opt-tip">订单恢复申诉前状态（与上面三个互斥）</span>
            </el-checkbox>
          </el-checkbox-group>
        </el-form-item>

        <el-form-item v-if="needAmount" label="补偿金额">
          <el-input-number
            v-model="handleForm.amount"
            :min="0.01"
            :precision="2"
            :step="5"
            style="width: 220px"
          />
          <div class="form-tip">
            从被申诉人的可用余额里实际扣款，余额不足时后端会直接报错（不做部分扣款），
            改小金额重试即可。
          </div>
        </el-form-item>

        <el-form-item label="处理说明">
          <el-input
            v-model="handleForm.remark"
            type="textarea"
            :rows="3"
            maxlength="200"
            show-word-limit
            placeholder="会记录进申诉档案，建议写清判断依据"
          />
        </el-form-item>
      </el-form>

      <template #footer>
        <el-button @click="handleVisible = false">取消</el-button>
        <el-button type="danger" :loading="submitting" @click="submitHandle"
          >确认处理</el-button
        >
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from "vue";
import { ElMessage } from "element-plus";
import { fetchAppeals, fetchAppealDetail, fetchAppealMessages, handleAppeal } from "@/api/appeal";

const loading = ref(false);
const detailLoading = ref(false);
const submitting = ref(false);
const appeals = ref([]);
const current = ref(null);
// 该申诉对应订单的聊天记录，随详情一起展示
const appealMessages = ref([]);
const chatLoading = ref(false);
const detailVisible = ref(false);
const handleVisible = ref(false);

const keyword = ref("");
// null = 全部；跟 el-tag 的 value 比较，所以不能写成空字符串
const statusFilter = ref(null);

//appeals.status：0 待处理、1 已处理（沿用种子数据的语义，没有第三种）
const statusTabs = [
  { value: null, label: "全部" },
  { value: 0, label: "待处理" },
  { value: 1, label: "已处理" },
];

const handleForm = ref(createDefaultHandleForm());

//勾了「补偿」才需要填金额
const needAmount = computed(() =>
  handleForm.value.actions.includes("COMPENSATE")
);

onMounted(loadAppeals);

async function loadAppeals() {
  loading.value = true;
  try {
    const params = {};
    if (statusFilter.value !== null) {
      params.status = statusFilter.value;
    }
    if (keyword.value.trim()) {
      params.keyword = keyword.value.trim();
    }
    appeals.value = (await fetchAppeals(params)) || [];
  } finally {
    loading.value = false;
  }
}

function createDefaultHandleForm() {
  return { actions: [], amount: 10, remark: "" };
}

//详情单独拉一次接口，而不是直接用列表里那行：列表行里的证据图和处理说明
//可能被后端裁过，而且详情接口才是「打开档案」的正式入口。
async function openDetail(row) {
  detailVisible.value = true;
  detailLoading.value = true;
  current.value = row;
  appealMessages.value = [];
  try {
    current.value = (await fetchAppealDetail(row.id)) || row;
  } finally {
    detailLoading.value = false;
  }

  // 聊天记录单独拉一次，不塞进上面那个 try 里：它只是详情中的一块附加信息，
  // 拉不到也只是空着，不该把整份申诉详情带成加载失败。
  chatLoading.value = true;
  try {
    appealMessages.value = (await fetchAppealMessages(row.id)) || [];
  } catch (e) {
    // 拦截器已经弹过提示了，这里兜住不让弹窗崩
    appealMessages.value = [];
  } finally {
    chatLoading.value = false;
  }
}

function openHandle(row) {
  current.value = row;
  handleForm.value = createDefaultHandleForm();
  handleVisible.value = true;
}

async function submitHandle() {
  const actions = handleForm.value.actions;
  if (!actions.length) {
    ElMessage.warning("请至少选择一个仲裁动作");
    return;
  }
  if (actions.includes("REJECT") && actions.length > 1) {
    ElMessage.warning("「驳回申诉」与其它仲裁动作互斥，请二选一");
    return;
  }
  if (actions.includes("COMPENSATE") && !(handleForm.value.amount > 0)) {
    ElMessage.warning("请填写大于 0 的补偿金额");
    return;
  }

  submitting.value = true;
  try {
    const data = { actions, remark: handleForm.value.remark };
    if (actions.includes("COMPENSATE")) {
      data.amount = handleForm.value.amount;
    }
    const result = await handleAppeal(current.value.id, data);
    //后端会把「实际做了什么」写进 admin_remark（比如无托管资金时退款是跳过还是执行），
    //这里原样透出来，别用一句笼统的「处理成功」盖掉。
    ElMessage.success(result?.adminRemark || "仲裁处理完成");
    handleVisible.value = false;
    await loadAppeals();
  } finally {
    submitting.value = false;
  }
}

//orders.status 的颜色：进行中/待确认是需要注意的，已完成是好的，已取消/申诉中是异常的
function orderStatusTag(status) {
  if (status === 4) return "success";
  if (status === 5 || status === 6) return "danger";
  if (status === 2 || status === 3) return "warning";
  return "info";
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

.block-title {
  font-weight: 700;
  color: #1e293b;
  margin: 18px 0 10px;
}

.block-title:first-child {
  margin-top: 0;
}

.count {
  font-weight: 400;
  color: #94a3b8;
  font-size: 13px;
}

.detail-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 10px 12px;
}

.detail-grid .full {
  grid-column: 1 / -1;
}

.detail-grid span {
  color: #64748b;
}

.reason-box {
  background: #f8fafc;
  border: 1px solid #e2e8f0;
  border-radius: 8px;
  padding: 12px;
  line-height: 1.7;
  color: #1e293b;
  white-space: pre-wrap;
  word-break: break-word;
}

.evidence-wrap {
  display: flex;
  gap: 10px;
  flex-wrap: wrap;
}

/* ============ 聊天记录（需求 3.5.3）============
   管理员是第三方，所以不按「我/对方」分左右，按申诉人/被申诉人分左右，
   配色和上面「证据图片」保持同一套灰蓝，不抢眼。 */
.chat-legend {
  display: flex;
  align-items: center;
  gap: 6px;
  margin-bottom: 10px;
  color: #94a3b8;
  font-size: 12px;
}

.legend-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  margin-left: 8px;
}

.legend-dot.appealer {
  background: #818cf8;
}

.legend-dot.defendant {
  background: #94a3b8;
}

.chat-wrap {
  display: flex;
  flex-direction: column;
  gap: 10px;
  max-height: 320px;
  overflow-y: auto;
  padding: 12px;
  border-radius: 8px;
  background: #f8fafc;
  border: 1px solid #e2e8f0;
}

.chat-item {
  display: flex;
  flex-direction: column;
  max-width: 78%;
}

.from-appealer {
  align-self: flex-start;
  align-items: flex-start;
}

.from-defendant {
  align-self: flex-end;
  align-items: flex-end;
}

.chat-meta {
  color: #94a3b8;
  font-size: 12px;
  margin-bottom: 4px;
}

.chat-bubble {
  padding: 8px 12px;
  border-radius: 10px;
  font-size: 13px;
  line-height: 1.6;
  white-space: pre-wrap;
  word-break: break-word;
}

.from-appealer .chat-bubble {
  background: #eef2ff;
  border: 1px solid #c7d2fe;
  color: #1e293b;
}

.from-defendant .chat-bubble {
  background: #fff;
  border: 1px solid #e2e8f0;
  color: #1e293b;
}

.evidence-img {
  width: 120px;
  height: 120px;
  border-radius: 8px;
  border: 1px solid #e2e8f0;
  cursor: zoom-in;
}

.img-error {
  width: 100%;
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  background: #f8fafc;
  color: #cbd5e1;
  font-size: 12px;
}

.empty-tip {
  color: #94a3b8;
  font-size: 13px;
}

.handle-order {
  background: #f8fafc;
  border-radius: 8px;
  padding: 12px;
  margin-bottom: 16px;
  line-height: 1.8;
  color: #475569;
}

.opt-tip {
  color: #94a3b8;
  font-size: 12px;
  margin-left: 6px;
}

.form-tip {
  color: #94a3b8;
  font-size: 12px;
  line-height: 1.6;
}
</style>
