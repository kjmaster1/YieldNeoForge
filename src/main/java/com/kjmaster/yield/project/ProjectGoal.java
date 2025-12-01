package com.kjmaster.yield.project;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.component.DataComponentPatch;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.Optional;

public class ProjectGoal {

    public static final Codec<ProjectGoal> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            BuiltInRegistries.ITEM.byNameCodec().fieldOf("item").forGetter(ProjectGoal::getItem),
            Codec.INT.fieldOf("target_amount").forGetter(ProjectGoal::getTargetAmount),
            Codec.BOOL.optionalFieldOf("strict", false).forGetter(ProjectGoal::isStrict),
            DataComponentPatch.CODEC.optionalFieldOf("components").forGetter(ProjectGoal::getComponents),
            TagKey.codec(Registries.ITEM).optionalFieldOf("tag").forGetter(ProjectGoal::getTargetTag)
    ).apply(instance, ProjectGoal::new));

    private final Item item;
    private int targetAmount;
    private boolean strict;
    private final Optional<DataComponentPatch> components;
    private final Optional<TagKey<Item>> targetTag;

    // Runtime cache for the render stack (lazy loaded)
    private ItemStack cachedStack;

    // Updated Constructor
    public ProjectGoal(Item item, int targetAmount, boolean strict, Optional<DataComponentPatch> components, Optional<TagKey<Item>> targetTag) {
        this.item = item;
        this.targetAmount = targetAmount;
        this.strict = strict;
        this.components = components;
        this.targetTag = targetTag;
    }

    // Legacy/Convenience Constructor
    public ProjectGoal(Item item, int targetAmount) {
        this(item, targetAmount, false, Optional.empty(), Optional.empty());
    }

    // Serialization Helper (used by CODEC if constructor structure varies, but here it matches)
    public ProjectGoal(Item item, int targetAmount, boolean strict, Optional<DataComponentPatch> components) {
        this(item, targetAmount, strict, components, Optional.empty());
    }

    public static ProjectGoal fromStack(ItemStack stack, int targetAmount) {
        // When creating from a stack, capture its components
        return new ProjectGoal(
                stack.getItem(),
                targetAmount,
                false, // Default to fuzzy initially
                Optional.of(stack.getComponentsPatch()),
                Optional.empty()
        );
    }

    public Item getItem() {
        return item;
    }

    public int getTargetAmount() {
        return targetAmount;
    }

    public void setTargetAmount(int targetAmount) {
        this.targetAmount = targetAmount;
    }

    public boolean isStrict() {
        return strict;
    }

    public void setStrict(boolean strict) {
        this.strict = strict;
    }

    public Optional<DataComponentPatch> getComponents() {
        return components;
    }

    public Optional<TagKey<Item>> getTargetTag() {
        return targetTag;
    }

    public ItemStack getRenderStack() {
        if (cachedStack == null) {
            cachedStack = new ItemStack(item);
            components.ifPresent(cachedStack::applyComponents);
        }
        return cachedStack;
    }
}