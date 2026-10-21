# Java 服务端

基于 Java 17、Spring Boot、Spring Data JPA。默认使用持久化 H2，生产环境可通过环境变量切换 MySQL。

## 启动配置

至少配置以下环境变量：

```bash
export WECHAT_APP_ID="小程序AppID"
export WECHAT_APP_SECRET="小程序AppSecret"
export WECHAT_VIRTUAL_PAY_OFFER_ID="虚拟支付OfferID"
export WECHAT_VIRTUAL_PAY_APP_KEY="虚拟支付AppKey"
export WECHAT_CALLBACK_TOKEN="消息推送Token"
export JWT_SECRET="至少32字节的随机密钥"
export SESSION_KEY_ENCRYPTION_SECRET="独立随机加密密钥"
```

也可以不用环境变量：把配置写入 `server/config/app.properties`（模板见 `config/app.properties.example`，键名与环境变量一致），服务启动时通过 `spring.config.import` 自动加载。jar 部署时放在 jar 同目录的 `config/app.properties`，或 `/etc/document-store/app.properties`。该文件已被 `.gitignore` 忽略，环境变量与命令行参数仍可覆盖文件值。

沙箱环境使用 `WECHAT_VIRTUAL_PAY_ENV=1`，正式环境使用 `0`。价格通过 `DOCUMENT_PRICE_FEN` 配置，单位为分，默认 100。

将真实 `res.csv` 放入 `src/main/resources/res.csv`，或设置 `CATALOG_CSV_LOCATION=file:/绝对路径/res.csv`。列顺序为：序号、备注、中文名称、图片数量、价格、网盘链接。价格为元，例如 `1.49`，启动时按四舍五入转换为分后入库。服务每次启动都会按序号幂等更新数据库，并清空首页分页缓存。

每份文档可以不同价格。`DOCUMENT_PRICE_FEN` 仅在 CSV 价格为空或无法解析时作为兜底价。

将缩略图与详情图使用同一张图片，放入服务器 `./images` 目录，文件名用文档序号，扩展名支持 `jpg`、`jpeg`、`png`、`webp`、`gif`、`bmp`，例如 `images/1.jpg`、`images/2.jpeg`、`images/21.png`。服务启动与每次 CSV 导入后会重建图片索引，接口按序号返回真实扩展名的地址；同一个序号有多种格式时按 jpg、jpeg、png、webp、gif、bmp 的优先级取用。可通过 `IMAGE_DIRECTORY` 修改目录，通过 `IMAGE_URL_PREFIX` 修改访问路径，默认 `/images`。图片由服务端作为静态资源返回，不需要云存储或 CDN。

启动：

```bash
mvn spring-boot:run
```

看广告免费下载的可选配置：

```bash
export AD_FREE_DOWNLOAD_MAX_PRICE_FEN=50
export AD_QUOTA_TIME_ZONE="Asia/Shanghai"
```

生产 MySQL 示例：

```bash
export DB_URL="jdbc:mysql://127.0.0.1:3306/document_store?useUnicode=true&characterEncoding=utf8&serverTimezone=UTC"
export DB_USERNAME="document_store"
export DB_PASSWORD="强密码"
```

MySQL 建表脚本见 `db/mysql-schema.sql`，包含四张表与所需唯一约束（每人每天一次广告下载依赖 `ad_download_usages` 的 `(user_id, usage_date)` 唯一索引）。执行脚本后可将 `spring.jpa.hibernate.ddl-auto` 设为 `validate`。

## 接口

- `POST /api/auth/wechat/login`：使用 `wx.login` 的 code 登录，保存加密后的 `session_key`。
- `GET/PUT /api/users/me`：读取或更新用户昵称、头像。
- `GET /api/documents?page=0&size=20&keyword=城市`：分页获取文档列表，包含 `priceFen`（单位：分），不返回网盘链接。`keyword` 按标题不区分大小写模糊匹配。商品基本不变，结果由 Caffeine 缓存，缓存键为 `keyword:page:size`。
- `GET /api/documents/{id}`：获取文档详情，包含 `priceFen`，不返回网盘链接。
- `GET /api/free-downloads/{documentId}/link`：限时免费下载，需要登录，返回 `{"status","documentId","downloadLink"}`。每人每天一次，超额返回 429 `FREE_DOWNLOAD_QUOTA_EXHAUSTED`；开关 `FREE_DOWNLOAD_ENABLED=false` 时返回 503。
- `GET /api/free-downloads/today`：查询今日限时免费配额，需要登录，返回 `{"date","limit","used","remaining"}`。
- `GET /images/{序号}.jpg`：服务器本地图片，缩略图与详情图共用同一张图片。
- `POST /api/mini-program/virtual-payment/orders`：创建订单并返回 `signData`、`paySig`、`signature`、`mode`。（小程序当前不调用，实现保留。）
- （当前未调用）`POST /api/mini-program/ad-downloads`：看广告免费下载解锁，请求体 `{"documentId":"2"}`，仅对价格不高于 `AD_FREE_DOWNLOAD_MAX_PRICE_FEN` 的文档生效，返回网盘链接。
- `GET /api/mini-program/ad-downloads/today`：返回今日免费下载配额 `{"date","limit","used","remaining"}`。
- `GET /api/mini-program/virtual-payment/orders/{outTradeNo}/download-link`：仅订单回调确认为 `PAID` 后返回链接。
- `GET/POST /api/mini-program/virtual-payment/callback`：微信发货推送 URL 校验和支付通知。

除登录、文档和回调接口外，请求需要 `Authorization: Bearer <JWT>`。

## 微信后台配置

1. 在虚拟支付后台登记每个 CSV 序号对应的 `productId`，登记价格必须与该文档在 CSV 中的价格一致。当前 `res.csv` 使用的价格档位为 0.5、0.99、1.29、1.49、1.99、2.99、4.99 元，共 7 档，需在后台分别登记。
2. 将发货推送 URL 配置为 `https://你的域名/api/mini-program/virtual-payment/callback`。
3. 推送 Token 必须与 `WECHAT_CALLBACK_TOKEN` 一致。本实现接收明文 JSON/XML 推送；如后台启用安全模式，需要额外实现消息体 AES 解密。
4. 不能将小程序端 `success` 回调视为支付凭证。服务端只在收到并验证 `xpay_goods_deliver_notify` 后将订单标记为已支付。
5. 正式上线前确认小程序主体、类目、虚拟商品模式及 `mode` 值符合微信平台当前准入要求。

## 部署注意

- `/images/**` 为公开静态资源，任何人拿到完整 URL 都能下载，不能用于存放未授权可访问的内容。
- 首页分页缓存默认 3600 秒、最多 200 个分页结果。修改 `res.csv` 后重启服务会重新导入并清空缓存。
- 限时免费下载按 `app_users.id` 与当天日期记录，唯一约束 `(user_id, usage_date)` 保证每人每天一次，日期按 `FREE_QUOTA_TIME_ZONE`（默认 `Asia/Shanghai`）计算，零点重置。
- 看广告免费下载由客户端在广告播放完成后调用，服务端只能校验价格门槛与每日一次配额，无法证明广告真的被完整观看。如需更强约束，需要在广告平台侧接入服务端回调或自行加密凭证。
