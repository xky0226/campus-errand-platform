"""校园帮订单聊天接口测试（移动端聊天界面 + 管理端仲裁看聊天记录）。

被测接口：
    GET  /messages?orderId=              拉某订单的聊天记录（顺手把发给我的标已读）
    POST /messages                       发一条消息
    GET  /admin/appeals/{id}/messages     管理端只读查看某条申诉对应订单的聊天记录

和 test_wallet.py 的收尾原则一致：用例自己造的数据测完物理删除。

聊天这份额外要**还原 is_read** —— 拉取接口有副作用（把发给我的未读置成 1），
这不是加行删行能解决的，得把原有 7 条种子消息的 is_read 改回原值，
否则跑一轮测试库里就多出一堆「已读」，报告截图和种子数据对不上。

用到的账号都来自 campus.sql 的种子数据，密码统一是 123456：
    id=2  张同学         2021001001  订单 9 / 11 的发布者
    id=6  快递小哥小陈    2020002001  订单 9 的跑腿员
「陌生人」则是一次性注册的，测完物理删除，users 表回到 73 行。
"""
import json
import time

import allure
import pytest
import requests

from conftest import AUTH_HEADERS, BASE_URL, db_connect

FEATURE = "订单聊天模块"

# ---- 现成的种子素材。报告里移动端/管理端的聊天截图就是这两个订单 ----
CHAT_ORDER_ID = 9                    # 订单 9：发布者 2 / 跑腿员 6，状态 2 进行中
CHAT_ORDER_NO = "CH20240317140001"
SEED_MESSAGE_COUNT = 4               # campus.sql 里订单 9 有 4 条消息（id 1~4）

APPEAL_ID = 1                        # 申诉 1 挂在订单 11 上（发布者 2 / 跑腿员 9）
APPEAL_ORDER_ID = 11
APPEAL_SEED_MESSAGE_COUNT = 3        # 订单 11 有 3 条消息（id 5~7）

# 消息内容上限，和 MessagesServiceImpl.MAX_CONTENT_LENGTH 对齐
MAX_CONTENT_LENGTH = 500

PUBLISHER = ("2021001001", "123456")
RUNNER = ("2020002001", "123456")

# 种子消息里 is_read 的原始值，收尾时照这个改回去（见文件头说明）
SEED_IS_READ = {1: 1, 2: 0, 3: 1, 4: 0, 5: 1, 6: 1, 7: 0}


# ============================ 工具 ============================

def _login(student_id, password):
    """走移动端登录换 token。用户名是**学号**不是昵称（AuthController.login 按 student_id 查）"""
    resp = requests.post(
        f"{BASE_URL}/auth/login",
        json={"username": student_id, "password": password},
        timeout=10,
    )
    assert resp.status_code == 200, resp.text
    body = resp.json()
    assert body["code"] == 200, f"{student_id} 登录失败：{body.get('message')}"
    return body["data"]["token"]


def _headers(token):
    """token 可以是登录返回的字符串，也可以直接传 {'Authorization': ...} 这样的整份请求头。

    管理员的头来自 conftest 的 AUTH_HEADERS（是个 dict），不能当 token 字符串拼 ——
    真拼了就发出 `Bearer {'Authorization': 'Bearer eyJ...'}`，后端只会回「登录已过期」。
    """
    if not token:
        return {}
    if isinstance(token, dict):
        return dict(token)
    return {"Authorization": f"Bearer {token}"}


def _send(method, path, token=None, **kwargs):
    url = f"{BASE_URL}{path}"
    resp = requests.request(method, url, headers=_headers(token), timeout=10, **kwargs)

    body = kwargs.get("json")
    allure.attach(
        f"{method} {url}\n"
        f"身份: {'带 token' if token else '不带 token'}\n"
        f"请求体: {json.dumps(body, ensure_ascii=False) if body else '无'}\n\n"
        f"响应 {resp.status_code}:\n{resp.text[:2000]}",
        name=f"{method} {path}",
        attachment_type=allure.attachment_type.TEXT,
    )
    return resp


def _get(path, token=None):
    return _send("GET", path, token)


def _post(path, token=None, **kwargs):
    return _send("POST", path, token, **kwargs)


def _data(resp):
    """断言接口通，并返回 data"""
    assert resp.status_code == 200, f"HTTP {resp.status_code}: {resp.text[:300]}"
    body = resp.json()
    assert body["code"] == 200, f"业务失败：{body.get('message')}"
    return body["data"]


def _reject(resp, keyword):
    """断言被业务拒绝。业务异常统一是 code=500 + HTTP 200（见 ControllerExceptionHandler）"""
    assert resp.status_code == 200, f"期望业务拒绝，实际 HTTP {resp.status_code}"
    body = resp.json()
    assert body["code"] == 500, f"期望被拒，实际通过了：{body}"
    assert keyword in (body.get("message") or ""), f"提示语不含「{keyword}」：{body.get('message')}"
    return body


# ============================ 夹具 ============================

@pytest.fixture(scope="module")
def pub_token():
    """订单发布者（张同学 id=2）的 token"""
    return _login(*PUBLISHER)


@pytest.fixture(scope="module")
def run_token():
    """订单跑腿员（快递小哥小陈 id=6）的 token"""
    return _login(*RUNNER)


@pytest.fixture(scope="module")
def bystander():
    """注册一个和订单毫无关系的用户，用来验证「非当事人不能看聊天记录」。

    yield (token, user_id)。收尾物理删除 —— 这条是测试自己造的账号，
    留着会让 users 表和基线对不上（也污染用户管理页的演示数据）。
    """
    student_id = f"T{int(time.time())}"
    resp = requests.post(
        f"{BASE_URL}/auth/register",
        json={
            "nickname": "闲聊路人",
            "username": student_id,
            "password": "123456",
            "phone": "13900000000",
        },
        timeout=10,
    )
    assert resp.status_code == 200, resp.text
    body = resp.json()
    assert body["code"] == 200, f"注册一次性用户失败：{body.get('message')}"
    # LoginResponse 有 userId / user.id / id 三个位置，按 LoginResponse 的定义取 userId 最稳
    user_id = body["data"].get("userId") or body["data"].get("id") \
        or (body["data"].get("user") or {}).get("id")
    assert user_id, f"注册返回里没找到用户 id：{body}"
    token = body["data"]["token"]

    allure.attach(
        f"一次性用户 users.id={user_id}（学号 {student_id}）\n"
        f"它和订单 {CHAT_ORDER_ID} / {APPEAL_ORDER_ID} 都没有关系",
        name="陌生人账号",
        attachment_type=allure.attachment_type.TEXT,
    )

    yield token, user_id

    conn = db_connect()
    try:
        with conn.cursor() as cursor:
            cursor.execute("DELETE FROM users WHERE id = %s", (user_id,))
    finally:
        conn.close()


@pytest.fixture(scope="module", autouse=True)
def messages_baseline():
    """记录种子消息的 is_read 和 max(id)，收尾时还原/清理。

    autouse + module 级：整个文件跑完自动复原，用例里不用管。
    """
    conn = db_connect()
    baseline_max_id = 0
    try:
        with conn.cursor() as cursor:
            cursor.execute("SELECT COALESCE(MAX(id), 0) FROM messages")
            baseline_max_id = cursor.fetchone()[0]
            cursor.execute("SELECT id, is_read FROM messages WHERE id <= 7 ORDER BY id")
            before = {row[0]: row[1] for row in cursor.fetchall()}

        allure.attach(
            f"种子消息 max(id)={baseline_max_id}\n"
            f"测试前 is_read={before}\n"
            f"收尾会清掉 id > {baseline_max_id} 的测试消息，并把 is_read 改回 {SEED_IS_READ}",
            name="聊天测试基线",
            attachment_type=allure.attachment_type.TEXT,
        )

        yield baseline_max_id
    finally:
        with conn.cursor() as cursor:
            cursor.execute("DELETE FROM messages WHERE id > %s", (baseline_max_id,))
            for msg_id, is_read in SEED_IS_READ.items():
                cursor.execute(
                    "UPDATE messages SET is_read = %s WHERE id = %s", (is_read, msg_id)
                )
        conn.close()


# ============================ 查聊天记录 ============================

@allure.feature(FEATURE)
@allure.story("查看聊天记录")
@allure.title(f"发布者拉订单 {CHAT_ORDER_ID} 的聊天记录：按 id 升序返回种子消息")
@allure.severity(allure.severity_level.CRITICAL)
def test_publisher_lists_seed_messages(pub_token):
    with allure.step(f"GET /messages?orderId={CHAT_ORDER_ID}"):
        data = _data(_get(f"/messages?orderId={CHAT_ORDER_ID}", pub_token))

    with allure.step(f"一共 {SEED_MESSAGE_COUNT} 条，和 campus.sql 的种子数据一致"):
        assert len(data) == SEED_MESSAGE_COUNT, data

    with allure.step("顺序按 id 升序（种子消息 created_at 全是同一秒，按时间排会乱）"):
        ids = [m["id"] for m in data]
        assert ids == sorted(ids) == list(range(1, SEED_MESSAGE_COUNT + 1)), ids

    with allure.step("每条都带发送者昵称，前端不用再查一遍用户表"):
        for m in data:
            assert m["orderId"] == CHAT_ORDER_ID
            assert m["senderName"], m
            assert m["senderId"] in (2, 6), m
            assert m["content"] and m["type"] == "text", m

    with allure.step("接收方是订单双方里的另一个人"):
        for m in data:
            assert m["receiverId"] in (2, 6) and m["receiverId"] != m["senderId"], m


@allure.feature(FEATURE)
@allure.story("查看聊天记录")
@allure.title("跑腿员也能拉同一个订单的聊天记录，看到的是同一份")
@allure.severity(allure.severity_level.CRITICAL)
def test_runner_sees_same_conversation(pub_token, run_token):
    with allure.step("发布者视角"):
        by_publisher = _data(_get(f"/messages?orderId={CHAT_ORDER_ID}", pub_token))

    with allure.step("跑腿员视角"):
        by_runner = _data(_get(f"/messages?orderId={CHAT_ORDER_ID}", run_token))

    with allure.step("两边条数和内容完全一致（只是各自的已读标记不同）"):
        assert len(by_runner) == len(by_publisher) == SEED_MESSAGE_COUNT
        assert [(m["id"], m["content"]) for m in by_runner] == \
               [(m["id"], m["content"]) for m in by_publisher]


@allure.feature(FEATURE)
@allure.story("查看聊天记录")
@allure.title("非当事人拉聊天记录被拒：不能围观别人的订单")
@allure.severity(allure.severity_level.CRITICAL)
def test_bystander_cannot_list(bystander):
    token, user_id = bystander
    with allure.step(f"陌生人（id={user_id}）GET /messages?orderId={CHAT_ORDER_ID}"):
        resp = _get(f"/messages?orderId={CHAT_ORDER_ID}", token)

    with allure.step("被拒：无权查看该订单的聊天记录"):
        _reject(resp, "无权查看该订单的聊天记录")


@allure.feature(FEATURE)
@allure.story("查看聊天记录")
@allure.title("订单不存在：返回「订单不存在」而不是空列表")
@allure.severity(allure.severity_level.NORMAL)
def test_list_unknown_order_rejected(pub_token):
    with allure.step("GET /messages?orderId=999999"):
        resp = _get("/messages?orderId=999999", pub_token)

    with allure.step("被拒：订单不存在"):
        _reject(resp, "订单不存在")


@allure.feature(FEATURE)
@allure.story("鉴权")
@allure.title("不带 token 打聊天接口返回 401（拦截器挂的是 /**）")
@allure.severity(allure.severity_level.CRITICAL)
def test_no_token_is_unauthorized():
    with allure.step(f"不带 token GET /messages?orderId={CHAT_ORDER_ID}"):
        resp = _get(f"/messages?orderId={CHAT_ORDER_ID}")

    with allure.step("401"):
        assert resp.status_code == 401, f"期望 401，实际 {resp.status_code}: {resp.text[:200]}"


# ============================ 发消息 ============================

@allure.feature(FEATURE)
@allure.story("发送消息")
@allure.title("发布者发消息：200，落库的 receiver 是跑腿员，初始未读")
@allure.severity(allure.severity_level.CRITICAL)
def test_send_message_persists(pub_token, conn):
    content = "pytest：我到楼下了，麻烦下来一下"

    with allure.step("POST /messages"):
        data = _data(_post(
            "/messages", pub_token,
            json={"orderId": CHAT_ORDER_ID, "content": content},
        ))

    with allure.step("接口返回的就是刚发的那条，发送者是我，接收方是跑腿员（后端推的，我没传）"):
        assert data["orderId"] == CHAT_ORDER_ID
        assert data["senderId"] == 2 and data["senderName"] == "张同学"
        assert data["receiverId"] == 6, "接收方应该由后端按订单双方反推"
        assert data["content"] == content
        assert data["type"] == "text"
        assert data["isRead"] == 0, "刚发出去对对方来说是未读"
        assert data["createdAt"], "createdAt 由数据库默认值生成，接口应回查后带出来"

    with allure.step("直连数据库确认真的落了库"):
        with conn.cursor() as cursor:
            cursor.execute(
                "SELECT order_id, sender_id, receiver_id, content, type, is_read "
                "FROM messages WHERE id = %s", (data["id"],)
            )
            row = cursor.fetchone()
        assert row == (CHAT_ORDER_ID, 2, 6, content, "text", 0), row

    with allure.step("再拉一次，这条已经出现在列表末尾"):
        listed = _data(_get(f"/messages?orderId={CHAT_ORDER_ID}", pub_token))
        assert listed[-1]["id"] == data["id"]


@allure.feature(FEATURE)
@allure.story("发送消息")
@allure.title("拉取会把「发给我的」未读置为已读，我自己发的不动")
@allure.severity(allure.severity_level.CRITICAL)
def test_listing_marks_incoming_as_read(pub_token, run_token, conn):
    content = "pytest：已读标记验证"

    with allure.step("发布者给跑腿员发一条（对跑腿员来说是未读，库里是 0）"):
        sent = _data(_post(
            "/messages", pub_token,
            json={"orderId": CHAT_ORDER_ID, "content": content},
        ))
        assert sent["isRead"] == 0
        with conn.cursor() as cursor:
            cursor.execute("SELECT is_read FROM messages WHERE id = %s", (sent["id"],))
            assert cursor.fetchone()[0] == 0

    with allure.step("发布者自己拉一次列表：这条是发给对方的，不该被我自己标成已读"):
        listed = _data(_get(f"/messages?orderId={CHAT_ORDER_ID}", pub_token))
        to_runner = [m for m in listed if m["id"] == sent["id"]]
        assert to_runner and to_runner[0]["isRead"] == 0, "已读是接收方的状态，发送方拉列表不该动它"
        with conn.cursor() as cursor:
            cursor.execute("SELECT is_read FROM messages WHERE id = %s", (sent["id"],))
            assert cursor.fetchone()[0] == 0

    with allure.step("跑腿员拉一次列表：这条是发给他的，应被标成已读"):
        listed = _data(_get(f"/messages?orderId={CHAT_ORDER_ID}", run_token))
        mine = [m for m in listed if m["id"] == sent["id"]]
        assert mine and mine[0]["isRead"] == 1, "拉取时应顺手把发给我的标成已读"

    with allure.step("库里也真的从 0 变成 1 了"):
        with conn.cursor() as cursor:
            cursor.execute("SELECT is_read FROM messages WHERE id = %s", (sent["id"],))
            assert cursor.fetchone()[0] == 1


@allure.feature(FEATURE)
@allure.story("发送消息")
@allure.title("非当事人发消息被拒")
@allure.severity(allure.severity_level.CRITICAL)
def test_bystander_cannot_send(bystander):
    token, user_id = bystander
    with allure.step(f"陌生人（id={user_id}）往订单 {CHAT_ORDER_ID} 发消息"):
        resp = _post(
            "/messages", token,
            json={"orderId": CHAT_ORDER_ID, "content": "pytest：我进来插一句"},
        )

    with allure.step("被拒"):
        _reject(resp, "无权查看该订单的聊天记录")


@allure.feature(FEATURE)
@allure.story("发送消息")
@allure.title("空白内容和超长内容都被拒")
@allure.severity(allure.severity_level.NORMAL)
@pytest.mark.parametrize("content, keyword", [
    ("     ", "消息内容不能为空"),
    ("", "消息内容不能为空"),
    ("啊" * (MAX_CONTENT_LENGTH + 1), "不能超过"),
])
def test_send_rejects_bad_content(pub_token, content, keyword):
    with allure.step(f"发送长度={len(content)} 的内容"):
        resp = _post(
            "/messages", pub_token,
            json={"orderId": CHAT_ORDER_ID, "content": content},
        )

    with allure.step(f"被拒：{keyword}"):
        _reject(resp, keyword)


@allure.feature(FEATURE)
@allure.story("发送消息")
@allure.title("内容前后空格会被去掉再存")
@allure.severity(allure.severity_level.MINOR)
def test_send_trims_content(pub_token):
    with allure.step("发送「  前后都有空格  」"):
        data = _data(_post(
            "/messages", pub_token,
            json={"orderId": CHAT_ORDER_ID, "content": "  前后都有空格  "},
        ))

    with allure.step("存下来的是去掉首尾空格的版本"):
        assert data["content"] == "前后都有空格", repr(data["content"])


@allure.feature(FEATURE)
@allure.story("发送消息")
@allure.title("订单还没人接单时不能发：没有跑腿员就没有对话对象")
@allure.severity(allure.severity_level.CRITICAL)
def test_cannot_send_when_no_runner(order_factory):
    with allure.step("用工厂新建一个订单（默认 publisherId=1，runner_id 为空）"):
        order = order_factory()
        assert order.get("runnerId") is None, order

    with allure.step("发布者（管理员账号 id=1 就是这条单的发布者）尝试发消息"):
        resp = _post(
            "/messages", AUTH_HEADERS,
            json={"orderId": order["id"], "content": "pytest：还没人接单就想聊"},
        )

    with allure.step("被拒：还没有跑腿员接单"):
        _reject(resp, "还没有跑腿员接单")


# ============================ 管理端仲裁看聊天记录 ============================

@allure.feature(FEATURE)
@allure.story("管理端查看聊天记录")
@allure.title(f"管理员拉申诉 {APPEAL_ID} 对应订单的聊天记录，且不改动已读状态")
@allure.severity(allure.severity_level.CRITICAL)
def test_admin_reads_appeal_messages(conn):
    with allure.step("先记下订单 11 那几条消息的已读状态"):
        with conn.cursor() as cursor:
            cursor.execute(
                "SELECT id, is_read FROM messages WHERE order_id = %s ORDER BY id",
                (APPEAL_ORDER_ID,),
            )
            before = cursor.fetchall()
        assert len(before) == APPEAL_SEED_MESSAGE_COUNT, before

    with allure.step(f"GET /admin/appeals/{APPEAL_ID}/messages"):
        data = _data(_get(f"/admin/appeals/{APPEAL_ID}/messages", AUTH_HEADERS))

    with allure.step(f"顺着申诉拿到订单 {APPEAL_ORDER_ID} 的 {APPEAL_SEED_MESSAGE_COUNT} 条消息"):
        assert len(data) == APPEAL_SEED_MESSAGE_COUNT, data
        assert {m["orderId"] for m in data} == {APPEAL_ORDER_ID}
        assert {m["senderId"] for m in data} == {2, 9}, "订单 11 的双方是发布者 2 和跑腿员 9"

    with allure.step("管理员是旁观者，看完不该把消息标成已读"):
        with conn.cursor() as cursor:
            cursor.execute(
                "SELECT id, is_read FROM messages WHERE order_id = %s ORDER BY id",
                (APPEAL_ORDER_ID,),
            )
            after = cursor.fetchall()
        assert after == before, f"管理端只读，已读状态不该变：{before} -> {after}"


@allure.feature(FEATURE)
@allure.story("管理端查看聊天记录")
@allure.title("普通用户打管理端聊天记录接口被拒：无管理员权限")
@allure.severity(allure.severity_level.CRITICAL)
def test_admin_endpoint_rejects_normal_user(pub_token):
    with allure.step(f"拿普通用户 token 打 /admin/appeals/{APPEAL_ID}/messages（这条单他本来还是当事人）"):
        resp = _get(f"/admin/appeals/{APPEAL_ID}/messages", pub_token)

    with allure.step("仍然被拒：管理端接口只看 role，不看你是不是订单当事人"):
        _reject(resp, "无管理员权限")


@allure.feature(FEATURE)
@allure.story("管理端查看聊天记录")
@allure.title("管理端聊天记录接口不带 token 返回 401")
@allure.severity(allure.severity_level.NORMAL)
def test_admin_endpoint_requires_token():
    with allure.step(f"不带 token GET /admin/appeals/{APPEAL_ID}/messages"):
        resp = _get(f"/admin/appeals/{APPEAL_ID}/messages")

    with allure.step("401"):
        assert resp.status_code == 401, f"期望 401，实际 {resp.status_code}"
