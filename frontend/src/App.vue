<script setup lang="ts">
import { computed, onMounted, ref } from 'vue';
import { Loading } from '@element-plus/icons-vue';
import { auth, hasRole, initAuth, login, logout } from './auth';

const loginUser = ref('');
const loginPass = ref('');
const logging = ref(false);
const error = ref('');

interface MenuItem {
  path: string;
  label: string;
  roles: string[];
}

const menu: MenuItem[] = [
  { path: '/', label: '监管驾驶舱', roles: ['ADMIN', 'REGULATOR'] },
  { path: '/standards', label: '食材标准库', roles: ['ADMIN', 'BUYER', 'REGULATOR'] },
  { path: '/suppliers', label: '供应商', roles: ['ADMIN', 'BUYER', 'REGULATOR'] },
  { path: '/plans', label: '采购计划', roles: ['ADMIN', 'BUYER', 'REGULATOR'] },
  { path: '/inquiries', label: '询价/竞价', roles: ['ADMIN', 'BUYER', 'REGULATOR', 'SUPPLIER'] },
  { path: '/orders', label: '采购订单', roles: ['ADMIN', 'BUYER', 'REGULATOR'] },
  { path: '/deliveries', label: '配送', roles: ['ADMIN', 'BUYER', 'REGULATOR'] },
  { path: '/inspections', label: '智能验收', roles: ['ADMIN', 'BUYER', 'REGULATOR'] },
  { path: '/inventory', label: '库存批次', roles: ['ADMIN', 'BUYER', 'REGULATOR'] },
  { path: '/settlements', label: '结算', roles: ['ADMIN', 'BUYER', 'REGULATOR'] },
];

const items = computed(() => menu.filter((m) => m.roles.some((r) => hasRole(r))));

const roleLabel = computed(() =>
  auth.authorities.map((a) => a.replace('ROLE_', '')).join(' / ')
);

async function doLogin() {
  error.value = '';
  logging.value = true;
  try {
    await login(loginUser.value.trim(), loginPass.value);
    location.reload();
  } catch (e: any) {
    error.value =
      e?.response?.status === 401
        ? '用户名或密码错误'
        : '无法连接服务，请确认后端已启动（docker compose up -d）';
  } finally {
    logging.value = false;
  }
}

function doLogout() {
  logout();
  location.reload();
}

onMounted(initAuth);
</script>

<template>
  <el-config-provider>
    <div v-if="!auth.checked" class="login">
      <el-icon class="is-loading" :size="28"><Loading /></el-icon>
    </div>

    <div v-else-if="!auth.username" class="login">
      <el-card class="login-card">
        <h2>食采监管</h2>
        <p>面向学校、医院及监管部门的采购协同平台</p>
        <el-input v-model="loginUser" placeholder="用户名" autocomplete="username" />
        <el-input
          v-model="loginPass"
          type="password"
          show-password
          placeholder="密码"
          autocomplete="current-password"
          @keyup.enter="doLogin"
        />
        <el-alert v-if="error" :title="error" type="error" :closable="false" show-icon />
        <el-button type="primary" :loading="logging" class="login-btn" @click="doLogin">登录</el-button>
      </el-card>
    </div>

    <el-container v-else>
      <el-aside width="220px">
        <h2 class="brand">食采监管</h2>
        <el-menu router>
          <el-menu-item v-for="x in items" :key="x.path" :index="x.path">{{ x.label }}</el-menu-item>
        </el-menu>
      </el-aside>
      <el-container>
        <el-header>
          <span>集中采购与食品安全追溯平台</span>
          <span class="who">当前用户：{{ auth.username }}（{{ roleLabel }}）</span>
          <el-button text type="primary" @click="doLogout">退出</el-button>
        </el-header>
        <el-main>
          <router-view />
        </el-main>
      </el-container>
    </el-container>
  </el-config-provider>
</template>

<style>
body {
  margin: 0;
  background: #f6f8fb;
  color: #24344d;
}
.brand {
  padding-left: 20px;
  color: #2f6fed;
}
.el-aside {
  min-height: 100vh;
  background: white;
  border-right: 1px solid #e6eaf0;
}
.el-header {
  background: white;
  line-height: 60px;
  border-bottom: 1px solid #e6eaf0;
  display: flex;
  align-items: center;
  gap: 16px;
}
.who {
  color: #8895a7;
  margin-left: auto;
}
.login {
  min-height: 100vh;
  display: grid;
  place-items: center;
}
.login-card {
  width: 380px;
}
.login-card h2 {
  padding: 0;
}
.login-card .el-input,
.login-card .el-alert,
.login-btn {
  margin-top: 12px;
  width: 100%;
}
</style>
