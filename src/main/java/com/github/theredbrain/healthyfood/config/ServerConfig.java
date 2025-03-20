package com.github.theredbrain.healthyfood.config;

import com.github.theredbrain.healthyfood.HealthyFood;
import me.fzzyhmstrs.fzzy_config.annotations.Comment;
import me.fzzyhmstrs.fzzy_config.annotations.ConvertFrom;
import me.fzzyhmstrs.fzzy_config.config.Config;
import me.fzzyhmstrs.fzzy_config.util.Walkable;
import me.fzzyhmstrs.fzzy_config.validation.collection.ValidatedMap;
import me.fzzyhmstrs.fzzy_config.validation.misc.ValidatedAny;
import me.fzzyhmstrs.fzzy_config.validation.misc.ValidatedString;

import java.util.HashMap;
import java.util.LinkedHashMap;

@ConvertFrom(fileName = "server.json5", folder = HealthyFood.MOD_ID)
public class ServerConfig extends Config {

	public ServerConfig() {
		super(HealthyFood.identifier("server"));
	}

	@Comment("""
			Eating a food item applies an item cooldown of this many ticks (20 ticks per second) to that item.
			This prevents the "You are full" message to immediately pop up.
			""")
	public int item_cooldown_after_eating = 20;
	@Comment("""
			Food items in this map grant the corresponding fullness when eaten.
			Food items not present here grant the default of 1 fullness.
			Food items with fullness of 0 can always be eaten.
			""")
	public LinkedHashMap<String, Integer> food_fullness = new LinkedHashMap<>() {{
		put("minecraft:cooked_porkchop", 2);
		put("bonfires:estus_flask", 0);
		put("minecraft:cookie", 0);
	}};

	public ValidatedAny<CalculationModifiers> default_calculation_multipliers = new ValidatedAny<>(new CalculationModifiers(2.0F, 0.0F, 1.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F));

	public ValidatedMap<String, CalculationModifiers> calculation_multipliers = new ValidatedMap<>(new HashMap<>() {{
		put("minecraft:glow_berries", new CalculationModifiers(2.0F, 0.0F, 0.0F, 1.0F, 0.0F, 0.0F, 0.0F, 0.0F));
		put("minecraft:apple", new CalculationModifiers(2.0F, 0.0F, 0.0F, 0.0F, 1.0F, 0.0F, 0.0F, 0.0F));
	}}, new ValidatedString(), new ValidatedAny<>(new CalculationModifiers()));

	public static class CalculationModifiers implements Walkable {

		public CalculationModifiers() {
			new CalculationModifiers(1.0F, 0.0F, 1.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F);
		}

		public CalculationModifiers(float food_multiplier, float saturation_multiplier, float health_multiplier, float mana_multiplier, float stamina_multiplier, float additional_health, float additional_mana, float additional_stamina) {
			this.food_multiplier = food_multiplier;
			this.saturation_multiplier = saturation_multiplier;
			this.health_multiplier = health_multiplier;
			this.mana_multiplier = mana_multiplier;
			this.stamina_multiplier = stamina_multiplier;
			this.additional_health = additional_health;
			this.additional_mana = additional_mana;
			this.additional_stamina = additional_stamina;
		}

		public float food_multiplier;
		public float saturation_multiplier;
		public float health_multiplier;
		public float mana_multiplier;
		public float stamina_multiplier;
		public float additional_health;
		public float additional_mana;
		public float additional_stamina;

		public String toString() {
			return "food_multiplier: " + this.food_multiplier +
					", saturation_multiplier: " + this.saturation_multiplier +
					", health_multiplier: " + this.health_multiplier +
					", mana_multiplier: " + this.mana_multiplier +
					", stamina_multiplier: " + this.stamina_multiplier +
					", additional_health: " + this.additional_health +
					", additional_mana: " + this.additional_mana +
					", additional_stamina: " + this.additional_stamina;
		}
	}
}
