package logisticspipes.proxy.interfaces;

import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;

public interface IElectricItemProxy {

	public abstract boolean isElectricItem(ItemStack stack);

	//public abstract boolean isElectricItem(ItemIdentifier item);

	public abstract int getCharge(ItemStack stack);

	public abstract int getMaxCharge(ItemStack stack);

	public abstract boolean isDischarged(ItemStack stack, boolean partial);

	public abstract boolean isCharged(ItemStack stack, boolean partial);

	public abstract boolean isDischarged(ItemStack stack, boolean partial, Item electricItem);

	public abstract boolean isCharged(ItemStack stack, boolean partial, Item electricItem);

	public abstract void addCraftingRecipes();

	public abstract boolean hasIC2();

}
