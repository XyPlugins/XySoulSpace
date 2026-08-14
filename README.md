# XySoulSpace 1.1.10

XySoulSpace 是 XY 系列的灵魂仓库插件，面向 `Paper/Spigot 1.12.2` RPG 服务器。

它由旧版 SoulSpace 重构而来，统一改为 `org.xyplugin.xysoulspace` 包名和 `/xyss` 指令入口，并为后续 `XyForge`、强化、兑换、活动等 XY 系列插件预留 API 与事件。

当前 1.1.10 版本默认使用本地 YML 存储，不要求 XyCore 开启 SQL 数据库。XyForgeCrafting读取材料时建议服务器同时安装XyCore 0.3.12或更高兼容版本。

## 核心功能

- 灵魂空间：远程仓库 GUI，同类物品按完整 ItemStack 模板无限叠加。
- 本地存储：每个玩家独立保存到 `plugins/XySoulSpace/soulspace/<uuid>.yml`。
- 自动拾取：普通事件拾取、范围扫描和 MythicMobs `ssdrops` 共用个人/全局开关；成功入库后才删除地面实体。
- 一键存入：只将主背包和快捷栏物品存入灵魂空间，不处理装备栏、副手或其它客户端槽位。
- 手动存入：打开灵魂空间时点击下方背包物品即可存入，左键1个，右键最多64个，Shift左键存入背包内全部相同物品。
- 快捷分解：按物品 Lore 匹配规则执行服务端命令。
- 物品库：管理员保存手持物品，并发放到玩家灵魂空间。
- 灵魂商店：使用灵魂空间中的材料兑换物品。
- MythicMobs 桥接：兼容旧 SoulSpace 的 `ssdrops` 字段，支持 `xyitems:`、`mythicmobs:`、`minecraft:` 完整物品ID和旧版裸 MythicMobs ID。
- XyCore 生态：检测到 XyCore 时会以 XY 扩展身份运行；灵魂仓库存取、购买、自动拾取等玩家玩法提示优先使用XyCore前缀，管理/帮助/报错提示继续使用本插件前缀。

## 安装

1. 将 `XySoulSpace-1.1.10.jar` 放入服务器 `plugins` 文件夹。
2. 重启服务器生成 `plugins/XySoulSpace/config.yml` 和 `shop.yml`。
3. 给玩家发放权限：

```text
xysoulspace.use
xysoulspace.shop.use
```

管理员默认 OP 可用，也可以单独发放：

```text
xysoulspace.admin
xysoulspace.reload
xysoulspace.shop.admin
```

## 命令

```text
/xyss open                         打开自己的灵魂空间
/xyss store                        将手中物品存入灵魂空间
/xyss pickup <on|off>              开关个人自动拾取
/xyss globalpickup <on|off|status> 管理全局自动拾取
/xyss shop [商店名]                打开灵魂商店
/xyss reloadshop                   重载商店配置
/xyss saveitem <id>                保存手中物品到物品库
/xyss giveitem <id> <玩家> <数量>  发放物品到玩家灵魂空间
/xyss admin <玩家>                 管理员查看玩家灵魂空间
/xyss clear [玩家]                 清空灵魂空间
/xyss reload                       重载插件配置
```

兼容别名：

```text
/xysoulspace
/soulspace
/soul
```

## 本地存储

当前版本使用本地 YML：

```text
plugins/XySoulSpace/soulspace/<uuid>.yml
```

相比旧版，本版本不再每次拾取都强制同步写盘，默认采用：

```yaml
storage:
  type: yaml
  autosave-interval-ticks: 1200
  save-on-deposit: false
```

玩家退出、服务器关闭、定时自动保存时会落盘。

如果你希望每次存入都立即保存，可以开启：

```yaml
storage:
  save-on-deposit: true
```

大服不建议开启，除非你更重视极端断电场景的数据即时性。

## 自动拾取

1.1.10 将普通拾取事件、附近掉落物范围扫描和 MythicMobs `ssdrops` 统一到同一套判断。玩家必须同时满足以下条件才会直接存入灵魂空间：

- 拥有 `xysoulspace.use` 权限。
- `pickup.global-enabled` 为 `true`。
- 玩家个人自动拾取已开启。

玩家可以使用 `/xyss pickup on|off`，也可以在灵魂空间GUI的槽位 `51` 点击自动拾取按钮。按钮有三种状态：个人已开启、个人已关闭、服务器全局停用。管理员通过 `/xyss admin <玩家>` 查看他人仓库时只能看到状态，不能替对方切换。

```yaml
pickup:
  global-enabled: true
  default-player-enabled: true
  range: 6.0
  scan-interval-ticks: 10
  notification-enabled: true
  notification-merge-ticks: 10
  notification-message: "&a已自动拾取 &r%item% &7x%amount% &a进入灵魂仓库"

gui:
  pickup-enabled-button-material: EMERALD
  pickup-disabled-button-material: REDSTONE
  pickup-global-disabled-button-material: BARRIER
```

`range` 是X/Y/Z三个方向的扫描半径，运行时限制在 `0.5-64`；扫描任务全服只创建一个，不会为每名玩家创建独立定时任务。事件拾取和范围扫描不会重建物品，会保留NBT、品质、随机属性和业务Lore；仅继续清理旧版本遗留的 `灵魂数量/Key/左键取右键取` 内部GUI提示行。OP与显式拥有 `xysoulspace.use` 的玩家使用相同判断，但普通玩家仍需实际获得该权限。

安全顺序固定为：先确认完整 ItemStack 成功写入仓库，再删除地面实体并发送提示。仓库拒绝写入或执行失败时，原地面物品会保留；MythicMobs `ssdrops` 无法直接入库时则在怪物死亡位置生成掉落物。相同玩家、相同物品在 `notification-merge-ticks` 窗口内会合并数量，避免连续掉落刷屏。

自动拾取成功提示属于玩家玩法消息：检测到兼容的 XyCore 时使用 XyCore 的玩家前缀，未安装或未启用 XyCore 时回退到 XySoulSpace 前缀。`%item%` 和 `%item_name%` 为实际 ItemStack 显示名，`%item_id%` 为完整物品ID，`%amount%` 为本次合并后的数量。

执行 `/xyss reload` 会重新读取配置、物品库与商店，刷新XyCore桥接，重新缓存 MythicMobs `ssdrops` 规则，并按新的扫描间隔和自动保存间隔重启各自唯一任务。已有玩家的个人开关保存在其灵魂空间YML中，不会因重载被覆盖。

从旧版本升级时可以继续使用原 `config.yml`，缺少的新键会使用上述默认值。旧的 `integrations.mythicmobs.pickup-message`、`pickup.message-enabled` 和 `pickup.message` 已不再读取；需要自定义统一提示时，请改用 `pickup.notification-*`。

## 物品匹配规则

灵魂空间将物品的完整 `ItemStack` 模板作为同类判断依据，并忽略数量。

这意味着以下内容不同会被视为不同物品：

- 材质
- 耐久
- 显示名
- Lore
- 附魔
- NBT/序列化元数据

商店消耗支持两种键：

```yaml
costs:
  DIAMOND: 1
  "十年斗环(孤竹)": 2
```

如果键是材质名，会匹配材料；否则会匹配物品去色后的显示名。

## GUI展示与真实物品隔离

灵魂空间GUI中的物品只是展示副本。1.1.4开始，取出物品时不再依赖可见Lore里的 `Key` 字段，而是使用玩家当前打开GUI时记录在内存中的槽位映射。

默认配置为：

```yaml
gui:
  show-amount-lore: true
  show-action-lore: true
  show-key-lore: false
  pickup-enabled-button-material: EMERALD
  pickup-disabled-button-material: REDSTONE
  pickup-global-disabled-button-material: BARRIER
```

这意味着：

- GUI里可以显示“灵魂数量”方便玩家查看。
- 默认显示“左键/右键/Shift左键”取出提示，但它只存在于GUI展示副本，不会写入真实物品Lore。
- 默认不显示内部 `Key`，避免玩家看到无意义的内部识别内容。
- 如果旧版本已经把这些内部Lore存入过灵魂空间，后续重新存入时会自动清理 `灵魂数量`、`Key`、`左键取/右键取` 等内部展示行。
- 一键存入只读取玩家背包槽位 `0-35`，不会读取穿着的装备、副手45号槽位或其它DragonCore映射槽位。
- 底栏槽位 `51` 是个人自动拾取开关；管理员查看他人仓库时为只读，全局停用时显示独立状态。
- GUI使用内部会话身份而不是可配置标题判断；修改 `gui.title` 不会使槽位保护失效。拖拽到上方仓库区域会被拒绝，管理员查看他人仓库时整个界面只允许翻页和关闭。

## 玩家消息中的物品显示

1.1.6继续优化了玩家消息中的原版物品名称显示：

```yaml
messages:
  item-name-mode: "id"
  vanilla-names:
    BEACON: "信标"
    IRON_INGOT: "铁锭"
```

可选值：

- `id`：默认。`XyItems/MythicMobs` 等自定义物品显示完整物品ID；原版 `minecraft` 物品优先显示 `vanilla-names` 覆盖名或插件内置中文名，例如 `BEACON -> 信标`。
- `raw-id`：严格显示完整ID，例如 `minecraft:BEACON`，主要用于调试。
- `name` / `display`：优先显示物品自定义显示名；没有自定义显示名时，再显示原版中文名或完整ID。

存入、取出、自动拾取、MythicMobs桥接拾取和灵魂商店购买提示都会使用这个规则。

说明：原版物品的“信标”“铁锭”等名字来自玩家客户端语言包，Bukkit 1.12.2服务端默认只能拿到 `BEACON` 这类材质枚举。因此插件内置了 463 个 1.12.2 基础 `Material` 中文名；`vanilla-names` 只用于覆盖你想自定义的叫法。

## 灵魂商店

配置文件：

```text
plugins/XySoulSpace/shop.yml
```

示例：

```yaml
shops:
  默认:
    size: 54
    items:
      diamond:
        name: "&b钻石"
        material: DIAMOND
        amount: 1
        slot: 10
        costs:
          LOG: 10
```

玩家左键购买 1 次，右键购买 64 次。

## 快捷分解

配置位于 `config.yml`：

```yaml
quick-decompose:
  enabled: true
  items:
    - lore: "分解可获得中阶斗兽晶核"
      commands:
        - "mm i give %player% 中阶斗兽晶核 1"
    - lore:
        - "十年斗环"
        - "百年斗环"
      commands:
        - "points give %player% 200"
```

匹配到 Lore 后，插件会从灵魂空间扣除该物品并执行命令。

## MythicMobs ssdrops

如果安装了兼容的 MythicMobs，XySoulSpace 会注册死亡事件桥接，并读取：

```text
plugins/MythicMobs/Mobs/**/*.yml
```

示例：

```yaml
ZombieKing:
  Type: ZOMBIE
  Display: "&c僵尸王"
  ssdrops:
    - "xyitems:chiyamopo 1 100%"
    - "mythicmobs:ForgingCrystal 2 0.25"
    - "minecraft:IRON_INGOT 4 80%"
    - "LegacyMythicItemId 1 0.1"
```

每行格式：

```text
物品ID 数量 概率
```

推荐使用完整物品ID：`xyitems:id`、`mythicmobs:id` 或 `minecraft:MATERIAL`。安装XyCore时统一通过其物品库创建；独立运行时 `mythicmobs:` 直接回退MM物品库，`minecraft:` 直接回退Bukkit材质。`xyitems:` 需要XyCore与XyItems已正常加载。旧版裸ID仍会先查 `plugins/XySoulSpace/itemlibrary/items.yml`，找不到再尝试从 MythicMobs 物品库生成。

概率只接受 `0-1` 小数或 `0%-100%` 百分数。数量必须为正整数；格式错误、`NaN` 或超范围规则会在重载时被忽略并记录后台警告，不会按100%意外掉落。

`ssdrops` 规则只在插件启动和 `/xyss reload` 时扫描并缓存。怪物死亡时只查询内存快照，不会在掉落热路径反复遍历 MythicMobs 配置文件。击杀者满足统一自动拾取条件时物品直接进入灵魂空间；个人或全局开关关闭、权限不足或入库失败时，物品会掉落在怪物死亡位置。

## 给其他 XY 插件调用

其他插件可以通过静态 API 调用：

```java
XySoulSpaceApi api = XySoulSpace.get();
api.deposit(player, itemStack);
api.getAmountByCostKey(player.getUniqueId(), "DIAMOND");
api.removeByCostKey(player.getUniqueId(), "DIAMOND", 10);
```

`getAmountByCostKey` 与 `removeByCostKey` 是给旧商店配置保留的材质/去色名称兼容接口。新锻造、强化和兑换功能必须使用完整物品ID接口：

```java
Map<String, Long> requirements = new LinkedHashMap<>();
requirements.put("mythicmobs:ForgingCrystal", 1L);
requirements.put("xyitems:forge_crystal", 8L);
requirements.put("minecraft:IRON_INGOT", 16L);

long owned = api.getAmountByItemId(player.getUniqueId(), "xyitems:forge_crystal");
boolean enough = api.hasItems(player.getUniqueId(), requirements);
Optional<SoulSpaceWithdrawal> receipt = api.withdrawItems(player.getUniqueId(), requirements);

// 中途失败时完整返还；正常锻造失败可按配方比例返还。
receipt.ifPresent(value -> api.refund(player.getUniqueId(), value, 100));
```

完整ID匹配统一调用XyCore 0.3.10的物品库规则，不读取显示名称或Lore。`withdrawItems` 会先为全部材料建立扣除计划：只要任一材料不足，返回空结果且仓库内容完全不变；成功时返回包含实际物品模板和数量的退款凭据。

退款百分比使用向下取整。例如只扣除了1个物品而退款比例为50%，该部分返还0个，避免凭空增加物品。

也可以监听：

```java
XySoulSpaceItemDepositEvent
```

后续锻造、强化、兑换插件建议优先通过 API 消耗灵魂空间材料，避免直接读写 YML。

## 玩家消息前缀

XySoulSpace 是可独立使用的插件，`plugin.yml` 中的 XyCore 只是软依赖。前缀规则为：

- 存入、取出、自动拾取、商店购买、材料不足、分解等玩家玩法提示：已安装并启用 XyCore 0.3.12+ 时优先读取 `plugins/XyCore/config.yml -> messages.prefix`。
- help、reload、reloadshop、globalpickup、giveitem、saveitem、admin、clear 的用法、权限和管理反馈：使用本插件 `config.yml -> messages.prefix`。
- 未安装 XyCore、XyCore未启用或旧Core没有前缀API：玩家玩法提示也回退到本插件 `messages.prefix`。

控制台日志和后台输出继续保留XySoulSpace自己的插件名，不使用统一玩家前缀。

## 版本记录

### 1.1.10

- GUI底栏槽位 `51` 新增个人自动拾取按钮，区分个人开启、个人关闭和全局停用；管理员查看他人仓库时为只读。
- 普通拾取事件、范围扫描和 MythicMobs `ssdrops` 统一检查全局开关、个人开关及 `xysoulspace.use` 权限。
- 地面物品只在确认成功入库后删除；入库失败保留地面实体，`ssdrops` 失败或关闭时回退到怪物死亡位置。
- 自动拾取提示使用实际物品显示名，兼容 MythicMobs/XyItems 品质物品，并可在短时间窗口内合并同物品数量。
- `ssdrops` 支持 `xyitems:`、`mythicmobs:`、`minecraft:` 完整ID；旧版裸 MythicMobs ID继续兼容。
- MythicMobs配置只在启动与重载时缓存，自动拾取继续复用一个全服扫描任务，不增加每玩家任务。
- GUI改用内部会话身份并封锁拖拽，修改标题也不会失去槽位保护；管理员他人视图全程只读。
- 异步自动保存使用一致快照与修订号，写盘期间的新拾取不会被错误标记为已保存；并发YML写入会串行执行。
- `/xyss reload` 会刷新桥接、重建 `ssdrops` 缓存，并使用新的扫描与自动保存间隔重启任务。

### 1.1.9

- 修复Shift左键只存入当前点击堆叠、没有存入背包内其它相同物品的问题。
- Shift左键现在只扫描主背包与快捷栏 `0-35`，按完整仓库存储键一次存入全部相同物品。
- 材质、耐久、名称、Lore、附魔或NBT不同的物品仍然分别保存，不会因材质相同而误合并。
- 玩家背包点击判断改为当前GUI的底部背包对象，提升Paper/Spigot 1.12.2兼容性。
- 批量操作只在玩家主动Shift点击时扫描固定36格，并只执行一次仓库写入、保存与界面刷新。

### 1.1.8

- 修复下方背包Shift左键存入整组可能无效的问题。
- 手动存入现在会在下一tick重新确认槽位物品后再扣除和写入灵魂空间，减少1.12.2取消点击事件造成的同步覆盖。
- 玩家提示消息末尾中文句号会自动清理，默认配置中的句号也已去掉。
- 不新增常驻扫描任务，不改变已有灵魂空间数据格式。

### 1.1.7

- 修复灵魂空间打开时下方玩家背包点击被完全锁定，导致只能使用“一键存入”的问题。
- 新增下方背包点击存入：左键存入1个，右键存入最多64个，Shift左键存入整组。
- 恢复GUI物品取出操作提示Lore，默认显示左键/右键/Shift左键说明；该Lore只存在于GUI展示副本，不会污染真实物品。
- 调整玩家消息物品名兜底顺序：完整ID识别失败时优先显示物品自定义名，再显示原版中文名，避免“初墨之锋”被显示为“钻石剑”。

### 1.1.6

- 将原版物品中文名fallback从少量常用表扩展为 463 个 Bukkit 1.12.2 基础 `Material`。
- 新增独立 `VanillaMaterialNames` 工具类，启动时一次性创建只读Map，运行时只做单次内存查询。
- `messages.vanilla-names` 继续作为覆盖表，默认无需服主手动翻译全部原版物品。
- 不改变 XyItems/MythicMobs 完整ID显示规则，也不改变灵魂空间存储结构。

### 1.1.5

- 修复原版物品没有自定义显示名时，玩家聊天提示只能显示 `minecraft:BEACON` 这类ID的问题。
- 新增 `messages.vanilla-names`，可为原版材质配置玩家提示显示名。
- `messages.item-name-mode: id` 现在对 XyItems/MythicMobs 继续显示完整ID，对原版minecraft物品优先显示中文名。
- 新增 `raw-id` 模式，保留严格显示完整ID的调试能力。

### 1.1.4

- 修复灵魂空间“一键存入”会把穿着装备、副手或其它槽位物品一起存入的问题。
- 一键存入现在只处理玩家主背包和快捷栏 `0-35`。
- 修复取出物品时把GUI展示Lore写入真实物品的问题。
- GUI内部 `Key` 与点击操作提示默认不再显示，也不会参与取出识别。
- 新增 `messages.item-name-mode`，玩家消息可默认显示完整物品ID，避免显示 `LEATHER_HELMET` 这类原始材质名造成误解。
- 旧的GUI内部Lore在后续重新存入时会自动清理。

### 1.1.3

- 按服主最终确认调整前缀语义：玩家玩法提示走 XyCore `messages.prefix`。
- 管理、帮助、权限、用法和后台定位相关提示保留 XySoulSpace 本地前缀。
- 保持 XyCore 软依赖，没有 XyCore 时继续独立运行。

### 1.1.2

- 玩家聊天提示前缀优先读取XyCore `messages.prefix`。
- 未安装或未启用XyCore时，继续使用XySoulSpace本地 `messages.prefix` 独立运行。
- 控制台日志保持XySoulSpace插件名。

### 1.1.1

- 新增按完整 `provider:item` ID统计材料的 `getAmountByItemId`。
- 新增批量 `hasItems` 与原子 `withdrawItems`，材料不足时不再发生部分扣除。
- 新增 `SoulSpaceWithdrawal`精确凭据与百分比退款接口。
- XyForgeCrafting默认先统计和扣除灵魂仓库，不足部分再读取玩家背包。
- 新接口统一使用XyCore匹配规则；旧 `CostKey` 接口只为已有商店流程保留。

### 1.1

- 从旧版 SoulSpace JAR 重构为 `XySoulSpace`。
- 包名改为 `org.xyplugin.xysoulspace`。
- 主命令改为 `/xyss`。
- 默认本地 YML 存储，保留后续 XyCore/SQL 切换空间。
- 改进保存策略：内存缓存、脏标记、定时保存、退出保存。
- 新增 XySoulSpace API 和物品存入事件。
- 重写灵魂空间 GUI、自动拾取、物品库、基础商店和快捷分解。
- 外部依赖改为软桥接，未安装 XyCore/MythicMobs/Vault/PlayerPoints 时不应阻止核心功能启动。
- 兼容旧 SoulSpace `ssdrops` 写法。

## 构建

```text
gradlew.bat clean build
```

输出：

```text
build/libs/XySoulSpace-1.1.10.jar
```
