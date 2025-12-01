package com.kjmaster.yield.project;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.component.DataComponentPatch;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.Optional;

public class ProjectGoal {

    public static final Codec<ProjectGoal> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            BuiltInRegistries.ITEM.byNameCodec().fieldOf("item").forGetter(ProjectGoal::getItem),
            Codec.INT.fieldOf("target_amount").forGetter(ProjectGoal::getTargetAmount),
            Codec.BOOL.optionalFieldOf("strict", false).forGetter(ProjectGoal::isStrict),
            DataComponentPatch.CODEC.optionalFieldOf("components").forGetter(ProjectGoal::getComponents)
    ).apply(instance, ProjectGoal::new));

    private final Item item;
    private int targetAmount;
    private boolean strict;
    private final Optional<DataComponentPatch> components;

    // Runtime cache for the render stack (lazy loaded)
    private ItemStack cachedStack;

    public ProjectGoal(Item item, int targetAmount, boolean strict, Optional<DataComponentPatch> components) {
        this.item = item;
        this.targetAmount = targetAmount;
        this.strict = strict;
        this.components = components;
    }

    public ProjectGoal(Item item, int targetAmount) {
        this(item, targetAmount, false, Optional.empty());
    }

    public static ProjectGoal fromStack(ItemStack stack, int targetAmount) {
        // When creating from a stack, capture its components
        return new ProjectGoal(
                stack.getItem(),
                targetAmount,
                false, // Default to fuzzy initially
                Optional.of(stack.getComponentsPatch())
        );
    }

    public Item getItem() { return item; }
    public int getTargetAmount() { return targetAmount; }
    public void setTargetAmount(int targetAmount) { this.targetAmount = targetAmount; }

    public boolean isStrict() { return strict; }
    public void setStrict(boolean strict) { this.strict = strict; }

    public Optional<DataComponentPatch> getComponents() { return components; }

    public ItemStack getRenderStack() {
        if (cachedStack == null) {
            cachedStack = new ItemStack(item);
            components.ifPresent(cachedStack::applyComponents);
        }
        return cachedStack;
    }
}