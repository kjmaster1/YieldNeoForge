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
import java.util.UUID;

public record ProjectGoal(
        UUID id,
        Item item,
        int targetAmount,
        boolean strict,
        Optional<DataComponentPatch> components,
        Optional<TagKey<Item>> targetTag
) {

    public static final Codec<ProjectGoal> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.STRING.xmap(UUID::fromString, UUID::toString).fieldOf("id").forGetter(ProjectGoal::id),
            BuiltInRegistries.ITEM.byNameCodec().fieldOf("item").forGetter(ProjectGoal::item),
            Codec.INT.fieldOf("target_amount").forGetter(ProjectGoal::targetAmount),
            Codec.BOOL.optionalFieldOf("strict", false).forGetter(ProjectGoal::strict),
            DataComponentPatch.CODEC.optionalFieldOf("components").forGetter(ProjectGoal::components),
            TagKey.codec(Registries.ITEM).optionalFieldOf("tag").forGetter(ProjectGoal::targetTag)
    ).apply(instance, ProjectGoal::new));

    // Convenience Constructor
    public ProjectGoal(Item item, int targetAmount) {
        this(UUID.randomUUID(), item, targetAmount, false, Optional.empty(), Optional.empty());
    }

    // Constructor for creating new goals
    public ProjectGoal(Item item, int targetAmount, boolean strict, Optional<DataComponentPatch> components, Optional<TagKey<Item>> targetTag) {
        this(UUID.randomUUID(), item, targetAmount, strict, components, targetTag);
    }

    public static ProjectGoal fromStack(ItemStack stack, int targetAmount) {
        return new ProjectGoal(
                UUID.randomUUID(),
                stack.getItem(),
                targetAmount,
                false,
                Optional.of(stack.getComponentsPatch()),
                Optional.empty()
        );
    }

    public ItemStack getRenderStack() {
        ItemStack stack = new ItemStack(item);
        components.ifPresent(stack::applyComponents);
        return stack;
    }
}