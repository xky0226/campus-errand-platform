<template>
  <!-- 右侧正文 -->
  <main class="main">
    <header class="header">
      <div>
        <h2>管理员首页</h2>
        <p>查看平台运行概况和核心数据</p>
      </div>
      <div class="admin-box">
        <span>{{ adminName }}</span>
        <el-button type="danger" plain @click="logout">退出登录</el-button>
      </div>
    </header>

    <section class="cards">
      <!-- 遍历统计数据-进行展示 -->
      <el-card v-for="item in statCards" :key="item.label" class="stat-card">
        <div class="stat-label">{{ item.label }}</div>
        <div class="stat-value">{{ item.value }}</div>
      </el-card>
    </section>

    <section class="content-grid">
      <!-- 近七日订单趋势 报表 -->
      <el-card class="chart-card">
        <template #header>近七日订单趋势</template>
        <div ref="chartRef" class="chart"></div>
      </el-card>

      <!-- 最近订单 报表 -->
      <el-card class="table-card">
        <template #header>最近订单</template>
        <el-table :data="recentOrders" height="320">
          <el-table-column prop="orderNo" label="订单号" min-width="150" />
          <el-table-column prop="taskType" label="类型" width="100" />
          <el-table-column prop="reward" label="金额" width="100" />
          <el-table-column prop="status" label="状态" width="100" />
        </el-table>
      </el-card>
    </section>
  </main>
</template>

<script setup>
import { computed, nextTick, onMounted, ref } from "vue";
import { useRouter } from "vue-router";
import * as echarts from "echarts";
import { get } from "@/api";

//定义变量
const router = useRouter();
const chartRef = ref();
const statistics = ref({});
const recentOrders = ref([]);
const orderTrend = ref([]);

const adminInfo = JSON.parse(localStorage.getItem("admin_info") || "{}");
// Users 实体里没有 username 字段，只有 nickname / realName（讲义 3-3 写的 username 取不到值）
const adminName = computed(() => adminInfo.nickname || adminInfo.realName || "管理员");

const statCards = computed(() => [
  { label: "用户总数", value: statistics.value.userCount || 0 },
  { label: "订单总数", value: statistics.value.orderCount || 0 },
  { label: "待审核认证", value: statistics.value.pendingVerifyCount || 0 },
  { label: "待处理申诉", value: statistics.value.pendingAppealCount || 0 },
  { label: "今日订单", value: statistics.value.todayOrderCount || 0 },
  {
    label: "平台交易额",
    value: "¥" + Number(statistics.value.totalAmount || 0).toFixed(2),
  },
]);

//页面加载的生命周期中发起数据查询
onMounted(async () => {
  await loadDashboard();
  await nextTick();
  renderChart();
});

async function loadDashboard() {
  statistics.value = (await get("/admin/dashboard/statistics")) || {};
  recentOrders.value = (await get("/admin/dashboard/recent-orders")) || [];
  orderTrend.value = (await get("/admin/dashboard/order-trend")) || [];
}

//将数据初始化到报表中
function renderChart() {
  if (!chartRef.value) return;

  const chart = echarts.init(chartRef.value);
  chart.setOption({
    tooltip: { trigger: "axis" },
    grid: { left: 40, right: 20, top: 30, bottom: 30 },
    xAxis: {
      type: "category",
      data: orderTrend.value.map((item) => item.date),
    },
    yAxis: {
      type: "value",
    },
    series: [
      {
        name: "订单数",
        type: "line",
        smooth: true,
        data: orderTrend.value.map((item) => item.count),
        areaStyle: {},
      },
    ],
  });

  window.addEventListener("resize", () => chart.resize());
}

function logout() {
  localStorage.removeItem("admin_token");
  localStorage.removeItem("admin_info");
  router.push("/login");
}
</script>

<style scoped>
.main {
  flex: 1;
  padding: 24px;
}

.header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 24px;
}

.header h2 {
  margin: 0;
  color: #1e293b;
}

.header p {
  margin: 8px 0 0;
  color: #64748b;
}

.admin-box {
  display: flex;
  align-items: center;
  gap: 16px;
}

.cards {
  display: grid;
  grid-template-columns: repeat(6, minmax(0, 1fr));
  gap: 16px;
  margin-bottom: 24px;
}

.stat-card {
  border-radius: 12px;
}

.stat-label {
  color: #64748b;
  font-size: 14px;
}

.stat-value {
  margin-top: 12px;
  color: #1e293b;
  font-size: 26px;
  font-weight: 700;
}

.content-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 16px;
}

.chart {
  height: 320px;
}

@media (max-width: 1200px) {
  .cards {
    grid-template-columns: repeat(3, minmax(0, 1fr));
  }

  .content-grid {
    grid-template-columns: 1fr;
  }
}
</style>
