package cc.tianxun.effectremover;

import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;
import com.mojang.brigadier.Command;
import com.mojang.brigadier.context.CommandContext;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.command.argument.RegistryEntryReferenceArgumentType;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.registry.Registries;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.text.Text;
import net.minecraft.text.TextColor;
import net.minecraft.util.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.ConcurrentModificationException;
import java.util.List;
import java.util.Objects;

public class EffectRemover implements ModInitializer {
	public static String MOD_ID = "effectremover";
	public static Logger LOGGER = LoggerFactory.getLogger("effectremover");

	public List<String> disabledEffects = new ArrayList<>();

	private void loadConfig() {
		LOGGER.info("Loading config.");
		File file = new File(String.format("config/%s.json", EffectRemover.MOD_ID));
		if (!file.exists()) {
			EffectRemover.LOGGER.info("Couldn't find config file, creating");
			try {
				if (!new File("config").mkdirs()) {
					LOGGER.warn("Failed to create the config dir.");
				}
				if (!file.createNewFile()) {
					LOGGER.warn("Failed to create the config file.");
				}
				InputStream defaultFileStream = EffectRemover.class.getResourceAsStream("/default_config.json");

				OutputStreamWriter writer = new OutputStreamWriter(new FileOutputStream(file), StandardCharsets.UTF_8);
				writer.write(new String(Objects.requireNonNull(defaultFileStream).readAllBytes()));

				writer.close();
				defaultFileStream.close();
			}
			catch (IOException e) {
				LOGGER.error("Error!");
				throw new RuntimeException(e);
			}
		}
		JsonReader reader;
		try {
			reader = new JsonReader(new InputStreamReader(new FileInputStream(file), StandardCharsets.UTF_8));
			reader.beginObject();
			while (reader.hasNext()) {
				String key = reader.nextName();
				switch (key) {
					case "disabled_effects" -> this.loadEffectsFromJsonFile(reader);
					default -> {
						LOGGER.warn("Invalid key in the config file: {}",key);
						reader.skipValue();
					}
				}
			}
			reader.endObject();
			reader.close();
		}
		catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	private void loadEffectsFromJsonFile(JsonReader reader) throws IOException {
		reader.beginArray();
		while (reader.hasNext()) {
			this.disabledEffects.add(reader.nextString());
		}
		reader.endArray();
	}

	private void saveConfig() {
		LOGGER.info("Saving config.");
		JsonWriter writer;
		try {
			writer = new JsonWriter(
				new OutputStreamWriter(new FileOutputStream(String.format("config/%s.json", EffectRemover.MOD_ID)))
			);
			writer.setIndent("\t");
			writer.beginObject();

			writer.name("disabled_effects");
			writer.beginArray();
			for (String effectId : this.disabledEffects) {
				writer.value(effectId);
			}
			writer.endArray();

			writer.endObject();

			writer.close();
		}
		catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	private int addingDisabledEffectCommand(CommandContext<FabricClientCommandSource> context) {
		String effectId = context.getArgument("effect", RegistryEntry.Reference.class).getIdAsString();
		if (this.disabledEffects.contains(effectId)) {
			context.getSource().sendFeedback(
				Text.translatable("commands.effectr.disable.exists", effectId).fillStyle(Text.literal("").getStyle().withColor(TextColor.fromRgb(255*256*256)))
			);
			return 0;
		}
		this.disabledEffects.add(effectId);
		this.saveConfig();
		context.getSource().sendFeedback(
			Text.translatable("commands.effectr.disable.success", effectId)
		);
		return Command.SINGLE_SUCCESS;
	}
	private int removingDisabledEffectCommand(CommandContext<FabricClientCommandSource> context) {
		String effectId = context.getArgument("effect", RegistryEntry.Reference.class).getIdAsString();
		if (!this.disabledEffects.contains(effectId)) {
			context.getSource().sendFeedback(
				Text.translatable("commands.effectr.enable.not_exists", effectId).fillStyle(Text.literal("").getStyle().withColor(TextColor.fromRgb(255*256*256)))
			);
			return 0;
		}
		this.disabledEffects.remove(effectId);
		this.saveConfig();
		context.getSource().sendFeedback(
			Text.translatable("commands.effectr.enable.success", effectId)
		);
		return Command.SINGLE_SUCCESS;
	}
	private int removingEffectOnceCommand(CommandContext<FabricClientCommandSource> context) {
		String effectId = context.getArgument("effect", RegistryEntry.Reference.class).getIdAsString();
		for (StatusEffectInstance effect : context.getSource().getPlayer().getStatusEffects()) {
			if (effect.getEffectType().getIdAsString().equals(effectId)) {
				context.getSource().getPlayer().removeStatusEffect(effect.getEffectType());
				context.getSource().sendFeedback(Text.translatable("commands.effectr.remove.success"));
				return Command.SINGLE_SUCCESS;
			}
		}
		context.getSource().sendFeedback(Text.translatable("commands.effectr.remove.without"));
		return 0;
	}
	private int sendDisabledEffectCommand(CommandContext<FabricClientCommandSource> context) {
		if (this.disabledEffects.isEmpty()) {
			context.getSource().sendFeedback(Text.translatable("commands.effectr.list.empty"));
			return 0;
		}
		context.getSource().sendFeedback(Text.translatable("commands.effectr.list.head"));
		for (String effectId : this.disabledEffects) {
			StatusEffect effect = Registries.STATUS_EFFECT.get(Identifier.of(effectId));
			if (effect == null) {
				continue;
			}
			context.getSource().sendFeedback(Text.empty().append(" - ").append(effectId).append(" (")
				.append(effect.getName()).append(")"));
		}
		return Command.SINGLE_SUCCESS;
	}

	@Override
	public void onInitialize() {
		this.loadConfig();
		ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> dispatcher.register(ClientCommandManager.literal("effectr")
			.then(ClientCommandManager.literal("disable").then(ClientCommandManager.argument("effect", RegistryEntryReferenceArgumentType.registryEntry(registryAccess, RegistryKeys.STATUS_EFFECT))
				.executes(this::addingDisabledEffectCommand)))
			.then(ClientCommandManager.literal("enable").then(ClientCommandManager.argument("effect", RegistryEntryReferenceArgumentType.registryEntry(registryAccess, RegistryKeys.STATUS_EFFECT))
				.executes(this::removingDisabledEffectCommand)))
			.then(ClientCommandManager.literal("list").executes(this::sendDisabledEffectCommand))
			.then(ClientCommandManager.literal("remove").then(ClientCommandManager.argument("effect", RegistryEntryReferenceArgumentType.registryEntry(registryAccess, RegistryKeys.STATUS_EFFECT))
				.executes(this::removingEffectOnceCommand)))
		));
		ClientTickEvents.END_CLIENT_TICK.register(client -> {
			if (client.player == null) {
				return;
			}
			try {
				for (StatusEffectInstance effect : client.player.getStatusEffects()) {
					if (this.disabledEffects.contains(effect.getEffectType().getIdAsString())) {
						client.player.removeStatusEffect(effect.getEffectType());
					}
				}
			}
			catch (ConcurrentModificationException e) {
				LOGGER.warn("ConcurrentModificationException: null");
			}
		});
	}
}
