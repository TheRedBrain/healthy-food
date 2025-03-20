package com.github.theredbrain.healthyfood.mixin.entity.attribute;

import com.github.theredbrain.healthyfood.HealthyFood;
import net.minecraft.entity.attribute.ClampedEntityAttribute;
import net.minecraft.entity.attribute.EntityAttribute;
import net.minecraft.entity.attribute.EntityAttributes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(EntityAttributes.class)
public class EntityAttributesMixin {
	@Shadow
	private static EntityAttribute register(String id, EntityAttribute attribute) {
		throw new AssertionError();
	}

	static {
		HealthyFood.FULLNESS_TICK_THRESHOLD = register(HealthyFood.MOD_ID + ":generic.fullness_tick_threshold", new ClampedEntityAttribute("attribute.name.generic.fullness_tick_threshold", 400.0, 0.0, 1024.0).setTracked(true));
		HealthyFood.MAX_FULLNESS = register(HealthyFood.MOD_ID + ":generic.max_fullness", new ClampedEntityAttribute("attribute.name.generic.max_fullness", 3.0, 0.0, 1024.0).setTracked(true));
	}
}
