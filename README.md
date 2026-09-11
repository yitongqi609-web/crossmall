# CrossMall · 潮汐全球购 🌊

> 一个以 **跨境业务复杂度** 为核心的电商全栈项目:多币种结算、关税试算、国际履约状态机、支付幂等——每一处都是真实跨境生意里的工程难题。

![Java](https://img.shields.io/badge/Java-21-orange)
![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.5.x-brightgreen)
![Vue](https://img.shields.io/badge/Vue-3.5-42b883)
![Element Plus](https://img.shields.io/badge/Element%20Plus-2.9-409eff)
![MySQL](https://img.shields.io/badge/MySQL-8.4-4479a1)
![Redis](https://img.shields.io/badge/Redis-7.4-dc382d)
![RocketMQ](https://img.shields.io/badge/RocketMQ-5.3-ff6a00)
![License](https://img.shields.io/badge/License-MIT-blue)

---

## 目录

- [项目简介](#一项目简介)
- [为什么不是又一个商城 Demo](#二为什么不是又一个商城-demo)
- [功能总览](#三功能总览)
- [架构设计](#四架构设计)
- [核心设计与权衡](#五核心设计与权衡)
- [数据模型](#六数据模型)
- [快速启动](#七快速启动)
- [演示账号](#八演示账号)
- [配置说明](#九配置说明)
- [接口文档](#十接口文档)
- [冒烟测试](#十一冒烟测试)
- [项目结构](#十二项目结构)
- [常见问题 FAQ](#十三常见问题-faq)
- [Roadmap](#十四roadmap)

---

## 一、项目简介

CrossMall(潮汐全球购 TideMall)是一个面向海外消费者的 **B2C 跨境电商独立站**,覆盖完整业务闭环:

```
浏览(多语言/多币种) → 购物车 → 结算试算(运费+关税+汇率)
→ 下单(锁定汇率快照) → 收银台支付(幂等回调) → 国际履约(状态机+轨迹) → 妥投
```

同时配套完整管理后台:商品中台、汇率管理、关税税则、物流线路、订单履约操作台、销售看板。

**技术栈**:Java 21 · Spring Boot 3.5 · MyBatis-Plus · MySQL 8.4 · Redis 7.4 · RocketMQ 5.3 · Vue 3 · Element Plus · ECharts · Docker Compose

## 二、为什么不是又一个商城 Demo

市面上绝大多数电商练手项目只覆盖"国内电商"的教科书链路。CrossMall 把重心放在 **跨境业务特有的工程难题** 上,每一处都有明确的业务动机:

| 跨境难题 | 本项目的解法 | 为什么必须这么做 |
|---|---|---|
| 汇率实时波动,下单到支付之间金额会变 | **汇率快照**:下单瞬间把汇率写入订单,支付/退款全部按快照 | 否则同一笔订单"下单 ¥100、支付 ¥103",财务对不上账 |
| 目的国关税规则不同 | **税则引擎**:国家 × HS 编码三级匹配 + de minimis 免税额度(美 $800 / 澳 AUD1000) | 关税透明展示直接影响跨境转化率,这是独立站的常识 |
| 国际物流计费复杂 | **首重/续重计费引擎**:整单计费重 = Σ(件重×数量),按线路首续重阶梯计费 | 不同线路(经济/快速/铁路/专线)目的国与费率完全不同 |
| 履约链路长、节点多 | **10 状态履约状态机**:备货→报关→干线→清关→派送→妥投,合法流转表 + 条件更新双保险 | 报关后不能跳回备货;管理端双开页面并发操作必须有一致性保障 |
| 支付网关必然重试回调 | **三重幂等**:验签 → Redis SETNX → 流水条件更新,金额校验防篡改 | 真实支付场景的第一原则 |
| 多币种金额精度 | 全链路 **BIGINT 分存储 + USD 基准币**,换算只在出入口发生 | 浮点误差在财务场景是事故 |

## 三、功能总览

### 用户端(中/英双语,6 币种实时切换)

- 🏠 首页聚合:分类导航、热销排行、汇率快照提示
- 🛍️ 商品:分类筛选 / 关键词搜索 / SKU 规格选择 / 库存实时显示
- 🛒 购物车:Redis Hash 存储,仅存「SKU+数量」,读取时实时解析最新价格与库存,规避快照过期
- 🧾 结算:地址选择 → 物流线路比价(各线路运费实时试算)→ 关税明细 Tooltip → 币种切换 → 汇率快照提示
- 💳 模拟收银台:PayPal / 国际卡双通道、支付倒计时、超时提醒
- 📦 订单:状态分页 / 详情(履约轨迹时间线)/ 待付款取消
- 🙋 个人中心:国际地址簿(13 国区号联动,如 +86 / +1 / +44)

### 管理后台

- 📊 数据看板:GMV(USD 折算)、有效订单、近 7 日趋势、状态分布饼图、热销 Top5(ECharts)
- 📦 商品管理:SPU+SKU 整体编辑、双语内容、HS 编码、上下架
- 🧭 分类管理 / 💱 汇率管理(手动改价 + 模拟第三方定时拉取,缓存自动失效)/ 🛃 关税税则 / 🚚 物流线路
- 🧾 订单履约操作台:按状态筛选 → 详情抽屉 → 依据状态机渲染的下一步流转按钮(备货/报关/启运/清关/派送/妥投/发货前退款)+ 轨迹时间线 + **超时关单兜底对账**

## 四、架构设计

```
                 ┌─────────────────────────────────────────────────────┐
                 │                  Vue 3 (用户端 + 管理端)              │
                 │      币种/语言切换 · 结算试算 · 收银台 · 履约看板        │
                 └────────────────────────┬────────────────────────────┘
                                          │ /api (axios 统一封装,双 token)
┌──────────────┐                ┌─────────▼──────────┐
│  模拟支付网关  │  签名异步回调   │   Spring Boot 3.5   │  Knife4j 文档
│  /api/mock/* │ ─────────────►│                     │
└──────────────┘   验签+幂等    │ ┌─────┐  ┌───────┐  │
                 ┌──────────┐   │ │订单  │  │履约    │  │
                 │ 汇率源(模拟)│  │ │服务  │  │状态机  │  │
                 └────┬─────┘   │ └─────┘  └───────┘  │
                      │ 定时拉取  └──┬──────┬──────┬───┘
                      ▼             ▼      ▼      ▼
                 ┌─────────┐   ┌────────┐ ┌─────┐ ┌──────────┐
                 │  Redis  │   │ MySQL  │ │ MQ  │ │ 定时任务   │
                 │ 库存闸门  │   │ 权威库存│ │延迟  │ │ 汇率拉取   │
                 │ 购物车/幂等│  │ 汇率快照│ │轨迹  │ │ 兜底关单   │
                 └─────────┘   └────────┘ └─────┘ └──────────┘
```

**分层约定**:Controller(参数校验)→ Service(业务编排)→ Mapper(MyBatis-Plus);`fulfillment` 包独立承载状态机,`mq` 包隔离消息生产/消费,第三方集成(支付网关/汇率源)均以独立 Controller/Task 隔离,可平滑替换为真实实现。

## 五、核心设计与权衡

### 5.1 多币种与汇率快照 💱

- SKU 只存基准币 **USD 分**(BIGINT),展示价由汇率服务实时换算,前端 6 币种切换
- 汇率服务:Redis 读穿透缓存(TTL 2h)+ 内存币种表(符号等只读信息)+ 定时模拟第三方拉取(±0.5% 随机游走)
- **下单瞬间锁定汇率快照**存入订单与支付流水,后续支付、退款、报表全部按快照折算

```java
// 订单落库:金额与汇率全部写入快照,与实时汇率解耦
order.setFxRate(fx.getRate());                    // 1 USD = fx
order.setTotalFeeCents(calc.totalUsd());          // USD 分
order.setTotalLocalCents(fxService.convert(...)); // 本币应付(快照折算)
```

> **权衡**:快照导致同一商品在不同订单可能单价不同——这是刻意的,跨境财务对账要求"订单金额永远等于成交时点金额"。

### 5.2 关税与运费试算 🛃

- 税则匹配三级降级:`(国家, HS)` → `(国家, *)` → `(全球 *, *)`,任何目的国都有兜底税率
- **de minimis 免税额度**:商品申报总额低于该国额度时整单免税(美 $800、日 $100、澳 AUD1000)
- 运费:`首重费 + ceil(超出重量/续重单位) × 续重费`,线路按目的国过滤,结算页多线路比价
- 简化约定:税基 = 商品总额(不含运费),多品类订单按品类税率分别计税后求和

### 5.3 履约状态机 🚚

```java
private static final Map<OrderStatus, Set<OrderStatus>> TRANSITIONS = Map.of(
        PENDING_PAYMENT, Set.of(PAID, CANCELLED),
        PAID, Set.of(STOCKED, REFUNDED),      // 发货前可退款
        STOCKED, Set.of(DECLARED),
        DECLARED, Set.of(IN_TRANSIT),
        IN_TRANSIT, Set.of(CUSTOMS_CLEARANCE),
        CUSTOMS_CLEARANCE, Set.of(DELIVERING),
        DELIVERING, Set.of(COMPLETED)
);
```

- **合法流转表是唯一真相源**,配合 `UPDATE ... WHERE status = 期望值` 条件更新双保险:非法跳状态直接拒绝,并发操作下条件更新失败的请求视为过期请求
- 每次流转发送 MQ 轨迹消息异步落库(含双语描述与节点地点),主链路不因轨迹写放大变慢

### 5.4 支付三重幂等 💳

```
回调 → ① 验签 MD5(txnNo|amount|secret)      防伪造
     → ② Redis SETNX pay:done:{txnNo}       高频重放快速拦截
     → ③ 流水条件更新 INIT→SUCCESS + 唯一索引  并发回调只成功一次
     → ④ 金额一致性校验                       回调金额≠流水金额直接拒绝(防篡改)
     → ⑤ 订单条件流转 PENDING_PAYMENT→PAID    与超时关单赛跑,输方自动补偿(流水置 VOID)
```

> **权衡**:用「结果对象」而非异常返回业务失败,避免误回滚已落库的流水状态。

### 5.5 超时关单 ⏰

- 下单事务 **提交后(afterCommit)** 才发 RocketMQ 延迟消息——避免消息先于订单落库被消费
- 延迟等级向上取整(如 45s → 1m 等级),保证消息永不早于截止时间到达
- 关单与支付回调并发:两者都对订单做条件更新 `WHERE status = PENDING_PAYMENT`,**谁先谁赢,输方自动补偿**
- 双保险:MQ 消息之外提供「兜底对账」扫描过期订单(管理端手动 + 可定时),防消息丢失

### 5.6 库存三层防超卖 📦

```
① Redis Lua 原子预扣(判存+扣减一体,挡量快速失败)
② DB 条件更新 WHERE stock >= n(权威值兜底,事务内)
③ 取消/超时关单/退款 双端回补(DB + Redis)
```

> Redis 只是"闸门"不是账本;30 并发下单实测:成功笔数 ≤ 库存,取消后库存分毫不差。

## 六、数据模型

13 张核心表(MySQL 8.4,utf8mb4):

| 表 | 职责 | 关键设计 |
|---|---|---|
| `spu` / `sku` | 商品 | 双语字段成对存储;SKU 存 USD 分基准价;`hs_code` 关联税则 |
| `fx_rate` | 汇率 | `MANUAL`/`AUTO` 双来源;1 USD = rate |
| `hs_tax` | 税则 | `(country, hs)` 唯一;`*` 为国家默认;`de_minimis_cents` 免税额度 |
| `logistics_line` | 物流线路 | 首重/续重费率 + 支持目的国 JSON |
| `orders` | 订单 | **汇率快照 + 三段金额(商品/运费/关税)+ 本币应付**;`pay_timeout_at` 支持兜底扫描 |
| `order_item` | 明细 | 标题/规格/单价全快照,历史订单不受商品改价影响 |
| `payment_transaction` | 支付流水 | `txn_no` 唯一索引 = 幂等锚点;状态机 INIT→SUCCESS→REFUNDED/VOID |
| `order_track` | 履约轨迹 | 每次状态流转一条,双语描述,由 MQ 消费者异步写入 |
| `user` / `address` / `admin_user` / `category` | 基础 | 地址含 ISO2 国家码,支持国际格式 |

## 七、快速启动

### 环境要求

| 依赖 | 版本 |
|---|---|
| JDK | 21+ |
| Maven | 3.9+ |
| Node.js | 18+ |
| Docker & Docker Compose | 任意近期版本 |

### 1. 启动中间件(MySQL / Redis / RocketMQ)

```bash
git clone https://github.com/yitongqi609-web/crossmall.git
cd crossmall
docker compose up -d
```

首次启动会自动执行 `docker/mysql/init/` 下的建库脚本与种子数据(约 30s)。

### 2. 启动后端

```bash
cd backend
mvn spring-boot:run
# 或:mvn package -DskipTests && java -jar target/crossmall-backend-1.0.0.jar
```

启动日志出现 `CrossMall · 潮汐全球购 started` 即成功,接口文档:<http://localhost:8582/doc.html>

### 3. 启动前端

```bash
cd frontend
npm install
npm run dev
```

访问 <http://localhost:5175>

### 4.(可选)前端容器化部署

```bash
cd frontend && npm run build
cd .. && docker compose up -d frontend
# 访问 http://localhost:8091(nginx 反代宿主机 8582)
```

### 端口清单(全部与常见默认值错开,支持多项目共存)

| 服务 | 端口 | 说明 |
|---|---|---|
| MySQL | **3311** → 3306 | root / root123,库名 `crossmall` |
| Redis | **6391** → 6379 | 无密码 |
| RocketMQ Namesrv | **9882** → 9876 | |
| RocketMQ Dashboard | **9883** → 8080 | 可视化控制台 |
| RocketMQ Broker | **21911** | 广播 127.0.0.1,与常见 10911 错开 |
| 后端 | **8582** | |
| 前端(dev) | **5175** | `/api` 代理到 8582 |
| 前端(nginx) | **8091** | 可选 |

## 八、演示账号

| 端 | 账号 | 密码 | 说明 |
|---|---|---|---|
| 买家 | `demo@crossmall.com` | `demo123` | 预置 2 个国际地址 |
| 管理员 | `admin` | `admin123` | 管理后台 `/admin/login` |

> 种子数据包含 20 个双语商品(43 SKU)、6 币种汇率、14 条关税税则、4 条物流线路。

## 九、配置说明

关键配置在 `backend/src/main/resources/application.yml`,全部支持启动参数覆盖:

```yaml
crossmall:
  order:
    pay-timeout: PT30M        # 支付超时关单时长(ISO-8601)
  fx:
    pull-cron: "0 0 */2 * * ?"  # 汇率定时拉取
  pay:
    mock-gateway-secret: ***    # 模拟网关回调验签密钥
```

**演示超时关单**(推荐体验):

```bash
java -jar target/crossmall-backend-1.0.0.jar --crossmall.order.pay-timeout=PT20S
# 下单后不支付,约 20 秒后订单自动取消、库存回补、轨迹落库
```

**Redis 依赖说明**:库存闸门在应用启动时自动从 DB 预热(SET NX);汇率缓存失效策略为写操作主动删除 + TTL 兜底。

## 十、接口文档

启动后端后访问 Knife4j 文档:<http://localhost:8582/doc.html>(支持 Authorize 填 Bearer Token)

| 分组 | 前缀 | 说明 |
|---|---|---|
| 买家账号 | `/api/auth` `/api/user` | 注册/登录/我的信息 |
| 地址 | `/api/address` | 国际地址 CRUD + 默认地址 + 区号联动 |
| 首页/商品 | `/api/home` `/api/goods` | 聚合/搜索/详情(`currency` 参数实时换算) |
| 汇率 | `/api/fx/rates` | 公开汇率(币种切换器) |
| 购物车 | `/api/cart` | Redis 购物车 |
| 订单 | `/api/order` | 试算/下单/分页/详情/取消 |
| 支付 | `/api/pay/{orderNo}` | 创建支付流水 |
| 模拟网关 | `/api/mock/pay` | 收银台确认 + 回调 Webhook(可重放验证幂等) |
| 管理端 | `/api/admin/*` | 登录/商品/分类/汇率/税则/线路/履约/看板 |

## 十一、冒烟测试

```bash
pip install requests
python scripts/smoke_test.py          # BASE=http://localhost:8582 可覆盖
```

**42 项断言**覆盖:

- 注册登录、多币种展示(EUR €/JPY ¥ 换算与符号)
- 购物车加购累加、实时价格解析
- 结算试算:三段金额、运费计算、de minimis 免税、超额度关税生效
- 下单汇率快照(EUR)、本币应付金额
- 支付:回调成功 → **重复回调幂等** → **篡改金额被拒**
- 履约轨迹 MQ 异步落库;状态机全链路流转(**非法跳状态被拒**)
- **30 并发下单防超卖**:成功笔数 ≤ 库存,取消后库存恢复一致
- 兜底关单、汇率模拟拉取、看板数据

## 十二、项目结构

```
crossmall
├── backend
│   └── src/main/java/com/crossmall
│       ├── common            # 统一返回/异常/UserContext/JWT
│       ├── config            # Web 拦截器/MP 分页/配置项/OpenAPI
│       ├── security          # 认证拦截器(买家/管理员双角色)
│       ├── controller        # 用户端控制器 + mock 支付网关
│       │   └── admin         # 管理端控制器
│       ├── service           # 订单/支付/试算/库存/汇率/关税/履约...
│       ├── fulfillment       # 订单状态机(合法流转表)
│       ├── mq                # 延迟关单 + 轨迹消息(producer/consumer)
│       ├── task              # 汇率定时拉取
│       ├── runner            # 初始密码 BCrypt 升级
│       └── entity / mapper / dto / vo
├── frontend
│   └── src
│       ├── api               # request 封装(双 token)+ mall/admin 接口
│       ├── components        # AppNavbar + admin 面板组件
│       ├── views             # 用户端页面(mall)+ 管理登录/管理台
│       ├── store / router / i18n / utils / assets
├── docker
│   ├── mysql/init            # 01_schema.sql + 02_seed.sql(自动执行)
│   └── rocketmq/broker.conf
├── scripts/smoke_test.py     # 42 项冒烟断言
├── docker-compose.yml
└── README.md
```

## 十三、常见问题 FAQ

**Q1:启动时报端口被占用?**
本项目所有端口均与常见默认值错开(见端口清单)。可用 `netstat -ano | findstr <端口>` 排查占用进程;Broker 监听端口固定 21911(在 `docker/rocketmq/broker.conf` 的 `listenPort`),修改时需同步 compose 映射。

**Q2:修改了 `init/*.sql` 但没有生效?**
MySQL 数据卷 `docker/mysql/data/` 只在**首次启动**时执行初始化脚本。修改种子数据后需:

```bash
docker compose rm -sf mysql && rm -rf docker/mysql/data && docker compose up -d mysql
```

**Q3:为什么要发 `SET NAMES utf8mb4`?**
Windows 下 MySQL 容器初始化会话默认 latin1 客户端字符集,中文种子数据会被双重编码(实测踩坑)。`01_schema.sql` 开头的 `SET NAMES utf8mb4` 解决此问题。

**Q4:金额为什么用「分」存储?**
浮点数无法精确表示 0.1,财务场景不可接受。全链路 BIGINT 分 + 出入口换算,汇率换算用 `BigDecimal` 四舍五入到分。

**Q5:支付/汇率源是模拟的,如何替换为真实实现?**
- 支付:回调协议(验签/幂等/金额校验)已按真实网关标准实现,替换只需在 `controller/mock` 位置实现 Stripe/PayPal 适配器
- 汇率:改写 `FxService.refreshFromSource()` 为调用 openexchangerates 等真实 API,失败时保留旧缓存降级

**Q6:管理端把状态推错了怎么办?**
状态机合法流转表会拒绝非法跳转;若已流转到下一节点,无法回退(符合真实履约语义),退款仅在发货前(PAID 状态)允许。

## 十四、Roadmap

- [ ] 接入 Stripe / PayPal 沙箱,替换模拟收银台
- [ ] 对接真实汇率源(openexchangerates)与失败降级告警
- [ ] 多币种对账模块:日结算单、汇率波动损益报表
- [ ] 物流轨迹订阅(4PX/云途 API)+ 清关异常件识别告警
- [ ] 售后逆向流:退运、部分退款

## 致谢

- 架构与模块划分思路参考 [macrozheng/mall](https://github.com/macrozheng/mall)
- 感谢开源社区:Spring Boot / MyBatis-Plus / Element Plus / RocketMQ

---

<div align="center">

**如果这个项目对你有帮助,欢迎点个 Star ⭐**

</div>
