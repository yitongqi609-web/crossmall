#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
CrossMall · 潮汐全球购 冒烟测试
覆盖:注册登录 → 商品(多币种) → 购物车 → 结算试算(运费+关税) → 下单(汇率快照)
      → 支付幂等(重复回调/篡改金额) → 履约轨迹 → 管理端状态机流转(含非法流转拒绝)
      → 并发下单防超卖 → 兜底关单 → 汇率拉取 → 看板

用法: python scripts/smoke_test.py
环境: BASE=http://localhost:8582 python scripts/smoke_test.py
依赖: pip install requests
"""
import concurrent.futures
import hashlib
import json
import os
import sys
import time
import uuid

import requests

sys.stdout.reconfigure(encoding="utf-8", errors="replace")

BASE = os.environ.get("BASE", "http://localhost:8582")
PAY_SECRET = "cm-mock-pay-gateway-secret-2026"

PASSED = []
FAILED = []


def check(name, cond, detail=""):
    if cond:
        PASSED.append(name)
        print(f"  ✅ {name}")
    else:
        FAILED.append((name, detail))
        print(f"  ❌ {name}  {detail}")


def jbody(resp):
    """统一按 UTF-8 解析响应体,规避编码歧义"""
    return json.loads(resp.content.decode("utf-8"))


def ok(resp, expect_code=200):
    body = jbody(resp)
    return resp.status_code == 200 and body.get("code") == expect_code, body


def data_of(body):
    return body.get("data") or {}


class Client:
    def __init__(self, token=None):
        self.s = requests.Session()
        if token:
            self.s.headers["Authorization"] = f"Bearer {token}"

    def get(self, path, **kw):
        return self.s.get(BASE + path, timeout=15, **kw)

    def post(self, path, **kw):
        return self.s.post(BASE + path, timeout=15, **kw)

    def put(self, path, **kw):
        return self.s.put(BASE + path, timeout=15, **kw)


def main():
    print(f"=== CrossMall Smoke Test → {BASE} ===\n")

    # ---------- 1. 注册/登录 ----------
    print("[1] 账号")
    email = f"smoke-{uuid.uuid4().hex[:8]}@crossmall.com"
    c_anon = Client()
    r = c_anon.post("/api/auth/register", json={"email": email, "password": "smoke123", "nickname": "SmokeBot"})
    passed, body = ok(r)
    check("注册", passed, str(body))

    r = c_anon.post("/api/auth/login", json={"email": email, "password": "smoke123"})
    passed, body = ok(r)
    check("登录", passed, str(body))
    token = data_of(body).get("token", "")
    c = Client(token)

    # ---------- 2. 商品与多币种 ----------
    print("[2] 商品与多币种")
    r = c.get("/api/home", params={"currency": "EUR"})
    passed, body = ok(r)
    hot = data_of(body).get("hotGoods", [])
    check("首页(分类+热销)", passed and len(hot) > 0, str(body.get("message")))
    check("EUR 展示价换算(€ 符号+金额≠USD)", passed and hot
          and hot[0].get("displaySymbol") == "€" and hot[0].get("displayPrice") != hot[0].get("priceCents"),
          str(hot[0] if hot else ""))

    r = c.get("/api/goods", params={"keyword": "phone", "currency": "USD"})
    passed, body = ok(r)
    goods_list = data_of(body).get("records", [])
    check("商品搜索(英文标题)", passed, str(body.get("message")))

    spu_id = hot[0]["spuId"] if hot else (goods_list[0]["spuId"] if goods_list else 1)
    r = c.get(f"/api/goods/{spu_id}", params={"currency": "JPY"})
    passed, body = ok(r)
    skus = data_of(body).get("skus", [])
    check("商品详情(SKU+JPY价)", passed and len(skus) > 0, str(body.get("message")))

    # ---------- 3. 购物车 ----------
    print("[3] 购物车")
    sku = skus[0]
    r = c.post("/api/cart/items", json={"skuId": sku["skuId"], "quantity": 1})
    passed, _ = ok(r)
    check("加购", passed)
    r = c.post("/api/cart/items", json={"skuId": sku["skuId"], "quantity": 1})
    passed, _ = ok(r)
    check("重复加购", passed)
    r = c.get("/api/cart/count")
    passed, body = ok(r)
    check("购物车 SKU 种类=1", passed and body.get("data") == 1, str(body))
    r = c.get("/api/cart", params={"currency": "USD"})
    passed, body = ok(r)
    cart_items = data_of(body)
    check("购物车数量累加=2", passed and len(cart_items) == 1
          and cart_items[0].get("quantity") == 2, str(cart_items))

    # ---------- 4. 地址 + 结算试算 ----------
    print("[4] 结算试算(运费+关税)")
    r = c.post("/api/address", json={
        "receiverName": "Smoke Bot", "phone": "+1 202-555-0100", "countryCode": "US",
        "countryName": "United States", "stateProvince": "California", "city": "Los Angeles",
        "street": "1 Test Street", "postcode": "90001", "isDefault": 1})
    passed, body = ok(r)
    addr_id = data_of(body).get("id")
    check("新增 US 地址", passed and addr_id, str(body))

    r = c.post("/api/order/preview", json={
        "addressId": addr_id, "currency": "USD", "items": [{"skuId": sku["skuId"], "quantity": 1}]})
    passed, body = ok(r)
    preview = data_of(body)
    check("试算三段金额", passed and all(k in preview for k in
          ("goodsFeeCents", "shippingFeeCents", "taxFeeCents", "totalFeeCents")), str(body.get("message")))
    check("运费已计算", preview.get("shippingFeeCents", 0) > 0, str(preview.get("shippingFeeCents")))
    check("美国 de minimis 免税额度返回", "deMinimisCents" in preview and preview.get("deMinimisCents", 0) > 0,
          str(preview.get("deMinimisCents")))
    check("线路选项≥1", passed and len(preview.get("lineOptions", [])) >= 1, str(preview.get("lineOptions")))

    # 大数量验证关税生效(商品额超免税额度)
    qty = max(1, int(80000 // max(1, sku["priceCents"])) + 1)
    r = c.post("/api/order/preview", json={
        "addressId": addr_id, "currency": "USD", "items": [{"skuId": sku["skuId"], "quantity": qty}]})
    passed, body = ok(r)
    pv_tax = data_of(body)
    check("超免税额度后关税生效", passed and pv_tax.get("taxFeeCents", 0) > 0,
          f"tax={pv_tax.get('taxFeeCents')} qty={qty}")

    # ---------- 5. 下单(汇率快照) ----------
    print("[5] 下单与汇率快照")
    r = c.post("/api/order", json={
        "addressId": addr_id, "lineId": preview["lineId"], "currency": "EUR",
        "items": [{"skuId": sku["skuId"], "quantity": 1}]})
    passed, body = ok(r)
    order_no = body.get("data", "")
    check("下单成功", passed and order_no, str(body.get("message")))

    r = c.get(f"/api/order/{order_no}")
    passed, body = ok(r)
    order = data_of(body)
    check("订单汇率快照(EUR)", passed and order.get("currency") == "EUR"
          and float(order.get("fxRate", 0)) > 0, str(order.get("fxRate")))
    check("本币应付金额(快照折算)", passed and order.get("totalDisplayPrice", 0) > 0,
          str(order.get("totalDisplayPrice")))

    # ---------- 6. 支付与幂等 ----------
    print("[6] 支付与幂等")
    r = c.post(f"/api/pay/{order_no}", json={"channel": "PAYPAL"})
    passed, body = ok(r)
    pay = data_of(body)
    txn_no = pay.get("txnNo", "")
    check("创建支付流水", passed and txn_no, str(body.get("message")))

    sign = hashlib.md5(f"{txn_no}|{pay['amountLocal']}|{PAY_SECRET}".encode()).hexdigest()
    r = c.post("/api/mock/pay/notify", json={
        "txnNo": txn_no, "amountLocal": pay["amountLocal"], "sign": sign, "payload": "smoke"})
    passed, body = ok(r)
    check("回调支付成功", passed, str(body))
    r = c.post("/api/mock/pay/notify", json={
        "txnNo": txn_no, "amountLocal": pay["amountLocal"], "sign": sign, "payload": "replay"})
    passed, body = ok(r)
    check("重复回调幂等", passed, str(body))
    r = c_anon.post("/api/mock/pay/notify", json={
        "txnNo": txn_no, "amountLocal": 1, "sign": sign, "payload": "tamper"})
    body = jbody(r)
    check("篡改金额被拒", body.get("code") != 200, str(body))

    r = c.get(f"/api/order/{order_no}")
    passed, body = ok(r)
    check("订单状态=PAID", data_of(body).get("status") == "PAID", str(data_of(body).get("status")))

    tracks = []
    track_ok = False
    for _ in range(10):
        r = c.get(f"/api/order/{order_no}")
        tracks = data_of(jbody(r)).get("tracks", [])
        if any(t.get("status") == "PAID" for t in tracks):
            track_ok = True
            break
        time.sleep(1)
    check("履约轨迹(MQ 异步落库)", track_ok, str([t.get("status") for t in tracks]))

    # ---------- 7. 管理端状态机 ----------
    print("[7] 管理端与状态机")
    r = c_anon.post("/api/admin/auth/login", json={"username": "admin", "password": "admin123"})
    passed, body = ok(r)
    admin_token = data_of(body).get("token", "")
    check("管理员登录", passed and admin_token, str(body))
    a = Client(admin_token)

    def transit(target, expect_ok=True, name=""):
        rr = a.post(f"/api/admin/order/{order_no}/transit",
                    json={"targetStatus": target, "location": "Smoke CN",
                          "description": "smoke", "descriptionEn": "smoke"})
        b = jbody(rr)
        if expect_ok:
            check(name or f"流转→{target}", b.get("code") == 200, str(b))
        else:
            check(name or f"非法流转→{target} 被拒", b.get("code") != 200, str(b))

    transit("STOCKED")
    transit("DECLARED")
    transit("COMPLETED", expect_ok=False, name="非法跳状态 DECLARED→COMPLETED 被拒")
    transit("IN_TRANSIT")
    transit("CUSTOMS_CLEARANCE")
    transit("DELIVERING")
    transit("COMPLETED")

    r = c.get(f"/api/order/{order_no}")
    tracks = data_of(jbody(r)).get("tracks", [])
    check("全链路轨迹完整(≥7 节点)", len(tracks) >= 7, str(len(tracks)))

    # ---------- 8. 并发下单防超卖 ----------
    print("[8] 并发下单防超卖")
    r = c.get(f"/api/goods/{spu_id}", params={"currency": "USD"})
    target_sku = data_of(jbody(r)).get("skus", [])[0]
    stock = target_sku["stock"]
    burst = min(30, max(5, stock + 5))
    print(f"  SKU#{target_sku['skuId']} 当前库存 {stock},并发下单 {burst} 笔(qty=1,仅下单不支付)")

    def create_once(_):
        cc = Client(token)
        rr = cc.post("/api/order", json={
            "addressId": addr_id, "lineId": preview["lineId"], "currency": "USD",
            "items": [{"skuId": target_sku["skuId"], "quantity": 1}]})
        b = jbody(rr)
        return (b.get("code") == 200, b.get("data", ""), b.get("message", ""))

    with concurrent.futures.ThreadPoolExecutor(max_workers=burst) as pool:
        results = list(pool.map(create_once, range(burst)))
    success_orders = [o for s, o, _ in results if s]
    fail_msgs = [m for s, _, m in results if not s]
    check(f"成功笔数({len(success_orders)}) ≤ 库存({stock})", len(success_orders) <= stock,
          str(len(success_orders)))
    check("失败原因均为库存不足", all("库存不足" in m for m in fail_msgs), str(fail_msgs[:2]))

    restored = 0
    for o in success_orders:
        rr = c.post(f"/api/order/{o}/cancel", params={"reason": "smoke cleanup"})
        if jbody(rr).get("code") == 200:
            restored += 1
    check("取消回补库存", restored == len(success_orders), f"{restored}/{len(success_orders)}")
    r = c.get(f"/api/goods/{spu_id}", params={"currency": "USD"})
    stock_after = data_of(jbody(r)).get("skus", [])[0]["stock"]
    check("库存恢复一致", stock_after == stock, f"before={stock} after={stock_after}")

    # ---------- 9. 兜底关单 + 汇率拉取 + 看板 ----------
    print("[9] 兜底与管理端杂项")
    r = a.post("/api/admin/order/close-expired", params={"limit": 50})
    passed, body = ok(r)
    check("关单兜底对账", passed and body.get("data", -1) >= 0, str(body))

    r = a.post("/api/admin/fx/pull")
    passed, body = ok(r)
    check("汇率模拟拉取", passed and body.get("data", 0) > 0, str(body))

    r = a.get("/api/admin/fx/rates")
    passed, body = ok(r)
    rates = data_of(body)
    check("汇率已刷新(AUTO)", any(x.get("source") == "AUTO" for x in rates), str(rates))

    r = a.get("/api/admin/dashboard")
    passed, body = ok(r)
    dash = data_of(body)
    check("看板数据", passed and "gmvUsdCents" in dash and "trend" in dash, str(body.get("message")))

    # ---------- 总结 ----------
    print(f"\n=== 结果: {len(PASSED)} passed, {len(FAILED)} failed ===")
    for name, detail in FAILED:
        print(f"  FAIL: {name} → {detail}")
    sys.exit(1 if FAILED else 0)


if __name__ == "__main__":
    main()
