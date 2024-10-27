package cc.tianxun.effectremover;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.exceptions.CommandSyntaxException;

public class ClientStatusEffectArgumentType implements ArgumentType<ClientStatusEffectArgumentType> {
	@Override
	public ClientStatusEffectArgumentType parse(StringReader stringReader) throws CommandSyntaxException {
		return null;
	}
}
