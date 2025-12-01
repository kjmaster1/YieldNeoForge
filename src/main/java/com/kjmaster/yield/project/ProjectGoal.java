package com.kjmaster.yield.project;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/**
 * Represents the configuration of a goal.
 */
public class ProjectGoal {

    public static final Codec<ProjectGoal> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            BuiltInRegistries.ITEM.byNameCodec().fieldOf("item").forGetter(ProjectGoal::getItem),
            Codec.INT.fieldOf("target_amount").forGetter(ProjectGoal::getTargetAmount)
    ).apply(instance, ProjectGoal::new));

    private final Item item;
    private int targetAmount;

    public ProjectGoal(Item item, int targetAmount) {
        this.item = item;
        this.targetAmount = targetAmount;
    }

    public static ProjectGoal fromStack(ItemStack stack, int targetAmount) {
        return new ProjectGoal(stack.getItem(), targetAmount);
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

    public ItemStack getRenderStack() {
        return new ItemStack(this.item);
    }
}