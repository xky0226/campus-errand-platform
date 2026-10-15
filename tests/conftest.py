"""校园帮订单接口 pytest 公共配置。

被测服务：Spring Boot 后端 http://localhost:8088，库 campus（账号 root，
口令走环境变量，不写在本文件里）。

收尾原则：用例自己建的订单，测完要清掉。这里用**物理删除**而不是接口的逻辑删除——
逻辑删除只是把 deleted 置 1，行还留在表里，总行数就跟基线对不上了。
"""
import os
import sys
from pathlib import Path

import pymysql
import pytest
import requests

BASE_URL = "http://localhost:8088"

# ===== 口令从哪来 =====
# 这个仓库是公开的，所以 MySQL 口令和管理员口令都不能写死在源码里。
# 值放 tests/.env（已被 .gitignore 忽略），一行一个 KEY=VALUE，
# 照 tests/.env.example 复制一份填上就行。
# 也可以不建文件，跑之前直接 export —— 两种都认；这里用 setdefault，
# 所以你已经设过的环境变量不会被文件盖掉。
_ENV_FILE = Path(__file__).parent / ".env"
if _ENV_FILE.exists():
    for _line in _ENV_FILE.read_text(encoding="utf-8").splitlines():
        _line = _line.strip()
        if not _line or _line.startswith("#") or "=" not in _line:
            continue
        _k, _v = _line.split("=", 1)
        os.environ.setdefault(_k.strip(), _v.strip())


def need_env(name: str, hint: str) -> str:
    """取一个口令类环境变量，缺了直接报错，不给兜底默认值。

    给默认值 = 又把口令写回仓库，那这一趟就白改了。
    """
    v = os.environ.get(name)
    if not v:
        raise RuntimeError(
            f"环境变量 {name} 没设 —— {hint}。"
            f"把 tests/.env.example 复制成 tests/.env 填上，"
            f"或者先 export {name}=... 再跑 pytest。"
        )
    return v

# 库里的底稿数据：94 行，其中 74 条未删除。测试不得改动这些数据。
# 74 = 原始 72 条 + 手工留的测试单 id=168 + 2026-09-24 在小程序里实测发布的一单 id=169。
# 这是硬编码的基线：只要再往库里加单（比如演示时发布订单），这个数就要跟着改。
BASELINE_ALIVE = 74

# 管理员账号。注意：adminLogin 是按 real_name 查的（讲义实现），
# 所以这里填的是真实姓名，不是 student_id / open_id。
# 姓名不算秘密（能猜），口令算 —— 所以只有后者走环境变量。
ADMIN_NAME = "管理员"
ADMIN_PASSWORD = need_env("ADMIN_PASSWORD", "管理员账号的口令")

# 登录后拿到 token 就原地塞进这个 dict。
# test_orders_crud.py 用 `from conftest import AUTH_HEADERS` 拿到的是同一个对象，
# 所以那边的 _send() 能直接读到这里写进去的头 —— 9 个用例不用逐个改签名。
AUTH_HEADERS = {}

DB_CONFIG = {
    "host": "localhost",
    "port": 3306,
    "user": "root",
    "password": need_env("DB_PASSWORD", "本机 MySQL 的 root 口令"),
    "database": "campus",
    "charset": "utf8mb4",
    "autocommit": True,
}

# 新增订单的必填字段。
# orders 表里 order_no / publisher_id / task_type / reward /
# pickup_location / delivery_location / expected_time 是 NOT NULL 且无默认值，
# 其中 publisherId 接口层的 @Valid 没校验（AdminOrderRequest 上没加注解），
# 但库里不允许为空，所以必须自己传。
NEW_ORDER_PAYLOAD = {
    "publisherId": 1,
    "taskType": "pytest代取快递",
    "reward": 5.00,
    "pickupLocation": "pytest取货点",
    "pickupLatitude": "23.123456",
    "pickupLongitude": "113.123456",
    "deliveryLocation": "pytest送达点",
    "deliveryLatitude": "23.223456",
    "deliveryLongitude": "113.223456",
    "expectedTime": "2026-09-30T18:00:00",
    "description": "pytest 测试数据，跑完即删",
}


def db_connect():
    return pymysql.connect(**DB_CONFIG)


@pytest.fixture(scope="session")
def admin_token():
    """先登录取 token。

    拦截器挂的是 /**，除了白名单（/auth/admin-login 在内）其余接口都要
    Authorization，所以后面所有请求都得先有这个 token。
    session 级：整套测试只登录一次，各用例共用。
    """
    try:
        resp = requests.post(
            f"{BASE_URL}/auth/admin-login",
            json={"username": ADMIN_NAME, "password": ADMIN_PASSWORD},
            timeout=5,
        )
    except requests.exceptions.ConnectionError:
        pytest.exit(f"后端没起来：{BASE_URL} 连不上。先启动 Spring Boot 再跑测试。")

    if resp.status_code != 200:
        pytest.exit(f"登录接口返回 {resp.status_code}：{resp.text[:200]}")

    body = resp.json()
    if body.get("code") != 200:
        pytest.exit(
            f"管理员登录失败（username={ADMIN_NAME}）：{body.get('message')}\n"
            f"检查 users 表里 real_name='{ADMIN_NAME}'、status<>0、密码是 {ADMIN_PASSWORD}"
        )

    return body["data"]["token"]


@pytest.fixture(scope="session", autouse=True)
def auth_headers(admin_token):
    """把 token 灌进模块级 AUTH_HEADERS，供各用例的请求带上。"""
    AUTH_HEADERS.update({"Authorization": f"Bearer {admin_token}"})
    return AUTH_HEADERS


@pytest.fixture(scope="session", autouse=True)
def require_service(auth_headers):
    """开跑前先确认后端和 MySQL 都起来了。

    不然 6 个接口全是 Connection refused，分不清是代码问题还是环境没开。

    依赖顺序有讲究：必须先过 auth_headers 拿到 token 再探活。
    拦截器挂了 /**，不带 token 打 /admin/orders 会返回 401，
    那样就分不清是「服务没起来」还是「没带 token」了。
    """
    try:
        resp = requests.get(f"{BASE_URL}/admin/orders", headers=auth_headers, timeout=5)
    except requests.exceptions.ConnectionError:
        pytest.exit(f"后端没起来：{BASE_URL} 连不上。先启动 Spring Boot 再跑测试。")

    if resp.status_code != 200:
        pytest.exit(f"后端起来了，但 /admin/orders 返回 {resp.status_code}，先排查。")


@pytest.fixture(scope="session", autouse=True)
def allure_environment(request):
    """往 Allure 结果目录写 environment.properties，报告首页会显示测试环境。

    注意：Allure 用 Java 的 Properties 读这个文件，中文容易乱码，所以键值都用 ASCII。
    """
    try:
        results_dir = request.config.getoption("--alluredir")
    except (ValueError, AttributeError):
        return
    if not results_dir:
        return

    results_dir = Path(results_dir)
    results_dir.mkdir(parents=True, exist_ok=True)
    (results_dir / "environment.properties").write_text(
        "\n".join([
            f"Backend={BASE_URL}",
            "Database=campus@localhost:3306",
            f"Baseline.Orders.Alive={BASELINE_ALIVE}",
            f"Python={sys.version.split()[0]}",
            f"pytest={pytest.__version__}",
        ]),
        encoding="utf-8",
    )


@pytest.fixture
def conn():
    """直连数据库，用于校验落库结果和测试数据收尾。"""
    connection = db_connect()
    yield connection
    connection.close()


@pytest.fixture
def order_factory(conn):
    """通过接口新建订单，测试结束后物理删除，保证库回到基线。

    用法：
        order = order_factory()                  # 用默认参数建一条
        order = order_factory(reward=9.99)       # 覆盖某个字段
    """
    created_ids = []

    def _make(**overrides):
        payload = {**NEW_ORDER_PAYLOAD, **overrides}
        resp = requests.post(
            f"{BASE_URL}/admin/orders/create",
            json=payload,
            headers=AUTH_HEADERS,
            timeout=10,
        )
        assert resp.status_code == 200, resp.text
        body = resp.json()
        assert body["code"] == 200, body
        created_ids.append(body["data"]["id"])
        return body["data"]

    yield _make

    # 收尾：真删，测试数据不留在库里。
    with conn.cursor() as cursor:
        for order_id in created_ids:
            cursor.execute("DELETE FROM orders WHERE id = %s", (order_id,))
