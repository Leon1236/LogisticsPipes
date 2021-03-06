/** 
 * Copyright (c) Krapht, 2011
 * 
 * "LogisticsPipes" is distributed under the terms of the Minecraft Mod Public 
 * License 1.0, or MMPL. Please check the contents of the license located in
 * http://www.mod-buildcraft.com/MMPL-1.0.txt
 */

package logisticspipes.routing;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import logisticspipes.interfaces.routing.IRequireReliableTransport;
import logisticspipes.logisticspipes.IRoutedItem;
import logisticspipes.pipes.PipeLogisticsChassi;
import logisticspipes.proxy.MainProxy;
import logisticspipes.proxy.SimpleServiceLocator;
import logisticspipes.utils.ItemIdentifier;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.world.World;
import net.minecraftforge.common.ForgeDirection;
import buildcraft.BuildCraftCore;
import buildcraft.api.core.Position;
import buildcraft.api.transport.IPipedItem;
import buildcraft.core.EntityPassiveItem;
import buildcraft.core.proxy.CoreProxy;
import buildcraft.transport.PipeTransportItems;
import buildcraft.transport.TileGenericPipe;

public class RoutedEntityItem extends EntityPassiveItem implements IRoutedItem{

	public UUID sourceUUID;
	public UUID destinationUUID;
	
	private boolean _doNotBuffer;
	
	private int bufferCounter = 0;
	
	public boolean arrived;
	public boolean reRoute;
	public boolean isUnrouted;
	
	private TransportMode _transportMode = TransportMode.Unknown;
	
	public List<UUID> jamlist = new ArrayList<UUID>();
	
	public RoutedEntityItem(World world, IPipedItem entityItem) {
		super(world, entityItem.getEntityId());
		container = entityItem.getContainer();
		position = entityItem.getPosition();
		speed = entityItem.getSpeed();
		item = entityItem.getItemStack();
	}
	
	@Override
	public EntityItem toEntityItem(ForgeDirection dir) {
		if (!CoreProxy.proxy.isRenderWorld(worldObj)) {
			if (getItemStack().stackSize <= 0) {
				return null;
			}

			Position motion = new Position(0, 0, 0, dir);
			motion.moveForwards(0.1 + getSpeed() * 2F);

			EntityItem entityitem = new EntityItem(worldObj, position.x, position.y, position.z, getItemStack());

			entityitem.lifespan = BuildCraftCore.itemLifespan;
			entityitem.delayBeforeCanPickup = 10;

			float f3 = 0.00F + worldObj.rand.nextFloat() * 0.01F - 0.02F;
			entityitem.motionX = (float) worldObj.rand.nextGaussian() * f3 + motion.x;
			entityitem.motionY = (float) worldObj.rand.nextGaussian() * f3 + motion.y;
			entityitem.motionZ = (float) worldObj.rand.nextGaussian() * f3 + motion.z;
			worldObj.spawnEntityInWorld(entityitem);
			remove();

			return entityitem;
		} else {
			return null;
		}
	}
	
	@Override
	public void changeDestination(UUID newDestination){
		if (destinationUUID != null && SimpleServiceLocator.routerManager.isRouter(destinationUUID)){
			IRouter destinationRouter = SimpleServiceLocator.routerManager.getRouter(destinationUUID);

			destinationRouter.itemDropped(this);
			
			if (destinationRouter.getPipe() != null && destinationRouter.getPipe().logic instanceof IRequireReliableTransport){
				((IRequireReliableTransport)destinationRouter.getPipe().logic).itemLost(ItemIdentifier.get(item));
			}
		}
		destinationUUID = newDestination;
		if(newDestination != null) {
			isUnrouted = false;
		}
	}
	
	@Override
	public void remove() {
		if(MainProxy.isClient()) return;
		if (sourceUUID != null && SimpleServiceLocator.routerManager.isRouter(sourceUUID)) {
			SimpleServiceLocator.routerManager.getRouter(sourceUUID).itemDropped(this);
		}
		
		if (destinationUUID != null && SimpleServiceLocator.routerManager.isRouter(destinationUUID)){
			IRouter destinationRouter = SimpleServiceLocator.routerManager.getRouter(destinationUUID); 
			destinationRouter.itemDropped(this);
			if (!arrived && destinationRouter.getPipe() != null && destinationRouter.getPipe().logic instanceof IRequireReliableTransport){
				((IRequireReliableTransport)destinationRouter.getPipe().logic).itemLost(ItemIdentifier.get(item));
			}
		}
		super.remove();
	}

	@Override
	public UUID getDestination() {
		return this.destinationUUID;
	}

	@Override
	public ItemStack getItemStack() {
		return this.item;
	}

	@Override
	public void setDestination(UUID destination) {
		this.destinationUUID = destination;
		if(destination != null) {
			isUnrouted = false;
		}
	}

	@Override
	public UUID getSource() {
		return this.sourceUUID;
	}

	@Override
	public void setSource(UUID source) {
		this.sourceUUID = source;
	}

	@Override
	public void setDoNotBuffer(boolean isBuffered) {
		_doNotBuffer = isBuffered;
	}

	@Override
	public boolean getDoNotBuffer() {
		return _doNotBuffer;
	}

	@Override
	public EntityPassiveItem getEntityPassiveItem() {
		return this;
	}

	@Override
	@Deprecated
	public void setArrived(boolean flag) {
		this.arrived = flag;
	}

	@Override
	public boolean getArrived() {
		return this.arrived;
	}

	@Override
	public IRoutedItem split(World worldObj, int itemsToTake, ForgeDirection orientation) {
		EntityPassiveItem newItem = new EntityPassiveItem(worldObj);
		newItem.setPosition(position.x, position.y, position.z);
		newItem.setSpeed(this.speed);
		newItem.setItemStack(this.item.splitStack(itemsToTake));
		
		if (this.container instanceof TileGenericPipe && ((TileGenericPipe)this.container).pipe.transport instanceof PipeTransportItems){
			if (((TileGenericPipe)this.container).pipe instanceof PipeLogisticsChassi){
				PipeLogisticsChassi chassi = (PipeLogisticsChassi) ((TileGenericPipe)this.container).pipe;
				chassi.queueRoutedItem(SimpleServiceLocator.buildCraftProxy.CreateRoutedItem(worldObj, newItem), orientation.getOpposite());
			} else {
				((PipeTransportItems)((TileGenericPipe)this.container).pipe.transport).entityEntering(newItem, orientation);
			}
		}
		
		return SimpleServiceLocator.buildCraftProxy.CreateRoutedItem(worldObj, newItem);
	}

	@Override
	public void SetPosition(double x, double y, double z) {
		this.position = new Position(x,y,z);
	}

	@Override
	public void setTransportMode(TransportMode transportMode) {
		this._transportMode = transportMode;
		
	}

	@Override
	public TransportMode getTransportMode() {
		return this._transportMode;
	}
	
	public boolean hasContributions() {
		return true;
		/*
		//prevent groupEntities()
		try {
			@SuppressWarnings("restriction")
			final Class<?> caller = sun.reflect.Reflection.getCallerClass(3);
			if(caller.equals(PipeTransportItems.class)) {
				return true;
			}
			return super.hasContributions();
		} catch(Exception e) {
			return true;
		}
		*/
	}
	
	@Override
	public void readFromNBT(NBTTagCompound nbttagcompound) {
		super.readFromNBT(nbttagcompound);
		if(nbttagcompound.hasKey("sourceUUID")) {
			sourceUUID = UUID.fromString(nbttagcompound.getString("sourceUUID"));
		}
		if(nbttagcompound.hasKey("destinationUUID")) {
			destinationUUID = UUID.fromString(nbttagcompound.getString("destinationUUID"));
		}
		arrived = nbttagcompound.getBoolean("arrived");
		bufferCounter = nbttagcompound.getInteger("bufferCounter");
	}

	@Override
	public void writeToNBT(NBTTagCompound nbttagcompound) {
		super.writeToNBT(nbttagcompound);
		if(sourceUUID != null) {
			nbttagcompound.setString("sourceUUID", sourceUUID.toString());
		}
		if(destinationUUID != null) {
			nbttagcompound.setString("destinationUUID", destinationUUID.toString());
		}
		nbttagcompound.setBoolean("arrived", arrived);
		nbttagcompound.setInteger("bufferCounter", bufferCounter);
		
	}

	@Override
	public IRoutedItem getNewUnRoutedItem() {
		EntityPassiveItem Entityitem = new EntityPassiveItem(worldObj, entityId);
		Entityitem.setContainer(container);
		Entityitem.setPosition(position.x, position.y, position.z);
		Entityitem.setSpeed(speed);
		Entityitem.setItemStack(item);
		RoutedEntityItem routed = new RoutedEntityItem(worldObj, Entityitem);
		routed.isUnrouted = true;
		routed.jamlist.addAll(jamlist);
		return routed;
	}

	@Override
	public EntityPassiveItem getNewEntityPassiveItem() {
		EntityPassiveItem Entityitem = new EntityPassiveItem(worldObj, entityId);
		Entityitem.setContainer(container);
		Entityitem.setPosition(position.x, position.y, position.z);
		Entityitem.setSpeed(speed);
		Entityitem.setItemStack(item);
		return Entityitem;
	}

	@Override
	public boolean isReRoute() {
		return reRoute;
	}

	@Override
	public void setReRoute(boolean flag) {
		reRoute = flag;
	}

	@Override
	public void addToJamList(IRouter router) {
		jamlist.add(router.getId());
	}

	@Override
	public List<UUID> getJamList() {
		return jamlist;
	}

	@Override
	public boolean isUnRouted() {
		return isUnrouted;
	}

	@Override
	public int getBufferCounter() {
		return bufferCounter;
	}

	@Override
	public void setBufferCounter(int counter) {
		bufferCounter = counter;
	}
}
