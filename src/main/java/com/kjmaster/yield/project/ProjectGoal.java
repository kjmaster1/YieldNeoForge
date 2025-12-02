package com.kjmaster.yield.project;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.component.DataComponentPatch;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public record ProjectGoal(
        UUID id,
        Item item,
        int targetAmount,
        boolean strict,
        Optional<DataComponentPatch> components,
        Optional<TagKey<Item>> targetTag,
        List<ResourceLocation> ignoredComponents // New Field: "Mask"
) {

    public static final Codec<ProjectGoal> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.STRING.xmap(UUID::fromString, UUID::toString).fieldOf("id").forGetter(ProjectGoal::id),
            BuiltInRegistries.ITEM.byNameCodec().fieldOf("item").forGetter(ProjectGoal::item),
            Codec.INT.fieldOf("target_amount").forGetter(ProjectGoal::targetAmount),
            Codec.BOOL.optionalFieldOf("strict", false).forGetter(ProjectGoal::strict),
            DataComponentPatch.CODEC.optionalFieldOf("components").forGetter(ProjectGoal::components),
            TagKey.codec(Registries.ITEM).optionalFieldOf("tag").forGetter(ProjectGoal::targetTag),
            ResourceLocation.CODEC.listOf().optionalFieldOf("ignored_components", Collections.emptyList()).forGetter(ProjectGoal::ignoredComponents)
    ).apply(instance, ProjectGoal::new));

    // Compatibility Constructor for code that hasn't updated to the new list field yet
    public ProjectGoal(UUID id, Item item, int targetAmount, boolean strict, Optional<DataComponentPatch> components, Optional<TagKey<Item>> targetTag) {
        this(id, item, targetAmount, strict, components, targetTag, Collections.emptyList());
    }

    // Convenience Constructor
    public ProjectGoal(Item item, int targetAmount) {
        this(UUID.randomUUID(), item, targetAmount, false, Optional.empty(), Optional.empty(), Collections.emptyList());
    }

    public static ProjectGoal fromStack(ItemStack stack, int targetAmount) {
        return new ProjectGoal(
                UUID.randomUUID(),
                stack.getItem(),
                targetAmount,
                false, // Default to loose matching
                Optional.of(stack.getComponentsPatch()),
                Optional.empty(),
                Collections.emptyList()
        );
    }

    public ItemStack getRenderStack() {
        ItemStack stack = new ItemStack(item);
        components.ifPresent(stack::applyComponents);
        return stack;
    }
}