"""校园帮订单模块 CRUD 接口测试（正向主流程）。

对应后端 OrdersController 的 6 个接口：
    GET    /admin/orders                    查全部订单（不含已删除）
    GET    /admin/orders/{orderId}          查订单详情
    POST   /admin/orders/create             新增订单
    POST   /admin/orders/{orderId}/update   编辑订单
    POST   /admin/orders/{orderId}/force-cancel  强制取消订单
    DELETE /admin/orders/{orderId}          删除订单（逻辑删除）

用例按 @allure.story 分组，报告里会拼成「订单模块」下的接口树。
"""
import json
import re

import allure
import requests

from conftest import AUTH_HEADERS, BASE_URL, BASELINE_ALIVE, NEW_ORDER_PAYLOAD

FEATURE = "订单模块"


def _send(method, path, **kwargs):
    """发请求，并把请求和响应贴进 Allure 报告——用例挂了能直接在报告里看报文。

    拦截器挂了 /**，除白名单外都要 Authorization。token 由 conftest 的
    auth_headers 夹具登录后写进 AUTH_HEADERS，这里统一带上；
    用例自己传了 headers 就以用例的为准。
    """
    url = f"{BASE_URL}{path}"
    headers = dict(AUTH_HEADERS)
    headers.update(kwargs.pop("headers", {}))
    resp = requests.request(method, url, headers=headers, timeout=10, **kwargs)

    body = kwargs.get("json")
    allure.attach(
        f"{method} {url}\n"
        f"请求体: {json.dumps(body, ensure_ascii=False) if body else '无'}\n\n"
        f"响应 {resp.status_code}:\n{resp.text[:2000]}",
        name=f"{method} {path}",
        attachment_type=allure.attachment_type.TEXT,
    )
    return resp


def _get(path):
    return _send("GET", path)


def _post(path, **kwargs):
    return _send("POST", path, **kwargs)


def _delete(path):
    return _send("DELETE", path)


@allure.feature(FEATURE)
@allure.story("查询订单列表")
@allure.title("查全部订单：只返回未删除的，条数等于基线 72")
@allure.severity(allure.severity_level.CRITICAL)
def test_list_orders_returns_undeleted_orders():
    with allure.step("GET /admin/orders"):
        resp = _get("/admin/orders")
        assert resp.status_code == 200

    with allure.step("校验响应结构"):
        body = resp.json()
        assert body["code"] == 200
        assert body["message"] == "success"

    with allure.step(f"校验返回 {BASELINE_ALIVE} 条且 deleted 全为 0"):
        orders = body["data"]
        assert isinstance(orders, list)
        assert len(orders) == BASELINE_ALIVE
        assert all(order["deleted"] == 0 for order in orders)


@allure.feature(FEATURE)
@allure.story("查询订单详情")
@allure.title("查详情：按 orderId 取一条，字段与列表里那条一致")
@allure.severity(allure.severity_level.NORMAL)
def test_get_order_detail_matches_list():
    with allure.step("先从列表拿一条作为基准"):
        listed = _get("/admin/orders").json()["data"][0]

    with allure.step(f"GET /admin/orders/{listed['id']} 并比对字段"):
        resp = _get(f"/admin/orders/{listed['id']}")
        assert resp.status_code == 200

        body = resp.json()
        assert body["code"] == 200

        detail = body["data"]
        assert detail["id"] == listed["id"]
        assert detail["orderNo"] == listed["orderNo"]
        assert detail["taskType"] == listed["taskType"]
        assert detail["reward"] == listed["reward"]


@allure.feature(FEATURE)
@allure.story("新增订单")
@allure.title("新增订单：请求字段全部落库，id 由数据库自增回填")
@allure.severity(allure.severity_level.CRITICAL)
def test_create_order_fills_request_fields(order_factory):
    with allure.step("POST /admin/orders/create"):
        order = order_factory()

    with allure.step("校验 id 回填与请求字段"):
        assert isinstance(order["id"], int)
        assert order["id"] > 0
        assert order["publisherId"] == NEW_ORDER_PAYLOAD["publisherId"]
        assert order["taskType"] == NEW_ORDER_PAYLOAD["taskType"]
        assert order["reward"] == NEW_ORDER_PAYLOAD["reward"]
        assert order["pickupLocation"] == NEW_ORDER_PAYLOAD["pickupLocation"]
        assert order["deliveryLocation"] == NEW_ORDER_PAYLOAD["deliveryLocation"]
        assert order["expectedTime"] == NEW_ORDER_PAYLOAD["expectedTime"]
        assert order["description"] == NEW_ORDER_PAYLOAD["description"]


@allure.feature(FEATURE)
@allure.story("新增订单")
@allure.title("新增订单：service 没赋值的字段走数据库默认值")
@allure.severity(allure.severity_level.NORMAL)
def test_create_order_uses_db_defaults(order_factory):
    """createOrder 返回的是内存里那个实体，MP 的 insert 只回填自增主键，
    不会把数据库默认值读回来，所以新增接口的响应里 status 等字段是 null。
    要验默认值是否真的落了库，得重新查一次详情。
    """
    with allure.step("新增一条订单"):
        order = order_factory()

    with allure.step("确认新增响应里未赋值字段是 null（MP 不回读默认值）"):
        assert order["status"] is None
        assert order["paymentStatus"] is None
        assert order["createdAt"] is None

    with allure.step("重新查详情，确认数据库默认值已落库"):
        detail = _get(f"/admin/orders/{order['id']}").json()["data"]
        assert detail["status"] == 0          # 0 待支付
        assert detail["paymentStatus"] == 0   # 未支付
        assert detail["platformFee"] == 0.0
        assert detail["deleted"] == 0
        assert detail["runnerId"] is None
        assert detail["cancelledAt"] is None
        assert detail["createdAt"] is not None


@allure.feature(FEATURE)
@allure.story("新增订单")
@allure.title("新增订单：订单号按 CH+17 位生成，两条不重复")
@allure.severity(allure.severity_level.MINOR)
def test_create_order_generates_unique_order_no(order_factory):
    with allure.step("连续新增两条订单"):
        first = order_factory()
        second = order_factory()

    with allure.step("校验订单号格式与唯一性"):
        pattern = re.compile(r"^CH\d{17}$")
        assert pattern.match(first["orderNo"]), first["orderNo"]
        assert pattern.match(second["orderNo"]), second["orderNo"]
        assert first["orderNo"] != second["orderNo"]


@allure.feature(FEATURE)
@allure.story("编辑订单")
@allure.title("编辑订单：改动落库，id/orderNo/publisherId/status 不受影响")
@allure.severity(allure.severity_level.CRITICAL)
def test_update_order_changes_editable_fields(order_factory):
    with allure.step("新增一条订单（status=0，可编辑）"):
        order = order_factory()
        order_id = order["id"]

    with allure.step(f"POST /admin/orders/{order_id}/update"):
        payload = {
            **NEW_ORDER_PAYLOAD,
            "taskType": "pytest改后类型",
            "reward": 12.50,
            "pickupLocation": "pytest改后取货点",
        }
        resp = _post(f"/admin/orders/{order_id}/update", json=payload)
        assert resp.status_code == 200

        body = resp.json()
        assert body["code"] == 200
        assert body["data"] is True

    with allure.step("重新查详情，确认改动已落库、不该变的字段没变"):
        detail = _get(f"/admin/orders/{order_id}").json()["data"]
        assert detail["taskType"] == "pytest改后类型"
        assert detail["reward"] == 12.50
        assert detail["pickupLocation"] == "pytest改后取货点"
        # 不该被改动的字段
        assert detail["id"] == order_id
        assert detail["orderNo"] == order["orderNo"]
        assert detail["publisherId"] == order["publisherId"]
        assert detail["status"] == 0


@allure.feature(FEATURE)
@allure.story("强制取消订单")
@allure.title("强制取消订单：status 变 5，cancelledAt 落时间")
@allure.severity(allure.severity_level.CRITICAL)
def test_force_cancel_order_sets_status_5(order_factory):
    with allure.step("新增一条订单（status=0，可强制取消）"):
        order = order_factory()
        order_id = order["id"]

    with allure.step(f"POST /admin/orders/{order_id}/force-cancel"):
        resp = _post(f"/admin/orders/{order_id}/force-cancel")
        assert resp.status_code == 200

        body = resp.json()
        assert body["code"] == 200
        assert body["data"] is True

    with allure.step("重新查详情，确认 status=5 且 cancelledAt 有值"):
        detail = _get(f"/admin/orders/{order_id}").json()["data"]
        assert detail["status"] == 5
        assert detail["cancelledAt"] is not None


@allure.feature(FEATURE)
@allure.story("删除订单")
@allure.title("删除订单：是逻辑删除，行保留在库里，deleted 置 1")
@allure.severity(allure.severity_level.CRITICAL)
def test_delete_order_is_logical(order_factory, conn):
    with allure.step("新增一条订单"):
        order = order_factory()
        order_id = order["id"]

    with allure.step(f"DELETE /admin/orders/{order_id}"):
        resp = _delete(f"/admin/orders/{order_id}")
        assert resp.status_code == 200

        body = resp.json()
        assert body["code"] == 200
        assert body["data"] is True

    with allure.step("直连数据库确认行还在、deleted=1"):
        with conn.cursor() as cursor:
            cursor.execute("SELECT deleted FROM orders WHERE id = %s", (order_id,))
            row = cursor.fetchone()

        allure.attach(
            f"SELECT deleted FROM orders WHERE id = {order_id}\n-> {row}",
            name="数据库校验",
            attachment_type=allure.attachment_type.TEXT,
        )
        assert row is not None, "逻辑删除不该把行真删掉"
        assert row[0] == 1


@allure.feature(FEATURE)
@allure.story("删除订单")
@allure.title("删除后：列表不含该条，条数回到基线，详情查不到")
@allure.severity(allure.severity_level.NORMAL)
def test_deleted_order_is_invisible(order_factory):
    with allure.step("新增一条订单，确认列表暂时多一条"):
        order = order_factory()
        order_id = order["id"]
        assert len(_get("/admin/orders").json()["data"]) == BASELINE_ALIVE + 1

    with allure.step(f"DELETE /admin/orders/{order_id}"):
        _delete(f"/admin/orders/{order_id}")

    with allure.step("确认列表回到基线且不含该条"):
        listed_ids = [item["id"] for item in _get("/admin/orders").json()["data"]]
        assert len(listed_ids) == BASELINE_ALIVE
        assert order_id not in listed_ids

    with allure.step("确认详情查不到（返回 data=null）"):
        assert _get(f"/admin/orders/{order_id}").json()["data"] is None
