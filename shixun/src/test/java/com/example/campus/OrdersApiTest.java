package com.example.campus;

import com.example.campus.dto.AdminOrderRequest;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

/**
 * 订单模块 6 个接口走一遍，把每次返回的内容打到控制台。
 *
 * <p>和 tests/ 那套 pytest 的区别：这里是 Java 测试类，在 IDEA 里右键直接跑，
 * 不用先手动启动后端。
 *
 * <p>两个关键点：
 * <ul>
 *   <li>{@code @SpringBootTest} + {@code @AutoConfigureMockMvc}：把整个 Spring 容器起起来，
 *       用 MockMvc 发真实的 HTTP 请求（走 URL 路由、@Valid 校验、Result 封装），
 *       只是不占用 8088 端口。所以打印出来的就是接口真实返回的 JSON。</li>
 *   <li>{@code @Transactional}：整个用例在一个事务里跑，跑完自动回滚。
 *       中间新建/修改/删除的订单都不会留在库里，底稿 92 行 / 72 条未删除保持原样。
 *       （代价：自增计数器会往前走，下次新建订单的 id 会比 114 大，这是 InnoDB 的正常行为。）</li>
 * </ul>
 *
 * @author coder
 * @since 2026-09-21
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class OrdersApiTest {

    @Autowired
    MockMvc mockMvc;

    @Autowired
    ObjectMapper objectMapper;

    /** 登录拿到的 token，第一次用到时去换，之后复用。 */
    private String cachedAdminToken;

    /** 管理员账号：adminLogin 按 real_name 查，所以填真实姓名，不是学号。 */
    private static final String ADMIN_NAME = "管理员";

    /**
     * 从环境变量取一个口令，没设就抛错 —— **不给兜底默认值**。
     *
     * <p>这个仓库是公开的，所以管理员口令不能写死在源码里（和 tests/conftest.py
     * 那边同一个理由、同一个变量名）。给默认值等于又把口令写回仓库，那这一趟就白改了。
     *
     * <p>怎么设：
     * <ul>
     *   <li>IDEA：Run/Debug Configurations → Environment variables 里加 ADMIN_PASSWORD</li>
     *   <li>命令行：{@code set ADMIN_PASSWORD=... && mvn test}</li>
     * </ul>
     */
    private static String requireEnv(String name) {
        String v = System.getenv(name);
        if (v == null || v.isEmpty()) {
            throw new IllegalStateException(
                    "环境变量 " + name + " 没设 —— 管理员登录口令。"
                            + "IDEA 里加进 Run Configuration 的环境变量，"
                            + "或命令行先 set " + name + "=... 再跑 mvn test。");
        }
        return v;
    }

    /**
     * 管理员登录取 token。
     *
     * <p>JwtInterceptor 挂的是 /**，除白名单（/auth/admin-login 在内）外所有接口
     * 都要带 Authorization。MockMvc 走的是同一套 HandlerMapping，拦截器一样生效，
     * 不能指望它豁免 —— 不带 token 的话下面 6 个接口全返回 401 的 JSON。
     *
     * <p>请求体用 Map 拼、走 objectMapper.writeValueAsBytes() 编码成 UTF-8 字节，
     * 而不是手写 JSON 字符串 —— 用户名是中文，一旦哪里编码对不上就会变成乱码
     * （用 curl 试的时候被这个坑过一次，后端直接报 Invalid UTF-8 start byte）。
     */
    private String adminToken() throws Exception {
        if (cachedAdminToken == null) {
            byte[] loginBody = objectMapper.writeValueAsBytes(Map.of(
                    "username", ADMIN_NAME,
                    "password", requireEnv("ADMIN_PASSWORD")));

            MvcResult result = mockMvc.perform(post("/auth/admin-login")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(loginBody)).andReturn();

            JsonNode loginResp = objectMapper.readTree(
                    result.getResponse().getContentAsString(StandardCharsets.UTF_8));
            assertEquals(200, loginResp.path("code").asInt(),
                    "管理员登录应该返回 code=200，实际：" + loginResp.path("message").asText());

            cachedAdminToken = loginResp.path("data").path("token").asText();
            System.out.println(">>> 管理员登录成功，token 长度 = " + cachedAdminToken.length());
        }
        return cachedAdminToken;
    }

    /**
     * 新增订单的必填字段。
     * orders 表里 order_no / publisher_id / task_type / reward / pickup_location /
     * delivery_location / expected_time 是 NOT NULL 且无默认值，所以必须传全。
     */
    private AdminOrderRequest buildRequest() {
        AdminOrderRequest request = new AdminOrderRequest();
        request.setPublisherId(1L);
        request.setTaskType("测试类代取快递");
        request.setReward(new BigDecimal("5.00"));
        request.setPickupLocation("测试类取货点");
        request.setPickupLatitude("23.123456");
        request.setPickupLongitude("113.123456");
        request.setDeliveryLocation("测试类送达点");
        request.setDeliveryLatitude("23.223456");
        request.setDeliveryLongitude("113.223456");
        request.setExpectedTime(LocalDateTime.of(2026, 9, 30, 18, 0, 0));
        request.setDescription("测试类数据，跑完自动回滚");
        return request;
    }

    @Test
    @DisplayName("订单模块 6 个接口依次调用并打印返回内容")
    void 走一遍所有接口() throws Exception {
        // ================= 1. 新增订单  POST /admin/orders/create =================
        banner("1. 新增订单  POST /admin/orders/create");
        String createBody = call(post("/admin/orders/create")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(buildRequest())));
        System.out.println(createBody);

        JsonNode created = objectMapper.readTree(createBody);
        assertEquals(200, created.path("code").asInt(), "新增接口应该返回 code=200");
        long orderId = created.path("data").path("id").asLong();
        assertTrue(orderId > 0, "新增后应该回填自增主键 id");
        System.out.println("\n>>> 新建订单 id = " + orderId);

        // MP 的 insert 只回填自增主键，不会把数据库默认值回读。
        // 所以上面响应里的 status / paymentStatus / createdAt 是 null，
        // 但库里实际落的是 0 / 0 / 当前时间。想看真实值要靠下面的详情接口。
        System.out.println(">>> 注意：新增响应里 status / paymentStatus / createdAt 是 null，"
                + "库里落的是默认值，要回查详情才看得到");

        // ================= 2. 查询详情  GET /admin/orders/{orderId} =================
        banner("2. 查询订单详情  GET /admin/orders/" + orderId);
        System.out.println(call(get("/admin/orders/" + orderId)));

        // ================= 3. 查询列表  GET /admin/orders =================
        banner("3. 查询订单列表  GET /admin/orders");
        String listBody = call(get("/admin/orders"));
        JsonNode listData = objectMapper.readTree(listBody).path("data");
        // 底稿 72 条未删除，全打出来太长，只显示总数 + 前 3 条样本
        System.out.println("订单总数：" + listData.size() + " 条（这里只展示前 3 条，全量太长）");
        int shown = Math.min(3, listData.size());
        for (int i = 0; i < shown; i++) {
            System.out.println("\n--- 第 " + (i + 1) + " 条 ---");
            System.out.println(objectMapper.writerWithDefaultPrettyPrinter()
                    .writeValueAsString(listData.get(i)));
        }

        // ================= 4. 编辑订单  POST /admin/orders/{orderId}/update =================
        banner("4. 编辑订单  POST /admin/orders/" + orderId + "/update");
        // 只有 status 0待支付 / 1待接单 能编辑。刚建的订单是 0，所以这里能改。
        AdminOrderRequest updateReq = buildRequest();
        updateReq.setTaskType("测试类代取快递（已改）");
        updateReq.setReward(new BigDecimal("8.88"));
        System.out.println(call(post("/admin/orders/" + orderId + "/update")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updateReq))));
        System.out.println("\n>>> 改完回查一次，确认字段真的写进去了：");
        System.out.println(call(get("/admin/orders/" + orderId)));

        // ================= 5. 强制取消  POST /admin/orders/{orderId}/force-cancel =================
        banner("5. 强制取消订单  POST /admin/orders/" + orderId + "/force-cancel");
        // 只有 0/1/2/3/6 能强取消（4已完成、5已取消不行）。当前还是 0，所以可以取消。
        System.out.println(call(post("/admin/orders/" + orderId + "/force-cancel")));
        System.out.println("\n>>> 取消后 status 应该变成 5（已取消）：");
        System.out.println(call(get("/admin/orders/" + orderId)));

        // ================= 6. 删除订单  DELETE /admin/orders/{orderId} =================
        banner("6. 删除订单【逻辑删除】  DELETE /admin/orders/" + orderId);
        System.out.println(call(delete("/admin/orders/" + orderId)));
        // @TableLogic 挂在 deleted 字段上，deleteById 会变成
        // UPDATE orders SET deleted=1 WHERE id=? AND deleted=0，行还在表里。
        System.out.println("\n>>> 逻辑删除，详情接口查不到了（data 为 null，不是报错）：");
        System.out.println(call(get("/admin/orders/" + orderId)));

        // ================= 收尾 =================
        banner("全部走完");
        System.out.println("整个用例在 @Transactional 里跑，上面所有改动跑完自动回滚，库里不留痕。");
    }

    /**
     * 发请求，返回格式化后的 JSON 字符串。
     *
     * <p>统一在这里挂上 Authorization，就不用逐个请求去加了。
     */
    private String call(MockHttpServletRequestBuilder request) throws Exception {
        MvcResult result = mockMvc.perform(
                request.header("Authorization", "Bearer " + adminToken())).andReturn();
        String body = result.getResponse().getContentAsString(StandardCharsets.UTF_8);
        // 重新缩进一下，控制台看着清楚
        return objectMapper.writerWithDefaultPrettyPrinter()
                .writeValueAsString(objectMapper.readTree(body));
    }

    /** 打印一个分节标题，方便在控制台里找。 */
    private void banner(String title) {
        System.out.println("\n\n=============== " + title + " ===============");
    }
}
