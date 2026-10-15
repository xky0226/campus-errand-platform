"""校园帮钱包模块接口测试（充值 / 提现 / 流水 / 订单支付打通）。

被测接口：
    GET  /wallet/balance                余额概览
    GET  /wallet/transactions           交易记录（可按类型筛选）
    POST /wallet/recharge               充值（模拟）
    POST /wallet/withdraw               申请提现
    GET  /admin/finance/withdrawals     管理端看待审核提现
    POST /admin/finance/withdrawals/{id}/audit   管理端审核提现
    POST /orders/{id}/pay               订单支付（走钱包扣款 + 冻结托管）
    GET  /orders/{id}/cancel            取消订单（退回托管资金）

收尾原则和 test_orders_crud.py 一致：用例自己造的数据，测完物理删除。
钱包这份额外要还原余额 —— 资金变动不像订单那样删行了事，必须把
users 表那三列改回原值，否则跑一轮测试就把账号余额改了。
"""
import json

import allure
import pytest
import requests

from conftest import AUTH_HEADERS, BASE_URL

FEATURE = "钱包模块"

# 用管理员账号（id=1）当钱包的测试主体：它有 token、余额是 0.00，
# 加减起来不容易和别的数据混在一起。跑完由 wallet_subject 夹具还原。
SUBJECT_ID = 1


def _send(method, path, **kwargs):
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


def _balance():
    """取钱包概览的 data，顺便断言接口通"""
    resp = _get("/wallet/balance")
    assert resp.status_code == 200
    body = resp.json()
    assert body["code"] == 200, body
    return body["data"]


def _amount(value):
    """把接口返回的金额统一成 float 好比较"""
    return round(float(value), 2)


@pytest.fixture
def wallet_subject(conn):
    """把测试主体的余额和流水记下来，测完还原。

    yield 出去的是同一个用户 id，用例里直接用。
    """
    with conn.cursor() as cursor:
        # alipay_account 也要一起备份：withdraw() 会顺手把收款账号写进 users 表
        # （下次提现自动带出来），不还原的话跑一轮测试就把账号改了。
        cursor.execute(
            "SELECT wallet_balance, frozen_balance, total_earnings, alipay_account "
            "FROM users WHERE id = %s", (SUBJECT_ID,)
        )
        original = cursor.fetchone()
        assert original is not None, f"users 表里没有 id={SUBJECT_ID} 的用户"

        cursor.execute("SELECT COALESCE(MAX(id), 0) FROM wallet_transactions")
        max_tx_id = cursor.fetchone()[0]

        cursor.execute("SELECT COALESCE(MAX(id), 0) FROM orders")
        max_order_id = cursor.fetchone()[0]

    allure.attach(
        f"测试主体 users.id={SUBJECT_ID}\n"
        f"测试前 (可用, 冻结, 累计收益, 收款账号)={original}\n"
        f"流水基线 max(id)={max_tx_id}，订单基线 max(id)={max_order_id}",
        name="测试基线",
        attachment_type=allure.attachment_type.TEXT,
    )

    yield SUBJECT_ID

    with conn.cursor() as cursor:
        # 用例建的流水和订单全删掉
        cursor.execute("DELETE FROM wallet_transactions WHERE id > %s", (max_tx_id,))
        cursor.execute("DELETE FROM orders WHERE id > %s", (max_order_id,))
        # 余额和收款账号都改回原值
        cursor.execute(
            "UPDATE users SET wallet_balance = %s, frozen_balance = %s, total_earnings = %s, "
            "alipay_account = %s WHERE id = %s",
            (original[0], original[1], original[2], original[3], SUBJECT_ID),
        )

    with conn.cursor() as cursor:
        cursor.execute(
            "SELECT wallet_balance, frozen_balance, total_earnings, alipay_account "
            "FROM users WHERE id = %s",
            (SUBJECT_ID,),
        )
        assert cursor.fetchone() == original, "收尾失败：余额/收款账号没还原回去"


# ============================ 余额概览 ============================


@allure.feature(FEATURE)
@allure.story("余额查询")
@allure.title("余额接口：返回可用余额、冻结金额、累计收益三个字段")
@allure.severity(allure.severity_level.CRITICAL)
def test_balance_returns_three_amounts(wallet_subject):
    with allure.step("GET /wallet/balance"):
        data = _balance()

    with allure.step("校验字段齐全且都是数字"):
        for key in ("balance", "frozenBalance", "totalEarnings"):
            assert key in data, f"少了字段 {key}：{data}"
            assert _amount(data[key]) >= 0


# ============================ 充值 ============================


@allure.feature(FEATURE)
@allure.story("充值")
@allure.title("充值：可用余额增加，落一条 status=1 的 recharge 流水")
@allure.severity(allure.severity_level.CRITICAL)
def test_recharge_increases_balance(wallet_subject, conn):
    with allure.step("记下充值前的余额"):
        before = _amount(_balance()["balance"])

    with allure.step("POST /wallet/recharge 充值 100 元"):
        resp = _post("/wallet/recharge", json={"amount": 100, "payMethod": "支付宝"})
        assert resp.status_code == 200
        body = resp.json()
        assert body["code"] == 200, body

    with allure.step("接口返回的流水字段正确"):
        tx = body["data"]
        assert tx["type"] == "recharge"
        assert tx["status"] == 1
        assert _amount(tx["amount"]) == 100.00
        assert tx["direction"] == 1
        assert tx["transactionNo"].startswith("TX")

    with allure.step("重新查余额，确认加了 100"):
        assert _amount(_balance()["balance"]) == round(before + 100, 2)

    with allure.step("直连数据库确认流水落了库"):
        with conn.cursor() as cursor:
            cursor.execute(
                "SELECT amount, type, status FROM wallet_transactions WHERE id = %s",
                (tx["id"],),
            )
            row = cursor.fetchone()
        assert row is not None, "流水没落库"
        assert _amount(row[0]) == 100.00
        assert row[1] == "recharge"
        assert row[2] == 1


@allure.feature(FEATURE)
@allure.story("充值")
@allure.title("充值金额非法：0 和负数都被拒，余额不变")
@allure.severity(allure.severity_level.NORMAL)
@pytest.mark.parametrize("amount", [0, -10])
def test_recharge_rejects_non_positive_amount(wallet_subject, amount):
    with allure.step("记下充值前的余额"):
        before = _amount(_balance()["balance"])

    with allure.step(f"POST /wallet/recharge amount={amount}"):
        resp = _post("/wallet/recharge", json={"amount": amount})
        body = resp.json()
        assert body["code"] != 200
        assert "必须大于" in body["message"]

    with allure.step("余额没被改动"):
        assert _amount(_balance()["balance"]) == before


@allure.feature(FEATURE)
@allure.story("充值")
@allure.title("充值超过单次上限 5000 被拒")
@allure.severity(allure.severity_level.MINOR)
def test_recharge_rejects_amount_over_limit(wallet_subject):
    with allure.step("POST /wallet/recharge amount=99999"):
        resp = _post("/wallet/recharge", json={"amount": 99999})
        body = resp.json()
        assert body["code"] != 200
        assert "单次充值不能超过" in body["message"]


# ============================ 提现 ============================


@allure.feature(FEATURE)
@allure.story("提现")
@allure.title("申请提现：余额转入冻结，落一条 status=0 的待审核流水")
@allure.severity(allure.severity_level.CRITICAL)
def test_withdraw_moves_money_to_frozen(wallet_subject, conn):
    with allure.step("先充 100 元，保证有余额可提"):
        _post("/wallet/recharge", json={"amount": 100})
        before = _balance()

    with allure.step("POST /wallet/withdraw 提现 30 元"):
        resp = _post("/wallet/withdraw", json={"amount": 30, "account": "test@alipay.com"})
        assert resp.status_code == 200
        body = resp.json()
        assert body["code"] == 200, body

    with allure.step("校验流水是待审核状态"):
        tx = body["data"]
        assert tx["type"] == "withdraw"
        assert tx["status"] == 0
        assert tx["direction"] == -1

    with allure.step("余额减 30、冻结加 30（钱还在账上，只是不能用了）"):
        after = _balance()
        assert _amount(after["balance"]) == round(_amount(before["balance"]) - 30, 2)
        assert _amount(after["frozenBalance"]) == round(_amount(before["frozenBalance"]) + 30, 2)

    with allure.step("直连数据库确认两条流水都在"):
        with conn.cursor() as cursor:
            cursor.execute(
                "SELECT type, status, amount FROM wallet_transactions "
                "WHERE user_id = %s ORDER BY id DESC LIMIT 2",
                (SUBJECT_ID,),
            )
            rows = cursor.fetchall()
        assert len(rows) == 2
        assert rows[0][0] == "withdraw" and rows[0][1] == 0
        assert rows[1][0] == "recharge" and rows[1][1] == 1


@allure.feature(FEATURE)
@allure.story("提现")
@allure.title("提现低于 10 元或超过可用余额都被拒")
@allure.severity(allure.severity_level.NORMAL)
def test_withdraw_rejects_invalid_amount(wallet_subject):
    with allure.step("先充 50 元"):
        _post("/wallet/recharge", json={"amount": 50})

    with allure.step("提 5 元：低于最低提现额"):
        body = _post("/wallet/withdraw", json={"amount": 5, "account": "a@b.com"}).json()
        assert body["code"] != 200
        assert "不能低于" in body["message"]

    with allure.step("提 99999 元：超过可用余额"):
        body = _post("/wallet/withdraw", json={"amount": 99999, "account": "a@b.com"}).json()
        assert body["code"] != 200
        assert "余额不足" in body["message"]

    with allure.step("提现不填收款账号被拒"):
        body = _post("/wallet/withdraw", json={"amount": 20}).json()
        assert body["code"] != 200
        assert "收款账号" in body["message"]


# ============================ 交易记录 ============================


@allure.feature(FEATURE)
@allure.story("交易记录")
@allure.title("流水查询：按类型筛选只返回该类型")
@allure.severity(allure.severity_level.NORMAL)
def test_transactions_filter_by_type(wallet_subject):
    with allure.step("造两条不同类型的流水"):
        _post("/wallet/recharge", json={"amount": 60})
        _post("/wallet/withdraw", json={"amount": 10, "account": "t@a.com"})

    with allure.step("GET /wallet/transactions 不带筛选"):
        all_tx = _get("/wallet/transactions").json()["data"]
        assert len(all_tx) >= 2

    with allure.step("按 recharge 筛选"):
        only_recharge = _get("/wallet/transactions?type=recharge").json()["data"]
        assert len(only_recharge) >= 1
        assert all(tx["type"] == "recharge" for tx in only_recharge)
        assert all(tx["direction"] == 1 for tx in only_recharge)

    with allure.step("按 withdraw 筛选，方向应该是支出"):
        only_withdraw = _get("/wallet/transactions?type=withdraw").json()["data"]
        assert all(tx["type"] == "withdraw" for tx in only_withdraw)
        assert all(tx["direction"] == -1 for tx in only_withdraw)

    with allure.step("limit 生效"):
        limited = _get("/wallet/transactions?limit=1").json()["data"]
        assert len(limited) == 1


# ============================ 管理端审核 ============================


@allure.feature(FEATURE)
@allure.story("提现审核")
@allure.title("提现审核通过：冻结金额扣掉，余额不再变化")
@allure.severity(allure.severity_level.CRITICAL)
def test_admin_audit_withdraw_pass(wallet_subject, conn):
    with allure.step("充 100 再提 40，造一条待审核申请"):
        _post("/wallet/recharge", json={"amount": 100})
        tx = _post("/wallet/withdraw", json={"amount": 40, "account": "x@y.com"}).json()["data"]
        before = _balance()

    with allure.step("GET /admin/finance/withdrawals?status=0 能查到这条"):
        pending = _get("/admin/finance/withdrawals?status=0").json()["data"]
        assert any(item["id"] == tx["id"] for item in pending)

    with allure.step(f"POST /admin/finance/withdrawals/{tx['id']}/audit pass=true"):
        body = _post(
            f"/admin/finance/withdrawals/{tx['id']}/audit",
            json={"pass": True, "remark": "pytest 审核通过"},
        ).json()
        assert body["code"] == 200, body
        assert body["data"]["status"] == 1

    with allure.step("冻结金额减 40，可用余额不变"):
        after = _balance()
        assert _amount(after["balance"]) == _amount(before["balance"])
        assert _amount(after["frozenBalance"]) == round(_amount(before["frozenBalance"]) - 40, 2)

    with allure.step("同一条不能重复审核"):
        again = _post(
            f"/admin/finance/withdrawals/{tx['id']}/audit", json={"pass": True}
        ).json()
        assert again["code"] != 200
        assert "已经处理过" in again["message"]


@allure.feature(FEATURE)
@allure.story("提现审核")
@allure.title("提现审核驳回：钱从冻结退回可用余额")
@allure.severity(allure.severity_level.CRITICAL)
def test_admin_audit_withdraw_reject_refunds(wallet_subject):
    with allure.step("充 100 再提 25"):
        _post("/wallet/recharge", json={"amount": 100})
        tx = _post("/wallet/withdraw", json={"amount": 25, "account": "z@y.com"}).json()["data"]
        before = _balance()

    with allure.step(f"审核驳回"):
        body = _post(
            f"/admin/finance/withdrawals/{tx['id']}/audit",
            json={"pass": False, "remark": "pytest 驳回"},
        ).json()
        assert body["code"] == 200, body
        assert body["data"]["status"] == 2

    with allure.step("可用余额加回 25，冻结减 25"):
        after = _balance()
        assert _amount(after["balance"]) == round(_amount(before["balance"]) + 25, 2)
        assert _amount(after["frozenBalance"]) == round(_amount(before["frozenBalance"]) - 25, 2)


# ============================ 订单支付打通钱包 ============================


def _publish_order(reward):
    """用测试主体发一单，返回订单 data"""
    resp = _post("/orders", json={
        "taskType": "pytest钱包联调",
        "pickupLocation": "钱包测试取货点",
        "deliveryLocation": "钱包测试送达点",
        "description": "pytest 钱包模块测试数据，跑完即删",
        "reward": reward,
    })
    body = resp.json()
    assert body["code"] == 200, body
    return body["data"]


@allure.feature(FEATURE)
@allure.story("订单支付")
@allure.title("订单支付：余额扣款并转入冻结托管")
@allure.severity(allure.severity_level.CRITICAL)
def test_pay_order_freezes_wallet_money(wallet_subject, conn):
    with allure.step("先充 50 元"):
        _post("/wallet/recharge", json={"amount": 50})

    with allure.step("发布一笔 8 元的订单"):
        order = _publish_order(8)
        before = _balance()

    with allure.step(f"POST /orders/{order['id']}/pay"):
        body = _post(f"/orders/{order['id']}/pay", json={"payMethod": "wallet"}).json()
        assert body["code"] == 200, body
        assert body["data"]["status"] == 1        # 待接单
        assert body["data"]["paymentStatus"] == 1

    with allure.step("余额减 8、冻结加 8"):
        after = _balance()
        assert _amount(after["balance"]) == round(_amount(before["balance"]) - 8, 2)
        assert _amount(after["frozenBalance"]) == round(_amount(before["frozenBalance"]) + 8, 2)

    with allure.step("流水是 order_pay，带上了订单 id"):
        tx = _get("/wallet/transactions?type=order_pay").json()["data"][0]
        assert tx["orderId"] == order["id"]
        assert tx["direction"] == -1

    with allure.step("直连数据库确认订单状态"):
        with conn.cursor() as cursor:
            cursor.execute("SELECT status, payment_status FROM orders WHERE id = %s", (order["id"],))
            row = cursor.fetchone()
        assert row == (1, 1)


@allure.feature(FEATURE)
@allure.story("订单支付")
@allure.title("余额不够时支付被拒，订单状态不变")
@allure.severity(allure.severity_level.CRITICAL)
def test_pay_order_rejected_when_balance_not_enough(wallet_subject, conn):
    with allure.step("确认当前余额，发布一笔远超余额的订单"):
        balance = _amount(_balance()["balance"])
        order = _publish_order(round(balance + 500, 2))

    with allure.step(f"POST /orders/{order['id']}/pay，应当失败"):
        body = _post(f"/orders/{order['id']}/pay", json={"payMethod": "wallet"}).json()
        assert body["code"] != 200
        assert "余额不足" in body["message"]

    with allure.step("订单还是待支付，没有变成待接单"):
        with conn.cursor() as cursor:
            cursor.execute(
                "SELECT status, payment_status FROM orders WHERE id = %s", (order["id"],)
            )
            row = cursor.fetchone()
        assert row == (0, 0)


@allure.feature(FEATURE)
@allure.story("订单支付")
@allure.title("已支付订单取消：托管资金退回可用余额")
@allure.severity(allure.severity_level.CRITICAL)
def test_cancel_paid_order_refunds(wallet_subject, conn):
    with allure.step("充 50，发一单 12 元并支付"):
        _post("/wallet/recharge", json={"amount": 50})
        order = _publish_order(12)
        _post(f"/orders/{order['id']}/pay", json={"payMethod": "wallet"})
        before = _balance()

    with allure.step(f"GET /orders/{order['id']}/cancel"):
        body = _get(f"/orders/{order['id']}/cancel?reason=pytest%20取消").json()
        assert body["code"] == 200, body
        assert body["data"]["status"] == 5

    with allure.step("冻结减 12、可用余额加回 12"):
        after = _balance()
        assert _amount(after["balance"]) == round(_amount(before["balance"]) + 12, 2)
        assert _amount(after["frozenBalance"]) == round(_amount(before["frozenBalance"]) - 12, 2)

    with allure.step("流水里有一条 order_refund"):
        refunds = _get("/wallet/transactions?type=order_refund").json()["data"]
        assert any(tx["orderId"] == order["id"] for tx in refunds)
