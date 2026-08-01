# XySoulSpace 1.1.3

XySoulSpace 是 XY 系列的灵魂仓库插件，面向 `Paper/Spigot 1.12.2` RPG 服务器。

它由旧版 SoulSpace 重构而来，统一改为 `org.xyplugin.xysoulspace` 包名和 `/xyss` 指令入口，并为后续 `XyForge`、强化、兑换、活动等 XY 系列插件预留 API 与事件。

当前 1.1.3 版本默认使用本地 YML 存储，不要求 XyCore 开启 SQL 数据库。XyForgeCrafting读取材料时建议服务器同时安装XyCore 0.3.12或更高兼容版本。

## 核心功能

- 灵魂空间：远程仓库 GUI，同类物品按完整 ItemStack 模板无限叠加。
- 本地存储：每个玩家独立保存到 `plugins/XySoulSpace/soulspace/<uuid>.yml`。
- 自动拾取：支持事件拾取和范围扫描，个人/全局开关可控。
- 一键存入：将背包物品存入灵魂空间。
- 快捷分解：按物品 Lore 匹配规则执行服务端命令。
- 物品库：管理员保存手持物品，并发放到玩家灵魂空间。
- 灵魂商店：使用灵魂空间中的材料兑换物品。
- MythicMobs 桥接：兼容旧 SoulSpace 的 `ssdrops` 字段，支持物品库 ID 或 MythicMobs 物品 ID。
- XyCore 生态：检测到 XyCore 时会以 XY 扩展身份运行；灵魂仓库存取、购买、自动拾取等玩家玩法提示优先使用XyCore前缀，管理/帮助/报错提示继续使用本插件前缀。

## 安装

1. 将 `XySoulSpace-1.1.3.jar` 放入服务器 `plugins` 文件夹。
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
    - "中阶斗兽晶核 1 100%"
    - "DragonSword 1 0.25"
```

每行格式：

```text
物品ID 数量 概率
```

物品 ID 会先查 `plugins/XySoulSpace/itemlibrary/items.yml`，找不到再尝试从 MythicMobs 物品库生成。

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
build/libs/XySoulSpace-1.1.3.jar
```
