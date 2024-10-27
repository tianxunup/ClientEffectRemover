[English](README.md) | 简体中文

## Client Effect Remover (客户端效果移除)
该mod可以在客户端屏蔽某些效果，例如失明、黑暗或者其他你不喜欢的（包括mod效果）。

如果你禁用了一种状态效果，你将不会看到它的图标。但是它们仍然在服务端存在。
例如，即使你已经禁用了缓慢效果，但是你仍然会走得很慢。

### 命令
主命令: `effectr`

`effectr disable <效果>`: 将效果添加到屏蔽列表。

`effectr enable <效果>`: 将效果从屏蔽列表删除。

`effectr list`: 查看屏蔽列表。

`effectr remove <效果>`: 在客户端一次性删除状态效果。

### 配置文件
配置文件路径: `config/effectremover.json`

这个文件应该是像这样的:
```json5
{
  "disabled_effects": [
    "minecraft:effect"
    // ...more
  ]
}
```