package com.kjmaster.yield.project;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

public class ProjectGoal {
    // The Codec allows automatic serialization/deserialization
    public static final Codec<ProjectGoal> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            BuiltInRegistries.ITEM.byNameCodec().fieldOf("item").forGetter(ProjectGoal::getItem),
            Codec.INT.fieldOf("target_amount").forGetter(ProjectGoal::getTargetAmount)
    ).apply(instance, ProjectGoal::new));

    private final Item item;
    private int targetAmount;

    // Transient data (not serialized) - used for live tracking
    private transient int currentCachedAmount = 0;
    private transient double itemsPerHour = 0.0;
    private transient List<Integer> rateHistory = new ArrayList<>();

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

    // --- Live Tracking Helpers ---

    public void updateCache(int currentAmount, double rate) {
        this.currentCachedAmount = currentAmount;
        this.itemsPerHour = rate;

        rateHistory.add((int)rate);
        if (rateHistory.size() > 20) {
            rateHistory.removeFirst();
        }
    }

    public float getProgress() {
        if (targetAmount == 0) return 0;
        return Math.min(1.0f, (float) currentCachedAmount / targetAmount);
    }

    public ItemStack getRenderStack() {
        return new ItemStack(this.item);
    }

    public int getCurrentCachedAmount() {
        return currentCachedAmount;
    }

    public double getItemsPerHour() {
        return itemsPerHour;
    }

    public List<Integer> getRateHistory() {
        return rateHistory;
    }
}
