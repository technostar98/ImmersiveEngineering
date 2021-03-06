package blusunrize.immersiveengineering.common.blocks;

import net.minecraft.block.properties.IProperty;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.Entity;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.Packet;
import net.minecraft.network.play.INetHandlerPlayClient;
import net.minecraft.network.play.server.S35PacketUpdateTileEntity;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.BlockPos;
import net.minecraft.world.World;

public abstract class TileEntityIEBase extends TileEntity
{
	@Override
	public void readFromNBT(NBTTagCompound nbt)
	{
		super.readFromNBT(nbt);
		this.readCustomNBT(nbt, false);
	}
	public abstract void readCustomNBT(NBTTagCompound nbt, boolean descPacket);
	@Override
	public void writeToNBT(NBTTagCompound nbt)
	{
		super.writeToNBT(nbt);
		this.writeCustomNBT(nbt, false);
	}
	public abstract void writeCustomNBT(NBTTagCompound nbt, boolean descPacket);
	
	@Override
	public Packet<INetHandlerPlayClient> getDescriptionPacket()
	{
		NBTTagCompound nbttagcompound = new NBTTagCompound();
		this.writeCustomNBT(nbttagcompound, true);
		return new S35PacketUpdateTileEntity(this.pos, 3, nbttagcompound);
	}
	@Override
	public void onDataPacket(NetworkManager net, S35PacketUpdateTileEntity pkt)
    {
		this.readCustomNBT(pkt.getNbtCompound(), true);
    }
	
	public void receiveMessageFromClient(NBTTagCompound message)
	{
	}
	public void receiveMessageFromServer(NBTTagCompound message)
	{
	}
	
	public void onEntityCollision(World world, Entity entity)
	{
	}
	@Override
	public boolean receiveClientEvent(int id, int type)
	{
		if (id==0||id==255)
		{
			worldObj.markBlockForUpdate(getPos());
			return true;
		}
		return super.receiveClientEvent(id, type);
	}
	@Override
	public boolean shouldRefresh(World world, BlockPos pos, IBlockState oldState, IBlockState newState)
	{
		if (world.isBlockLoaded(pos))
				newState = world.getBlockState(pos);
		if (oldState.getBlock()!=newState.getBlock()||!(oldState.getBlock() instanceof BlockIEBase)||!(newState.getBlock() instanceof BlockIEBase))
			return true;
		IProperty type = ((BlockIEBase)oldState.getBlock()).getMetaProperty();
		if (oldState.getValue(type)!=newState.getValue(type))
			return true;
		return false;
	}
}