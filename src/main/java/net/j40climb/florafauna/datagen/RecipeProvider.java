package net.j40climb.florafauna.datagen;

import net.j40climb.florafauna.FloraFauna;
import net.j40climb.florafauna.block.ModBlocks;
import net.j40climb.florafauna.item.ModItems;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.data.recipes.*;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.*;
import net.minecraft.world.level.ItemLike;
import net.neoforged.neoforge.common.conditions.IConditionBuilder;

import java.util.List;
import java.util.concurrent.CompletableFuture;

public class RecipeProvider extends net.minecraft.data.recipes.RecipeProvider implements IConditionBuilder {
    public RecipeProvider(PackOutput pOutput, CompletableFuture<HolderLookup.Provider> pRegistries) {
        super(pOutput, pRegistries);
    }

    @Override
    protected void buildRecipes(RecipeOutput pRecipeOutput) {
        List<ItemLike> BLACK_OPAL_SMELTABLES = List.of(ModItems.RAW_BLACK_OPAL,
                ModBlocks.BLACK_OPAL_ORE, ModBlocks.BLACK_OPAL_DEEPSLATE_ORE, ModBlocks.BLACK_OPAL_END_ORE, ModBlocks.BLACK_OPAL_NETHER_ORE);

        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ModBlocks.BLACK_OPAL_BLOCK.get())
                .pattern("BBB")
                .pattern("BBB")
                .pattern("BBB")
                .define('B', ModItems.BLACK_OPAL.get())
                .unlockedBy("has_block_opal", has(ModItems.BLACK_OPAL.get())).save(pRecipeOutput);

        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ModItems.BLACK_OPAL_HAMMER.get())
                .pattern("BBB")
                .pattern(" S ")
                .pattern(" S ")
                .define('B', ModItems.BLACK_OPAL.get())
                .define('S', Items.STICK)
                .unlockedBy("has_block_opal", has(ModItems.BLACK_OPAL.get())).save(pRecipeOutput);

        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ModItems.BLACK_OPAL_HELMET.get())
                .pattern("BBB")
                .pattern("B B")
                .pattern("   ")
                .define('B', ModItems.BLACK_OPAL.get())
                .unlockedBy("has_block_opal", has(ModItems.BLACK_OPAL.get())).save(pRecipeOutput);

        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ModItems.BLACK_OPAL_CHESTPLATE.get())
                .pattern("B B")
                .pattern("BBB")
                .pattern("BBB")
                .define('B', ModItems.BLACK_OPAL.get())
                .unlockedBy("has_block_opal", has(ModItems.BLACK_OPAL.get())).save(pRecipeOutput);

        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ModItems.BLACK_OPAL_LEGGINGS.get())
                .pattern("BBB")
                .pattern("B B")
                .pattern("B B")
                .define('B', ModItems.BLACK_OPAL.get())
                .unlockedBy("has_block_opal", has(ModItems.BLACK_OPAL.get())).save(pRecipeOutput);

        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ModItems.BLACK_OPAL_BOOTS.get())
                .pattern("   ")
                .pattern("B B")
                .pattern("B B")
                .define('B', ModItems.BLACK_OPAL.get())
                .unlockedBy("has_block_opal", has(ModItems.BLACK_OPAL.get())).save(pRecipeOutput);

        ShapelessRecipeBuilder.shapeless(RecipeCategory.MISC, ModItems.BLACK_OPAL.get(), 9)
                .requires(ModBlocks.BLACK_OPAL_BLOCK.get())
                .unlockedBy("has_black_opal_block", has(ModBlocks.BLACK_OPAL_BLOCK.get())).save(pRecipeOutput);

        ShapelessRecipeBuilder.shapeless(RecipeCategory.MISC, ModItems.BLACK_OPAL.get(), 9)
                .requires(ModBlocks.MAGIC_BLOCK.get())
                .unlockedBy("has_magic_block", has(ModBlocks.MAGIC_BLOCK.get())).save(pRecipeOutput, "florafauna:black_opal_2");

        oreSmelting(pRecipeOutput, BLACK_OPAL_SMELTABLES, RecipeCategory.MISC, ModItems.BLACK_OPAL.get(), 0.25f, 200, "black_opal");
        oreBlasting(pRecipeOutput, BLACK_OPAL_SMELTABLES, RecipeCategory.MISC, ModItems.BLACK_OPAL.get(), 0.25f, 100, "black_opal");

        stairBuilder(ModBlocks.BLACK_OPAL_STAIRS.get(), Ingredient.of(ModItems.BLACK_OPAL.get())).group("black_opal")
                        .unlockedBy("has_black_opal", has(ModItems.BLACK_OPAL.get())).save(pRecipeOutput);
        slab(pRecipeOutput, RecipeCategory.BUILDING_BLOCKS, ModBlocks.BLACK_OPAL_SLAB.get(), ModItems.BLACK_OPAL.get());

        pressurePlate(pRecipeOutput, ModBlocks.BLACK_OPAL_PRESSURE_PLATE.get(), ModItems.BLACK_OPAL.get());
        buttonBuilder(ModBlocks.BLACK_OPAL_BUTTON.get(), Ingredient.of(ModItems.BLACK_OPAL.get())).group("black_opal")
                .unlockedBy("has_black_opal", has(ModItems.BLACK_OPAL.get())).save(pRecipeOutput);

        fenceBuilder(ModBlocks.BLACK_OPAL_FENCE.get(), Ingredient.of(ModItems.BLACK_OPAL.get())).group("black_opal")
                .unlockedBy("has_black_opal", has(ModItems.BLACK_OPAL.get())).save(pRecipeOutput);
        fenceGateBuilder(ModBlocks.BLACK_OPAL_FENCE_GATE.get(), Ingredient.of(ModItems.BLACK_OPAL.get())).group("black_opal")
                .unlockedBy("has_black_opal", has(ModItems.BLACK_OPAL.get())).save(pRecipeOutput);
        wall(pRecipeOutput, RecipeCategory.BUILDING_BLOCKS, ModBlocks.BLACK_OPAL_WALL.get(), ModItems.BLACK_OPAL.get());

        doorBuilder(ModBlocks.BLACK_OPAL_DOOR.get(), Ingredient.of(ModItems.BLACK_OPAL.get())).group("black_opal")
                .unlockedBy("has_black_opal", has(ModItems.BLACK_OPAL.get())).save(pRecipeOutput);
        trapdoorBuilder(ModBlocks.BLACK_OPAL_TRAPDOOR.get(), Ingredient.of(ModItems.BLACK_OPAL.get())).group("black_opal")
                .unlockedBy("has_black_opal", has(ModItems.BLACK_OPAL.get())).save(pRecipeOutput);
    }

    protected static void oreSmelting(RecipeOutput pRecipeOutput, List<ItemLike> pIngredients, RecipeCategory pCategory, ItemLike pResult,
                                      float pExperience, int pCookingTIme, String pGroup) {
        oreCooking(pRecipeOutput, RecipeSerializer.SMELTING_RECIPE, SmeltingRecipe::new, pIngredients, pCategory, pResult,
                pExperience, pCookingTIme, pGroup, "_from_smelting");
    }

    protected static void oreBlasting(RecipeOutput pRecipeOutput, List<ItemLike> pIngredients, RecipeCategory pCategory, ItemLike pResult,
                                      float pExperience, int pCookingTime, String pGroup) {
        oreCooking(pRecipeOutput, RecipeSerializer.BLASTING_RECIPE, BlastingRecipe::new, pIngredients, pCategory, pResult,
                pExperience, pCookingTime, pGroup, "_from_blasting");
    }

    protected static <T extends AbstractCookingRecipe> void oreCooking(RecipeOutput pRecipeOutput, RecipeSerializer<T> pCookingSerializer, AbstractCookingRecipe.Factory<T> factory,
                                                                       List<ItemLike> pIngredients, RecipeCategory pCategory, ItemLike pResult, float pExperience, int pCookingTime, String pGroup, String pRecipeName) {
        for(ItemLike itemlike : pIngredients) {
            SimpleCookingRecipeBuilder.generic(Ingredient.of(itemlike), pCategory, pResult, pExperience, pCookingTime, pCookingSerializer, factory).group(pGroup).unlockedBy(getHasName(itemlike), has(itemlike))
                    .save(pRecipeOutput, FloraFauna.MOD_ID + ":" + getItemName(pResult) + pRecipeName + "_" + getItemName(itemlike));
        }
    }
}
