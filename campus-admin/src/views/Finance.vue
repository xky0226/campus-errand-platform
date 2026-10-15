<template>
  <div>
    <div class="section-title">
      财务统计
      <el-button class="export-btn" :loading="exporting" @click="handleExport"
        >导出财务报表</el-button
      >
    </div>

    <section class="cards">
      <el-card v-for="item in statCards" :key="item.label" class="stat-card">
        <div class="stat-label">{{ item.label }}</div>
        <div class="stat-value">{{ item.value }}</div>
        <div class="stat-sub">{{ item.sub }}</div>
      </el-card>
    </section>

    <el-card class="chart-card">
      <template #header>
        <div class="card-header">
          <span>营收趋势</span>
          <el-radio-group v-model="period" size="small" @change="loadRevenue">
            <el-radio-button value="day">近 7 天</el-radio-button>
            <el-radio-button value="week">近 6 周</el-radio-button>
            <el-radio-button value="month">近 6 个月</el-radio-button>
          </el-radio-group>
        </div>
      </template>
      <div ref="chartRef" class="chart" v-loading="revenueLoading"></div>
      <!-- 全 0 的时候要解释一句，否则看起来像页面坏了 -->
      <el-alert
        v-if="!revenueHasData && !revenueLoading"
        class="chart-alert"
        type="info"
        :closable="false"
        show-icon
        title="所选区间内没有已完成的订单支付流水"
        description="只有状态为「已完成」且金额为正的 order_pay 流水才计入本图。演示时在小程序端下一单并支付，这里立刻会出现数据。"
      />
      <div class="form-tip">
        口径说明：平台目前不抽成（<code>orders.platform_fee</code> 恒为
        0），赏金全额结算给跑腿员。因此这里统计的是平台上的订单支付交易额，
        不是平台自己的收入。
      </div>
    </el-card>

    <el-card class="table-card">
      <template #header>流水类型汇总（只统计已完成的流水）</template>
      <el-table :data="statistics.rows || []" border>
        <el-table-column prop="type" label="类型标识" width="150" />
        <el-table-column prop="typeName" label="类型" width="130" />
        <el-table-column prop="count" label="笔数" width="100" />
        <el-table-column prop="amount" label="金额合计">
          <template #default="{ row }">
            <span class="amount">¥{{ money(row.amount) }}</span>
          </template>
        </el-table-column>
      </el-table>
    </el-card>
  </div>
</template>

<script setup>
import { computed, nextTick, onMounted, onUnmounted, ref } from "vue";
import * as echarts from "echarts";
import { ElMessage } from "element-plus";
import { exportFinance, fetchFinanceStatistics, fetchRevenue } from "@/api/finance";

const chartRef = ref();
const statistics = ref({});
const revenue = ref([]);
const revenueLoading = ref(false);
const exporting = ref(false);
const period = ref("day");

//chart 实例放在闭包里而不是 ref：echarts 的实例不需要响应式，
//塞进 ref 反而会被 Vue 包一层 Proxy，resize/dispose 时容易出怪问题。
let chart = null;

const statCards = computed(() => [
  {
    label: "累计充值",
    value: "¥" + money(statistics.value.totalRecharge),
    sub: "用户充值成功金额",
  },
  {
    label: "订单交易额",
    value: "¥" + money(statistics.value.totalPay),
    sub: "已完成的订单支付流水",
  },
  {
    label: "跑腿员收益",
    value: "¥" + money(statistics.value.totalEarn),
    sub: "已结算给跑腿员的赏金",
  },
  {
    label: "提现总额",
    value: "¥" + money(statistics.value.totalWithdraw),
    sub: "审核通过并转出的金额",
  },
  {
    label: "待审核提现",
    //统计接口返回的是 long，JSON 里就是数字，不用再处理
    value: statistics.value.pendingWithdrawCount || 0,
    sub: "笔，去「提现审核」处理",
  },
]);

const revenueHasData = computed(() =>
  revenue.value.some((item) => Number(item.count || 0) > 0)
);

onMounted(async () => {
  await Promise.all([loadStatistics(), loadRevenue()]);
  await nextTick();
  renderChart();
  window.addEventListener("resize", handleResize);
});

//组件销毁时必须 dispose：Vue 只清 DOM，echarts 实例还挂在被移除的节点上，
//来回切菜单会一直堆积。
onUnmounted(() => {
  window.removeEventListener("resize", handleResize);
  if (chart) {
    chart.dispose();
    chart = null;
  }
});

function handleResize() {
  if (chart) chart.resize();
}

//导出全量资金流水明细。和订单报表共用一个下载工具（@/api/download），
//失败时抛出来的 message 就是后端那句业务提示，直接展示。
async function handleExport() {
  exporting.value = true;
  try {
    const filename = await exportFinance();
    ElMessage.success(`财务报表已导出：${filename}`);
  } catch (error) {
    ElMessage.error(error.message || "导出失败");
  } finally {
    exporting.value = false;
  }
}

async function loadStatistics() {
  statistics.value = (await fetchFinanceStatistics()) || {};
}

async function loadRevenue() {
  revenueLoading.value = true;
  try {
    revenue.value = (await fetchRevenue(period.value)) || [];
    //切区间只换数据，图不重建
    if (chart) {
      chart.setOption(buildOption());
    }
  } finally {
    revenueLoading.value = false;
  }
}

function renderChart() {
  if (!chartRef.value) return;
  chart = echarts.init(chartRef.value);
  chart.setOption(buildOption());
}

function buildOption() {
  return {
    tooltip: { trigger: "axis" },
    legend: { data: ["交易额", "交易笔数"], top: 0 },
    grid: { left: 60, right: 50, top: 40, bottom: 30 },
    xAxis: {
      type: "category",
      data: revenue.value.map((item) => item.label),
    },
    //双 Y 轴：笔数是个位数、金额是几百，共用一根轴的话柱子会被压成一条线
    yAxis: [
      { type: "value", name: "金额(元)" },
      { type: "value", name: "笔数", minInterval: 1 },
    ],
    series: [
      {
        name: "交易额",
        type: "line",
        smooth: true,
        areaStyle: {},
        data: revenue.value.map((item) => Number(item.amount || 0)),
      },
      {
        name: "交易笔数",
        type: "bar",
        yAxisIndex: 1,
        barWidth: 18,
        data: revenue.value.map((item) => item.count),
      },
    ],
  };
}

function money(value) {
  return Number(value || 0).toFixed(2);
}
</script>

<style scoped>
.section-title {
  font-size: 20px;
  font-weight: 700;
  margin-bottom: 20px;
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.export-btn {
  font-weight: 400;
  font-size: 14px;
}

.cards {
  display: grid;
  grid-template-columns: repeat(5, minmax(0, 1fr));
  gap: 16px;
  margin-bottom: 16px;
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
  font-size: 24px;
  font-weight: 700;
}

.stat-sub {
  margin-top: 8px;
  color: #94a3b8;
  font-size: 12px;
}

.chart-card {
  margin-bottom: 16px;
}

.card-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.chart {
  height: 320px;
}

.chart-alert {
  margin-top: 12px;
}

.table-card {
  margin-bottom: 16px;
}

.amount {
  font-weight: 700;
  color: #f56c6c;
}

.form-tip {
  margin-top: 12px;
  color: #94a3b8;
  font-size: 12px;
  line-height: 1.7;
}

.form-tip code {
  background: #f1f5f9;
  padding: 1px 4px;
  border-radius: 3px;
}

@media (max-width: 1200px) {
  .cards {
    grid-template-columns: repeat(3, minmax(0, 1fr));
  }
}
</style>
