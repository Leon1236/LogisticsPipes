package logisticspipes.proxy.side;

import java.io.File;

import logisticspipes.LogisticsPipes;
import logisticspipes.blocks.LogisticsSignTileEntity;
import logisticspipes.blocks.LogisticsSolderingTileEntity;
import logisticspipes.network.packets.PacketNameUpdatePacket;
import logisticspipes.proxy.SimpleServiceLocator;
import logisticspipes.proxy.interfaces.IProxy;
import logisticspipes.utils.ItemIdentifier;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.network.packet.Packet250CustomPayload;
import net.minecraft.world.World;
import net.minecraftforge.common.Configuration;
import net.minecraftforge.common.DimensionManager;
import cpw.mods.fml.common.network.Player;
import cpw.mods.fml.common.registry.GameRegistry;
import cpw.mods.fml.server.FMLServerHandler;

public class ServerProxy implements IProxy {
	
	private Configuration langDatabase;
	private long saveThreadTime = 0;
	
	public ServerProxy() {
		langDatabase = new Configuration(new File("config/LogisticsPipes-LangDatabase.cfg"));
	}
	
	@Override
	public String getSide() {
		return "Server";
	}

	@Override
	public World getWorld() {
		return null;
	}

	@Override
	public void registerTileEntitis() {
		GameRegistry.registerTileEntity(LogisticsSignTileEntity.class, "net.minecraft.src.buildcraft.logisticspipes.blocks.LogisticsTileEntiy");
		GameRegistry.registerTileEntity(LogisticsSignTileEntity.class, "logisticspipes.blocks.LogisticsSignTileEntity");
		GameRegistry.registerTileEntity(LogisticsSolderingTileEntity.class, "logisticspipes.blocks.LogisticsSolderingTileEntity");
		GameRegistry.registerTileEntity(LogisticsPipes.powerTileEntity, "logisticspipes.blocks.powertile.LogisticsPowerJuntionTileEntity");
		GameRegistry.registerTileEntity(LogisticsPipes.logisticsTileGenericPipe, LogisticsPipes.logisticsTileGenericPipeMapping);
	}

	@Override
	public World getWorld(int _dimension) {
		return DimensionManager.getWorld(_dimension);
	}

	@Override
	public EntityPlayer getClientPlayer() {
		return null;
	}

	@Override
	public boolean isMainThreadRunning() {
		return FMLServerHandler.instance().getServer().isServerRunning();
	}

	@Override
	public void addLogisticsPipesOverride(int index, String override1, String override2) {
		//Only Client Side
	}

	@Override
	public void registerParticles() {
		//Only Client Side
	}
	
	private String tryGetName(ItemIdentifier item) {
		String name = "???";
		try {
			name = Item.itemsList[item.itemID].getItemDisplayName(item.makeNormalStack(1));
			if(name == null) {
				throw new Exception();
			}
		} catch(Exception e) {
			try {
				name = Item.itemsList[item.itemID].getItemNameIS(item.makeNormalStack(1));
				if(name == null) {
					throw new Exception();
				}
			} catch(Exception e1) {
				try {
					name = Item.itemsList[item.itemID].getItemName();
					if(name == null) {
						throw new Exception();
					}
				} catch(Exception e2) {
					name = "???"; 
				}
			}
		}
		return name;
	}
	
	private String getNameForCategory(String category, ItemIdentifier item) {
		String name = langDatabase.get(category, "name", "").value;
		if(name.equals("")) {
			saveLangDatabase();
			if(item.makeNormalStack(1).isItemStackDamageable()) {
				return tryGetName(item);
			} else {
				return  "LP|UNDEFINED";
			}
		}
		return name;
	}
	
	private void setNameForCategory(String category, ItemIdentifier item, String newName) {
		langDatabase.get(category, "name", newName).value = newName;
		saveLangDatabase();
	}
	
	private void saveLangDatabase() {
		saveThreadTime = System.currentTimeMillis() + 30 * 1000;
	}
	
	@Override
	public String getName(ItemIdentifier item) {
		String category = "";
		if(item.makeNormalStack(1).isItemStackDamageable()) {
			category = "itemNames." + Integer.toString(item.itemID);
		} else {
			if(item.itemDamage == 0) {
				category = "itemNames." + Integer.toString(item.itemID);
			} else {
				category = "itemNames." + Integer.toString(item.itemID) + "." + Integer.toString(item.itemDamage);
			}
		}
		String name = getNameForCategory(category, item);
		if(name.equals("LP|UNDEFINED")) {
			if(item.itemDamage == 0) {
				return tryGetName(item);
			} else {
				category = "itemNames." + Integer.toString(item.itemID);
				name = getNameForCategory(category, item);
				if(name.equals("LP|UNDEFINED")) {
					return tryGetName(item);
				}
			}
		}
		return name;
	}

	@Override
	public void updateNames(ItemIdentifier item, String name) {
		String category = "";
		if(item.makeNormalStack(1).isItemStackDamageable()) {
			category = "itemNames." + Integer.toString(item.itemID);
		} else {
			if(item.itemDamage == 0) {
				category = "itemNames." + Integer.toString(item.itemID);
			} else {
				category = "itemNames." + Integer.toString(item.itemID) + "." + Integer.toString(item.itemDamage);
			}
		}
		setNameForCategory(category, item, name);
	}

	@Override
	public void tick() {
		//Save Language Database
		if(saveThreadTime != 0) {
			if(saveThreadTime < System.currentTimeMillis()) {
				saveThreadTime = 0;
				langDatabase.save();
				LogisticsPipes.log.info("LangDatabase saved");
			}
		}
	}

	@Override
	public void sendNameUpdateRequest(Player player) {
		for(String category:langDatabase.categories.keySet()) {
			if(!category.startsWith("itemNames.")) continue;
			String name = langDatabase.get(category, "name", "").value;
			if(name.equals("")) {
				String itemPart = category.substring(10);
				String metaPart = "0";
				if(itemPart.contains(".")) {
					String[] itemPartSplit = itemPart.split(".");
					itemPart = itemPartSplit[0];
					metaPart = itemPartSplit[1];
				}
				int id = Integer.valueOf(itemPart);
				int meta = Integer.valueOf(metaPart);
				SimpleServiceLocator.serverBufferHandler.addPacketToCompressor((Packet250CustomPayload) new PacketNameUpdatePacket(ItemIdentifier.get(id, meta, null), "-").getPacket(), player);
			}
		}
	}
}
