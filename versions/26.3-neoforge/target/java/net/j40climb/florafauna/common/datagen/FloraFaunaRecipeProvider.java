package net.j40climb.florafauna.common.datagen;

import net.j40climb.florafauna.FloraFauna;
import net.j40climb.florafauna.common.block.wood.WoodBlockSet;
import net.j40climb.florafauna.common.block.wood.WoodType;
import net.j40climb.florafauna.setup.FloraFaunaRegistry;
import net.minecraft.advancements.Advancement;
import net.minecraft.data.recipes.*;
import net.minecraft.data.worldgen.BootstrapContext;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.level.ItemLike;

public class FloraFaunaRecipeProvider extends RecipeProvider {
    public FloraFaunaRecipeProvider(BootstrapContext<Recipe<?>> recipes, BootstrapContext<Advancement> advancements) {
        super(recipes, advancements, FloraFauna.MOD_ID);
    }

    @Override
    public void buildRecipes() {
        // Wood recipes - iterate through all wood types
        for (WoodType woodType : WoodType.values()) {
            WoodBlockSet wood = woodType.getBlockSet();
            String name = woodType.getName();

            // Logs -> 4 Planks (shapeless, from any of the 4 log types)
            planksFromLog(wood.planks().get(), wood.log().get(), name);
            planksFromLog(wood.planks().get(), wood.strippedLog().get(), "stripped_" + name);
            planksFromLog(wood.planks().get(), wood.wood().get(), name + "_wood");
            planksFromLog(wood.planks().get(), wood.strippedWood().get(), "stripped_" + name + "_wood");

            // Wood from logs (2x2 logs -> 3 wood)
            woodFromLogs(wood.wood().get(), wood.log().get(), name);
            woodFromLogs(wood.strippedWood().get(), wood.strippedLog().get(), "stripped_" + name);

            // Slab from planks (3 planks -> 6 slabs)
            slabFromPlanks(wood.slab().get(), wood.planks().get(), name);

            // Fence from planks + sticks (4 planks + 2 sticks -> 3 fences)
            fenceFromPlanks(wood.fence().get(), wood.planks().get(), name);

            // Fence gate from planks + sticks (2 planks + 4 sticks -> 1 fence gate)
            fenceGateFromPlanks(wood.fenceGate().get(), wood.planks().get(), name);
        }

        // Iron Garden System recipes
        // Ferric Poppy -> 3 Iron Nuggets (shapeless)
        ShapelessRecipeBuilder.shapeless(items, RecipeCategory.MISC, Items.IRON_NUGGET, 3)
                .requires(FloraFaunaRegistry.FERRIC_POPPY_ITEM.get())
                .unlockedBy("has_ferric_poppy", has(FloraFaunaRegistry.FERRIC_POPPY_ITEM.get()))
                .save(output, FloraFauna.MOD_ID + ":iron_nuggets_from_ferric_poppy");
    }

    private void planksFromLog(ItemLike planks, ItemLike log, String logName) {
        ShapelessRecipeBuilder.shapeless(items, RecipeCategory.BUILDING_BLOCKS, planks, 4)
                .requires(log)
                .group("planks")
                .unlockedBy("has_log", has(log))
                .save(output, FloraFauna.MOD_ID + ":planks_from_" + logName);
    }

    private void woodFromLogs(ItemLike wood, ItemLike log, String name) {
        ShapedRecipeBuilder.shaped(items, RecipeCategory.BUILDING_BLOCKS, wood, 3)
                .pattern("##")
                .pattern("##")
                .define('#', log)
                .group("bark")
                .unlockedBy("has_log", has(log))
                .save(output, FloraFauna.MOD_ID + ":" + name + "_wood_from_logs");
    }

    private void slabFromPlanks(ItemLike slab, ItemLike planks, String name) {
        ShapedRecipeBuilder.shaped(items, RecipeCategory.BUILDING_BLOCKS, slab, 6)
                .pattern("###")
                .define('#', planks)
                .group("wooden_slab")
                .unlockedBy("has_planks", has(planks))
                .save(output);
    }

    private void fenceFromPlanks(ItemLike fence, ItemLike planks, String name) {
        ShapedRecipeBuilder.shaped(items, RecipeCategory.DECORATIONS, fence, 3)
                .pattern("#S#")
                .pattern("#S#")
                .define('#', planks)
                .define('S', Items.STICK)
                .group("wooden_fence")
                .unlockedBy("has_planks", has(planks))
                .save(output);
    }

    private void fenceGateFromPlanks(ItemLike fenceGate, ItemLike planks, String name) {
        ShapedRecipeBuilder.shaped(items, RecipeCategory.REDSTONE, fenceGate, 1)
                .pattern("S#S")
                .pattern("S#S")
                .define('#', planks)
                .define('S', Items.STICK)
                .group("wooden_fence_gate")
                .unlockedBy("has_planks", has(planks))
                .save(output);
    }
}
