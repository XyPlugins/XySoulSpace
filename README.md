# XySoulSpace 1.1

XySoulSpace 是 XY 系列的灵魂仓库插件，面向 `Paper/Spigot 1.12.2` RPG 服务器。

它由旧版 SoulSpace 重构而来，统一改为 `org.xyplugin.xysoulspace` 包名和 `/xyss` 指令入口，并为后续 `XyForge`、强化、兑换、活动等 XY 系列插件预留 API 与事件。

当前 1.1 版本默认使用本地 YML 存储，不要求 XyCore 开启 SQL 数据库。

## 核心功能

- 灵魂空间：远程仓库 GUI，同类物品按完整 ItemStack 模板无限叠加。
- 本地存储：每个玩家独立保存到 `plugins/XySoulSpace/soulspace/<uuid>.yml`。
- 自动拾取：支持事件拾取和范围扫描，个人/全局开关可控。
- 一键存入：将背包物品存入灵魂空间。
- 快捷分解：按物品 Lore 匹配规则执行服务端命令。
- 物品库：管理员保存手持物品，并发放到玩家灵魂空间。
- 灵魂商店：使用灵魂空间中的材料兑换物品。
- MythicMobs 桥接：兼容旧 SoulSpace 的 `ssdrops` 字段，支持物品库 ID 或 MythicMobs 物品 ID。
- XyCore 生态：检测到 XyCore 时会以 XY 扩展身份运行，后续可切换到统一数据服务。

## 安装

1. 将 `XySoulSpace-1.1.jar` 放入服务器 `plugins` 文件夹。
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

也可以监听：

```java
XySoulSpaceItemDepositEvent
```

后续锻造、强化、兑换插件建议优先通过 API 消耗灵魂空间材料，避免直接读写 YML。

## 版本记录

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
build/libs/XySoulSpace-1.1.jar
```
