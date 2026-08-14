# AI 使用记录

## 1.1.10

本次修改由AI根据服主提出的“底栏增加自动拾取按钮、自动拾取后移除地面物品、兼容MM与XyItems并保持性能轻量”需求辅助完成。

已确认的实现边界：

- GUI槽位 `51` 固定显示个人自动拾取状态；玩家只能切换自己的设置，管理员查看他人仓库时整个界面只允许翻页和关闭，全局停用时也不能切换。
- 普通 `PlayerPickupItemEvent`、附近实体范围扫描和 MythicMobs `ssdrops` 统一调用同一套权限、全局开关和个人开关判断，避免三条路径行为不一致。
- 只有拥有 `xysoulspace.use` 权限、全局开关开启且个人开关开启的玩家才会直接入库；玩家个人状态继续保存到既有YML，不新增SQL或数据迁移。
- 对已有地面实体严格执行“成功入库后删除”；任何入库失败都会保留实体。MM `ssdrops` 是现场生成的掉落，未入库时生成在怪物死亡位置。
- 地面上的MM/XyItems ItemStack不会被重新生成，NBT、品质、随机属性和业务Lore会保留；只沿用既有兼容逻辑清理旧版 `灵魂数量/Key/左键取右键取` 内部GUI提示行。
- `ssdrops` 的 `xyitems:`、`mythicmobs:`、`minecraft:` 完整ID由XyCore `ItemLibraryService#create` 创建；旧版裸ID仍兼容本地物品库和MythicMobs物品库。
- 自动拾取提示优先使用实际显示名，能够展示MM/XyItems品质名和颜色；检测到兼容XyCore时使用XyCore玩家前缀，未安装时回退本插件前缀。
- 删除新默认配置中的旧MM独立提示，统一读取 `pickup.notification-*`；旧服原配置键即使保留也不会产生双提示。
- 相同玩家、相同物品的提示按短窗口合并；待发送物品类别有固定上限，聊天变量会过滤控制字符，避免掉落高峰造成无界内存增长或消息注入。
- 继续复用一个全服范围扫描任务，不创建每玩家任务；一次扫描批次内每名玩家最多刷新一次已打开的GUI。
- 扫描半径在运行时限制为64格以内，避免配置笔误造成无界附近实体查询；权限判断统一兼容OP和显式 `xysoulspace.use`。
- MythicMobs怪物配置只在启动和 `/xyss reload` 时递归读取并构建不可变快照，死亡事件不会扫描磁盘。
- 无效MM掉落数量或概率采用失败关闭策略，0%/100%严格生效；无XyCore时 `mythicmobs:` 与 `minecraft:` 有本地回退，`xyitems:` 明确依赖Core物品库。
- GUI使用私有 `InventoryHolder` 保存owner/page/key会话并拒绝拖拽，不再用可配置标题识别，避免自定义标题或翻页关闭事件破坏槽位保护。
- 异步YML保存使用同一快照和修订号条件清理dirty，并串行化文件读写；写盘过程中产生的新拾取会留待下一次保存。
- `/xyss reload` 会重新读取配置与物品库，刷新XyCore桥接、MM `ssdrops` 快照，并按新扫描/自动保存间隔分别重启唯一任务。

验证记录：

- `gradlew.bat compileJava` 已通过。
- `gradlew.bat test --rerun-tasks --no-daemon` 已通过，自动拾取个人开关、dirty标记、玩家间隔离及旧保存快照不能清除新修订共5项测试成功。
- 已执行 `gradlew.bat clean test build --no-daemon`，源码编译、5项JUnit测试和 `XySoulSpace-1.1.10.jar` 构建均通过。

## 1.1.9

本次修改由AI根据服主复测反馈的“Shift+左键仍然无法全部存入”问题辅助完成，并对照旧版SoulSpace 2.7行为确认了“全部”的准确语义。

已确认的实现边界：

- 旧版行为是存入背包中全部相同物品；1.1.8只存当前点击堆叠，属于语义回归。
- Shift左键仅遍历玩家主背包与快捷栏 `0-35`，不会读取装备、副手45号槽位或DragonCore扩展槽位。
- 相同物品使用当前仓库的完整存储键判断；不同耐久、名称、Lore、附魔、NBT或随机属性不会误合并。
- 左键仍存入1个，右键仍存入最多64个，Shift右键仍不处理。
- 保留下一tick重新确认点击槽位和单玩家短暂处理锁，避免1.12.2背包同步覆盖及快速连点重复处理。
- 每次Shift点击最多检查36格，且汇总后只存入、保存和刷新一次；不增加常驻扫描或定时任务。

验证记录：

- 已执行 `gradlew.bat clean test build --no-daemon`，源码编译、JUnit测试和JAR构建均通过。

## 1.1.8

本次修改由AI根据服主反馈的“存入时候Shift+左键无法全部存入”和“提示信息末尾句号需要去掉”问题辅助完成。

已确认的实现边界：

- 下方玩家背包点击存入仍只允许主背包和快捷栏 `0-35`，不会处理装备、副手或DragonCore扩展槽位。
- 左键存入1个、右键存入最多64个、Shift左键存入当前点击整组。
- 实际扣除和存入延迟到下一tick执行，避免1.12.2取消点击事件后的客户端/服务端同步覆盖。
- 单个玩家同一时间只处理一个背包存入点击，降低快速连点导致重复处理的风险。
- 玩家提示消息会自动去掉末尾中文句号，旧config.yml未手动更新时也能生效。
- 不新增常驻扫描任务，不改变灵魂空间YML存储结构。

验证记录：

- 已执行 `gradlew.bat clean build --no-daemon`，构建与测试通过，并交付 `XySoulSpace-1.1.8.jar`。

## 1.1.7

本次修改由AI根据服主反馈的“只能一键存入、不能点击下方背包手动存入”和“初墨之锋被提示成钻石剑”问题辅助完成。

已确认的实现边界：

- 灵魂空间上方仓库槽和按钮仍然锁定，避免GUI展示物品被拿下。
- 下方玩家主背包与快捷栏允许点击触发存入：左键1个，右键最多64个，Shift左键整组。
- 不启用拖拽存入，降低交互复杂度和误操作风险。
- 管理员查看别人灵魂空间时不允许从自己的背包存入到对方仓库。
- GUI取出提示Lore默认恢复显示，但只存在于展示副本，不写入真实物品。
- 玩家消息物品名兜底顺序修正：完整ID识别失败时优先自定义显示名，再使用原版中文名。

验证记录：

- 已执行 `gradlew.bat clean build --no-daemon`，构建与测试通过，并交付 `XySoulSpace-1.1.7.jar`。

## 1.1.6

本次修改由AI根据服主确认“公测主要使用MM和XyItems，原版物品较少，但希望默认不需要手动翻译”辅助完成。

已确认的实现边界：

- 生成并内置 Bukkit 1.12.2 的 463 个基础 `Material` 中文显示名。
- 数据来源为本地服务器已有 `StarLibrary/Data/ItemName-CH.yml`，并对少数不适合玩家提示的基础项做了人工修正，例如水、熔岩、床、药水。
- `messages.vanilla-names` 继续作为覆盖表；服主只在想改叫法时配置，不需要完整翻译。
- 运行时不读外部语言文件，不扫描全物品表；玩家提示只按当前物品的 `Material.name()` 查一次只读Map。
- XyItems/MythicMobs物品身份识别和完整ID显示逻辑不变。
- 本次不改变灵魂空间YML存储结构、不新增SQL、不新增常驻扫描任务。

验证记录：

- 已执行 `gradlew.bat clean build --no-daemon` 通过，并交付 `XySoulSpace-1.1.6.jar`。

## 1.1.5

本次修改由AI根据服主反馈的“取出原版信标仍显示 minecraft:BEACON，无法显示信标名字”问题辅助完成。

已确认的实现边界：

- Bukkit 1.12.2服务端无法直接读取玩家客户端语言包里的“信标”等原版物品中文名。
- 插件采用轻量映射方案：`messages.vanilla-names` 配置优先，插件内置少量常用原版材质中文名fallback。
- `item-name-mode: id` 继续保留自定义物品完整ID展示能力；仅对原版minecraft物品优先显示中文名，避免聊天框出现大量 `minecraft:BEACON`。
- 新增 `raw-id` 调试模式，服主需要严格查看完整ID时可手动切换。
- 本次不改变仓库数据结构，不增加全局扫描，不影响XyItems/MythicMobs身份匹配。

验证记录：

- 已执行 `gradlew.bat clean build --no-daemon` 通过，并交付 `XySoulSpace-1.1.5.jar`。

## 1.1.4

本次修改由AI根据服主反馈的灵魂空间取出显示与一键存入安全问题辅助完成。

已确认的实现边界：

- 一键存入只处理 Bukkit 玩家背包槽位 `0-35`，即主背包与快捷栏。
- 不处理装备栏、副手45号槽位或其它DragonCore映射/客户端扩展槽位，避免穿戴中的墨魂、墨魄、装备被误存入。
- GUI展示物品与真实存储模板分离：点击取出使用内存中的槽位到存储Key映射，不再通过可见Lore中的 `Key` 识别。
- 默认不显示GUI内部 `Key` 和点击操作提示；如服主确实需要调试，可在配置中手动开启。
- 存入时会清理旧版本遗留的 `灵魂数量`、`Key`、`左键取/右键取` 内部Lore。
- 玩家消息中的物品显示默认走完整物品ID；检测到XyCore物品库时优先识别XyItems/MythicMobs ID，没有XyCore时回退到 `minecraft:材质名`。
- 本次不改变灵魂空间YML数据结构，不新增SQL，不增加常驻全局扫描。

验证记录：

- 已执行 `gradlew.bat clean build --no-daemon` 通过，并交付 `XySoulSpace-1.1.4.jar`。

## 1.1.3

本次修改由AI根据服主最终确认的“玩家玩法提示统一、管理/报错提示保留插件名”规则辅助完成。

已确认的实现边界：

- 玩家玩法结果继续通过 `Text.send/sendRaw` 走 XyCore 前缀，包括存入、取出、自动拾取、商店购买、MM掉落和分解提示。
- 管理/帮助/报错改用 `Text.sendLocal/sendLocalRaw`，包括 help、reload、reloadshop、globalpickup、giveitem、saveitem、admin、clear 的用法与权限反馈。
- XySoulSpace 仍保持 XyCore 软依赖，独立运行时所有提示回退到本插件 `messages.prefix`。
- 本次不改变仓库YML结构、材料原子扣除、商店兑换、自动拾取性能和 MythicMobs 桥接。

验证记录：

- 已执行 `gradlew.bat compileJava --no-daemon` 通过，最终交付前参与全量 `clean build`。

## 1.1.2

本次修改由AI根据服主确认的“可独立使用插件没有XyCore时保留自己前缀”规则辅助完成。

已确认的实现边界：

- XySoulSpace保持XyCore软依赖，不因未安装Core而阻止灵魂仓库、商店、自动拾取等核心功能启动。
- 玩家聊天提示优先使用XyCore 0.3.11+ 的 `getMessagePrefix()`；没有可用Core时使用本插件 `messages.prefix`。
- 本次只处理聊天提示，不改变DragonCore/HUD类显示，也不改变控制台日志插件名。
- 散落在命令、自动拾取、MM掉落、商店、分解和物品库中的玩家提示统一收敛到 `Text.sendRaw`。

验证记录：

- 已进行Java编译验证；完整构建随本次多插件统一前缀交付执行。

## 1.1.1

本次修改由AI根据服主确认的XyForgeCrafting材料来源规则辅助完成。

已确认的实现边界：

- 新接口只面向Java 8与Paper/Spigot 1.12.2。
- 物品身份由XyCore完整 `provider:item` 匹配，不按名称或Lore推断。
- 仓库批量扣除在同一同步边界内先规划后执行，任一材料不足时完全不修改数据。
- 成功扣除返回实际物品模板和数量组成的凭据，事务异常或锻造失败可按比例退款。
- 保留1.1的旧 `CostKey` 接口，避免现有灵魂商店配置失效；XyForgeCrafting不使用旧接口。
- XyCore继续声明为软依赖，未安装时原有仓库功能可用，但完整ID事务接口不会匹配物品。

验证记录：

- `gradlew.bat clean build --no-daemon` 成功。
- 测试确认批量材料中任一项不足时仓库内容完全不变；全部满足时凭据和剩余数量正确。
- 已核对 `XySoulSpace-1.1.1.jar` 未打入XyCore类型；软依赖桥接使用延迟反射，未安装Core时不会因类链接失败阻止旧仓库功能启动。
