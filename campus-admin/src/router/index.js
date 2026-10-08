import { createRouter, createWebHistory } from 'vue-router'

const routes = [
    {
        path: '/login',
        name: 'Login',
        component: () => import('@/views/Login.vue')
    },
    {
        path: '/',
        name: 'Dashboard',
        component: () => import('@/views/Dashboard.vue'),
        // 讲义没写这一行：Dashboard.vue 在 3-3 里被改成布局壳（侧边栏 + <router-view>），
        // 直接访问 / 时没有子路由匹配，正文区是空的。
        // 讲义自己的菜单把「首页」指向 /DataView，所以在这里重定向过去。
        redirect: '/DataView',
        children: [
            {
                path: '/DataView',
                name: 'DataView',
                component: () => import('@/views/DataView.vue')
            },
            {
                // 讲义的路由表里没有 /users，但它在自己的菜单里放了「用户管理」，
                // 点下去会停在原页面。页面照 Orders.vue 的写法自己补的。
                // 后端 GET /admin/users（列表）+ PUT /admin/users/{id}（编辑、禁用启用）。
                path: '/users',
                name: 'UserManage',
                component: () => import('@/views/UserManage.vue')
            },
            {
                path: '/orders',
                name: 'Orders',
                component: () => import('@/views/Orders.vue')
            }
        ]
    }
]

const router = createRouter({
    history: createWebHistory(),
    routes
})

router.beforeEach((to, from, next) => {
    const token = localStorage.getItem('admin_token')
    console.log("token==" + token);
    //没有登录认证、则跳转登录页面
    if (to.path !== '/login' && !token) {
        next('/login')
        return
    }
    //有登录认证、则跳转/首页
    if (to.path === '/login' && token) {
        next('/')
        return
    }

    next()
})

export default router
