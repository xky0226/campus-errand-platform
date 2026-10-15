<template>
  <div class="dashboard-page">
    <!-- 左侧导航菜单 -->
    <aside class="sidebar">
      <div class="logo">校园互助后台</div>
      <!-- 菜单 -->
      <el-menu
        router
        default-active="/DataView"
        background-color="#0f172a"
        text-color="#cbd5e1"
        active-text-color="#ffffff"
      >
        <el-menu-item index="/DataView">首页</el-menu-item>
        <el-menu-item index="/users">用户管理</el-menu-item>
        <el-menu-item index="/verify">认证审核</el-menu-item>
        <el-menu-item index="/orders">订单管理</el-menu-item>
        <el-menu-item index="/appeals">纠纷仲裁</el-menu-item>
        <el-menu-item index="/withdraws">提现审核</el-menu-item>
        <el-menu-item index="/finance">财务统计</el-menu-item>
      </el-menu>
    </aside>

    <!-- 右侧正文 -->
    <router-view style="padding-left: 30px;"></router-view>
  </div>
</template>

<script setup>

</script>

<style scoped>
.dashboard-page {
  min-height: 100vh;
  display: flex;
  background: #f1f5f9;
}

.sidebar {
  width: 220px;
  background: #0f172a;
}

.logo {
  height: 64px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #ffffff;
  font-size: 20px;
  font-weight: 700;
}
</style>
