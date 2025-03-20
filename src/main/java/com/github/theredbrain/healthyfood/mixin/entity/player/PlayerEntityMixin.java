package com.github.theredbrain.healthyfood.mixin.entity.player;

import com.github.theredbrain.healthyfood.HealthyFood;
import com.github.theredbrain.healthyfood.config.ServerConfig;
import com.github.theredbrain.healthyfood.entity.DuckLivingEntityMixin;
import com.github.theredbrain.healthyfood.entity.player.DuckPlayerEntityMixin;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.data.TrackedData;
import net.minecraft.entity.data.TrackedDataHandlerRegistry;
import net.minecraft.entity.player.HungerManager;
import net.minecraft.entity.player.ItemCooldownManager;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.FoodComponent;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.registry.Registries;
import net.minecraft.registry.RegistryKey;
import net.minecraft.text.Text;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Optional;

@Mixin(PlayerEntity.class)
public abstract class PlayerEntityMixin extends LivingEntity implements DuckPlayerEntityMixin {

	@Shadow
	public abstract void sendMessage(Text message, boolean overlay);

	@Shadow
	public abstract ItemStack getEquippedStack(EquipmentSlot slot);

	@Shadow
	public abstract ItemCooldownManager getItemCooldownManager();

	@Unique
	private int fullnessTickTimer = 0;

	@Unique
	private static final TrackedData<Float> FULLNESS = DataTracker.registerData(PlayerEntity.class, TrackedDataHandlerRegistry.FLOAT);

	protected PlayerEntityMixin(EntityType<? extends LivingEntity> entityType, World world) {
		super(entityType, world);
	}

	@Inject(method = "initDataTracker", at = @At("RETURN"))
	protected void healthyfood$initDataTracker(CallbackInfo ci) {
		this.dataTracker.startTracking(FULLNESS, 0.0F);

	}

	@Inject(method = "readCustomDataFromNbt", at = @At("TAIL"))
	public void healthyfood$readCustomDataFromNbt(NbtCompound nbt, CallbackInfo ci) {

		if (nbt.contains("fullness", NbtElement.NUMBER_TYPE)) {
			this.healthyfood$setFullness(nbt.getFloat("fullness"));
		}

	}

	@Inject(method = "writeCustomDataToNbt", at = @At("TAIL"))
	public void healthyfood$writeCustomDataToNbt(NbtCompound nbt, CallbackInfo ci) {

		nbt.putFloat("fullness", this.healthyfood$getFullness());

	}

	@Inject(method = "tick", at = @At("TAIL"))
	public void healthyfood$tick(CallbackInfo ci) {
		if (!this.getWorld().isClient) {

			this.fullnessTickTimer++;

			if (this.fullnessTickTimer > ((DuckLivingEntityMixin) this).healthyfood$getFullnessTickThreshold()) {
				this.healthyfood$addFullness(-1);
				this.fullnessTickTimer = 0;
			}
		}
	}

	@WrapOperation(
			method = "eatFood",
			at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/player/HungerManager;eat(Lnet/minecraft/item/Item;Lnet/minecraft/item/ItemStack;)V")
	)
	private void healthyfood$eatFood(HungerManager instance, Item item, ItemStack stack, Operation<Void> original) {
		healthyfood$healWithFood(item, stack);
		int foodFullness = 1;
		String itemId = "";
		Optional<RegistryKey<Item>> optional = Registries.ITEM.getKey(item);
		if (optional.isPresent()) {
			itemId = optional.get().getValue().toString();
		}
		if (!itemId.isEmpty()) {
			foodFullness = HealthyFood.SERVER_CONFIG.food_fullness.getOrDefault(itemId, 1);
		}
		this.healthyfood$addFullness(foodFullness);
	}

	@Unique
	public void healthyfood$healWithFood(Item item, ItemStack stack) {
		FoodComponent foodComponent = item.getFoodComponent();
		if (foodComponent != null) {
			ServerConfig serverConfig = HealthyFood.SERVER_CONFIG;

			ServerConfig.CalculationModifiers calculation_multipliers = null;
			String itemId = "";
			Optional<RegistryKey<Item>> optional = Registries.ITEM.getKey(item);
			if (optional.isPresent()) {
				itemId = optional.get().getValue().toString();
			}
			if (!itemId.isEmpty()) {
				calculation_multipliers = serverConfig.calculation_multipliers.getOrDefault(itemId, serverConfig.default_calculation_multipliers.get());
			}
			if (calculation_multipliers == null) {
				calculation_multipliers = serverConfig.default_calculation_multipliers.get();
			}
			float amount = (foodComponent.getHunger() * calculation_multipliers.food_multiplier) + (foodComponent.getSaturationModifier() * calculation_multipliers.saturation_multiplier);
			this.heal((amount * calculation_multipliers.health_multiplier) + calculation_multipliers.additional_health);
			HealthyFood.addMana(this, (amount * calculation_multipliers.mana_multiplier) + calculation_multipliers.additional_mana);
			HealthyFood.addStamina(this, (amount * calculation_multipliers.stamina_multiplier) + calculation_multipliers.additional_stamina);
			this.getItemCooldownManager().set(stack.getItem(), serverConfig.item_cooldown_after_eating);
		}
	}

	@Unique
	public boolean healthyfood$canConsumeItem(ItemStack itemStack) {
		if (itemStack.getItem().getFoodComponent() != null) {
			float currentFullness = this.healthyfood$getFullness();
			int maxFullness = ((DuckLivingEntityMixin) this).healthyfood$getMaxFullness();
			Item item = itemStack.getItem();
			int foodFullness = 1;
			String itemId = "";
			Optional<RegistryKey<Item>> optional = Registries.ITEM.getKey(item);
			if (optional.isPresent()) {
				itemId = optional.get().getValue().toString();
			}
			if (!itemId.isEmpty()) {
				foodFullness = HealthyFood.SERVER_CONFIG.food_fullness.getOrDefault(itemId, 1);
			}
			if (foodFullness == 0) {
				return true;
			} else if (currentFullness >= maxFullness) {
				if (this.getWorld().isClient) {
					this.sendMessage(Text.translatable("hud.message.max_fullness_reached"), true);
				}
				this.getItemCooldownManager().set(item, HealthyFood.SERVER_CONFIG.item_cooldown_after_eating);
				return false;
			} else {
				if (currentFullness + foodFullness > maxFullness) {
					if (this.getWorld().isClient) {
						this.sendMessage(Text.translatable("hud.message.too_full_for_item", Text.translatable(item.getTranslationKey())), true);
					}
					this.getItemCooldownManager().set(item, HealthyFood.SERVER_CONFIG.item_cooldown_after_eating);
					return false;
				} else {
					return true;
				}
			}
		}
		return false;
	}

	@Override
	public void healthyfood$addFullness(float fullness) {
		float f = this.healthyfood$getFullness();
		this.healthyfood$setFullness(f + fullness);
		if (fullness > 0) {
			this.fullnessTickTimer = 0;
		}
	}

	@Override
	public float healthyfood$getFullness() {
		return this.dataTracker.get(FULLNESS);
	}

	@Override
	public void healthyfood$setFullness(float fullness) {
		this.dataTracker.set(FULLNESS, MathHelper.clamp(fullness, 0, ((DuckLivingEntityMixin) this).healthyfood$getMaxFullness()));
	}

}
