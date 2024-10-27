English | [简体中文](README_cn.md)

## Client Effect Remover
This mod can disable some status effects in the client-side, such as blindness, darkness and others you don't like(including effects of mods).

If a type of status effects is disabled, you will not see the icon in your client.But they are still existent in the server.
For example, you have disabled the slowness effect, but you walk slow still.

### Commands
The main command: `effectr`

`effectr disable <effect>`: Append the effect to the disabled list.

`effectr enable <effect>`: Remove the effect from the disabled list.

`effectr list`: Show the disabled list.

`effectr remove <effect>`: Remove the effect in your client for once.

### Config File
Config file path: `config/effectremover.json`

The file should be like this:
```json5
{
  "disabled_effects": [
    "minecraft:effect"
    // ...more
  ]
}
```