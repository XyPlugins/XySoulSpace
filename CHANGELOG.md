# XySoulSpace 更新说明

## 1.1.12 - 2026-08-19

- 自动拾取现在仅处理 MythicMobs `MythicMobDeathEvent` 提供的最终掉落清单与既有 `ssdrops`。
- 删除普通 `EntityDeathEvent` 与 `ItemSpawnEvent` 掉落关联，不再扫描玩家附近的普通地面物品。
- 玩家丢弃物、普通生物掉落、自然生成物品和其它无归属Item实体继续使用原版拾取逻辑。
- `PlayerPickupItemEvent` 只对已登记的MM击杀归属实体执行保护或入库，避免误接管无关物品。
- 保留MM掉落约0.5秒展示、击杀者归属保护、入库失败保留、每tick处理预算、消息合并及完整ItemStack/NBT。
- 运行时仅保留一个全服共享的到期归属队列，不创建逐玩家或逐掉落任务。
- 默认配置删除已停用的 `pickup.range` 与 `pickup.scan-interval-ticks`；旧服配置中的同名键会被忽略。
- 版本号更新为 `1.1.12`，同步更新默认配置、README、更新说明和AI记录。

## 1.1.11 - 2026-08-15

- 修复灵魂空间取出提示将带自定义Name的XyItems物品显示成 `xyitems:id` 的问题。
- `messages.item-name-mode: id/name/display` 现在统一优先显示ItemStack实际Name；没有Name的自定义物品才回退完整ID，原版物品回退中文名。
- `raw-id` 模式语义保持不变，仍可用于严格查看完整物品库ID。
- 手动存入、取出、灵魂商店与自动拾取的玩家提示现在使用一致的Name优先规则。
- 修复玩家A击杀怪物并离开后，附近玩家B可能自动拾取该掉落的问题；怪物死亡清单与实际地面Item现在会绑定原击杀者。
- 原版怪物掉落、MythicMobs普通Drops、XyItems桥接Drops和 `ssdrops` 默认先在地面展示10 tick，再无视距离进入击杀者仓库。
- 归属期阻止其他玩家、漏斗、生物以及不同归属物品合并；击杀者不可用或入库失败时只解除归属并保留地面实体。
- 新增 `pickup.mob-drop-delay-ticks`，支持1-200 tick，旧配置缺少该键时使用默认值10。
- 归属匹配完全由死亡/生成事件驱动，到期处理复用一个全服共享优先队列，不创建每玩家或每掉落任务。
- 新增 `pickup.max-owned-pickups-per-tick`，默认每tick最多入库32件，超额到期物品顺延，避免500人同时刷怪产生单tick处理尖峰。
- 取消的死亡/生成事件不会留下可误匹配的归属批次；插件真正停用时会解除尚未完成的实体保护并恢复正常拾取。
- `ssdrops` 生成事件被其它合并/清理插件消费时不再二次回退生成，避免跨插件互操作造成双倍掉落。
- 将存储键SHA-256的十六进制转换改为等价的查表实现，保持Key结果不变并减少批量入库时的格式化对象。
- 新增十一项纯逻辑回归测试，其中四项覆盖物品名选择，六项覆盖死亡掉落匹配/队列/积压保护，一项验证优化前后的标准SHA-256输出；当前共16项测试。
- 不改变灵魂仓库YML、ItemStack/NBT、公开API、`ssdrops` 配置格式或软依赖关系。
- 版本号更新为 `1.1.11`，同步更新默认配置、README、更新说明和AI记录。

## 1.1.10 - 2026-08-14

- 灵魂空间GUI底栏槽位 `51` 新增个人自动拾取按钮，显示个人开启、个人关闭、全局停用三种状态；管理员查看他人仓库时整个界面只允许翻页和关闭。
- 普通地面拾取事件、附近掉落物范围扫描和 MythicMobs `ssdrops` 统一检查 `pickup.global-enabled`、玩家个人开关及 `xysoulspace.use` 权限。
- 调整自动拾取事务顺序：完整物品成功写入灵魂空间后才删除地面实体；写入失败时保留原实体，`ssdrops` 未入库时回退到怪物死亡位置。
- 自动拾取提示改为读取实际 ItemStack 显示名，兼容 MythicMobs、XyItems 品质名称和颜色；检测到XyCore时使用统一玩家前缀，独立运行时回退到XySoulSpace前缀。
- 新增 `pickup.notification-enabled`、`notification-merge-ticks` 和 `notification-message`，相同玩家、相同物品可在短窗口内合并数量，减少连续掉落刷屏。
- 默认配置移除旧的MM独立拾取提示；旧服配置仍可保留，但 `integrations.mythicmobs.pickup-message`、`pickup.message-enabled` 与 `pickup.message` 不再读取。
- MythicMobs `ssdrops` 支持 `xyitems:id`、`mythicmobs:id`、`minecraft:MATERIAL` 完整ID，并继续兼容旧版裸 MythicMobs ID；无XyCore时MM和原版ID有直接回退。
- 无效数量、无效概率、`NaN` 和超范围概率改为忽略并记录后台警告，不再静默按100%掉落；0%与100%边界严格生效。
- `ssdrops` 规则改为仅在启动与 `/xyss reload` 时扫描并缓存；怪物死亡热路径只读内存快照。
- GUI身份不再依赖可配置标题，增加拖拽事件封锁和窗口内会话数据，修复自定义标题、翻页关闭事件可能导致的槽位保护/映射失效。
- 自动保存改用一致快照、修订号条件清理和串行YML写入，避免写盘期间的新自动拾取被误标记为已保存。
- `/xyss reload` 现在同步刷新XyCore桥接和MM规则缓存，并按最新扫描间隔与自动保存间隔重启唯一任务。
- 自动拾取继续使用单个全服定时任务，不为玩家创建独立任务；同一批入库最多刷新一次GUI。
- 自动拾取扫描半径运行时最高限制为64格，OP与显式权限玩家使用一致的自动拾取判断。
- 版本号统一更新为 `1.1.10`，同步更新默认配置、README、更新说明和AI记录。

## 1.1.9 - 2026-08-05

- 修复灵魂空间GUI中下方背包 `Shift+左键` 只存入当前点击堆叠的问题；现在会存入主背包与快捷栏 `0-35` 中全部相同物品。
- 同类判断使用灵魂空间现有的完整物品存储键，会忽略旧版内部GUI提示Lore，但不会合并不同耐久、名称、Lore、附魔、NBT或随机属性的物品。
- 下方背包识别改为当前GUI的底部背包对象，避免依赖CraftInventory包装对象引用。
- 批量存入只在主动点击时扫描固定36格，汇总后仅执行一次存入、保存、消息与GUI刷新，不新增常驻任务。

## 1.1.8 - 2026-08-05

- 修复灵魂空间GUI中下方背包 `Shift+左键` 存入整组可能不生效的问题。
- 手动存入改为点击事件取消后下一tick执行实际扣除、存入、保存和GUI刷新，避免Paper/Spigot 1.12.2在事件结束时覆盖背包同步。
- 新增单玩家短暂处理锁，防止同一tick连续点击造成重复存入或客户端残影。
- 玩家提示消息发送时会自动去掉末尾中文句号；默认配置中的玩家提示也同步去掉句号。
- 不新增常驻扫描任务，不改变灵魂空间YML数据结构和API事务接口。

## 1.1.7 - 2026-08-05

- 修复灵魂空间GUI打开时下方玩家背包点击被取消，导致无法手动存入、只能一键存入的问题。
- 新增下方背包点击存入规则：左键存入1个，右键存入最多64个/一组，Shift左键存入整组。
- 上方仓库展示槽仍然锁定，空鼠标点击才执行取出，避免展示物品或按钮被拿走。
- 恢复GUI展示物品的取出操作提示Lore；提示只写在GUI展示副本，不会写入真实取出的物品。
- 玩家消息中的物品名兜底顺序调整为：XyCore完整ID识别成功时显示完整ID；若识别结果为minecraft且物品有自定义显示名，则显示自定义名；最后才显示原版中文名。
- 修复XyItems/MM身份未被识别时，带自定义名的武器被提示成“钻石剑”等原版材质名的问题。

## 1.1.6 - 2026-08-04

- 将原版物品中文名fallback从少量常用表扩展为完整 Bukkit 1.12.2 基础 `Material` 表，共 463 项。
- 新增 `VanillaMaterialNames` 工具类，中文名表在插件类加载时创建为只读Map，玩家消息只做一次内存查询。
- `messages.vanilla-names` 继续保留为覆盖表，服主只需要配置想改名的原版物品，不需要手动翻译全部物品。
- 自定义物品仍优先显示 `xyitems:xxx` / `mythicmobs:xxx` 完整ID；本次只扩展原版 `minecraft` 物品的玩家提示显示。
- 不改变灵魂空间YML结构、不新增扫描任务、不影响自动拾取和材料事务接口。

## 1.1.5 - 2026-08-04

- 修复原版物品没有自定义显示名时，玩家聊天提示只能显示 `minecraft:BEACON` 这类完整ID的问题。
- 新增 `messages.vanilla-names` 配置，可为原版 `Material` 指定玩家提示显示名，例如 `BEACON: "信标"`。
- 调整 `messages.item-name-mode: id` 的玩家提示语义：XyItems/MythicMobs等自定义物品继续显示完整ID，原版minecraft物品优先显示中文名映射。
- 新增 `raw-id` 模式，用于需要严格显示 `minecraft:BEACON` 这类完整ID的调试场景。
- 内置少量常用1.12.2原版物品中文名fallback；配置中的 `vanilla-names` 优先级更高。

## 1.1.4 - 2026-08-04

- 修复GUI“一键存入”读取整包内容时可能把穿戴装备、副手或其它客户端槽位一起存入灵魂空间的问题。
- 一键存入现在只遍历玩家主背包与快捷栏 `0-35`，不再调用可能包含扩展槽位的整包数组。
- 修复取出物品时使用GUI展示物品导致 `灵魂数量`、`Key`、`左键取/右键取` 等内部Lore污染真实物品的问题。
- GUI点击取出改为内存槽位映射，不再依赖可见Lore中的内部Key；关闭GUI后映射立即清理。
- 新增 `gui.show-amount-lore`、`gui.show-action-lore`、`gui.show-key-lore`，默认只显示数量，不显示内部Key和操作提示。
- 新增 `messages.item-name-mode`，玩家提示可默认显示 `xyitems:xxx`、`mythicmobs:xxx`、`minecraft:xxx` 形式的完整物品ID。
- 存入时会清理旧版本遗留的内部GUI Lore，避免同一物品因展示Lore不同被拆成多个仓库条目。
- `plugin.yml` 软依赖补充 XyItems，便于启动时通过 XyCore 物品库识别 XyItems 完整ID。

## 1.1.3 - 2026-08-02

- 按服主最终确认调整前缀语义：存入、取出、购买成功、自动拾取、材料不足、分解等玩家玩法提示走 XyCore `messages.prefix`。
- `/xyss help/reload/reloadshop/globalpickup/giveitem/saveitem/admin/clear` 的权限、用法、玩家不在线和管理反馈保留 XySoulSpace 本地前缀。
- 新增 `Text.sendLocal/sendLocalRaw`，避免玩家执行管理命令时被误显示为统一系统提示。
- 保持 XyCore 软依赖；没有 XyCore 时玩家玩法提示仍回退到 XySoulSpace `messages.prefix`。

## 1.1.2 - 2026-08-02

- 玩家聊天提示前缀改为优先读取 `XyCoreApi#getMessagePrefix()`，检测到XyCore 0.3.11+时与全服Xy系列提示保持一致。
- 保持XyCore软依赖：未安装、未启用或API不可用时，继续使用本插件 `messages.prefix` 独立运行。
- 自动拾取、MythicMobs掉落、灵魂商店、快捷分解和命令反馈统一走同一个消息发送工具。
- 控制台日志仍保留XySoulSpace插件名，便于定位仓库、商店和材料事务问题。
- 同步更新README、AI记录、默认配置注释和版本号。

## 1.1.1 - 2026-07-30

- 新增完整物品库ID材料统计。
- 新增批量材料检查和全有或全无的原子扣除。
- 新增精确退款凭据及0到100百分比退款。
- 接入XyCore 0.3.10统一物品匹配，避免按去色名称误扣同名材料。
- 为XyForgeCrafting提供默认“灵魂仓库优先、玩家背包补足”的事务接口。

旧版1.1功能记录保留在README中。
