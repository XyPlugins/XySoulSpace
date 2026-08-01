# AI 使用记录

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
