# XySoulSpace 更新说明

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
