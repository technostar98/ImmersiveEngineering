package blusunrize.immersiveengineering.common.entities;

import java.util.List;

import blusunrize.immersiveengineering.api.energy.immersiveflux.IFluxContainerItem;
import blusunrize.immersiveengineering.common.Config;
import blusunrize.immersiveengineering.common.util.IEAchievements;
import blusunrize.immersiveengineering.common.util.IEDamageSources;
import blusunrize.immersiveengineering.common.util.Utils;
import blusunrize.immersiveengineering.common.util.chickenbones.Matrix4;
import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemPotion;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.potion.Potion;
import net.minecraft.potion.PotionEffect;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.BlockPos;
import net.minecraft.util.DamageSource;
import net.minecraft.util.EnumParticleTypes;
import net.minecraft.util.MathHelper;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.util.Vec3;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

public class EntityRevolvershot extends Entity
{
	private int xTile = -1;
	private int yTile = -1;
	private int zTile = -1;
	private Block inTile;
	private int inData;
	private boolean inGround;
	public EntityLivingBase shootingEntity;
	private int ticksInGround;
	private int ticksInAir;

	private int tickLimit=40;
	int bulletType = 0;
	public boolean bulletElectro = false;
	public ItemStack bulletPotion = null;
	
	final static int dataMarker_shooter = 12;

	public EntityRevolvershot(World world)
	{
		super(world);
		this.renderDistanceWeight=10;
		this.setSize(.125f,.125f);
	}
	public EntityRevolvershot(World world, double x, double y, double z, double ax, double ay, double az, int type)
	{
		super(world);
		this.setSize(0.125F, 0.125F);
		this.setLocationAndAngles(x, y, z, this.rotationYaw, this.rotationPitch);
		this.setPosition(x, y, z);
		this.bulletType = type;
	}
	public EntityRevolvershot(World world, EntityLivingBase living, double ax, double ay, double az, int type, ItemStack stack)
	{
		super(world);
		this.shootingEntity = living;
		this.setSize(0.125F, 0.125F);
		this.setLocationAndAngles(living.posX+ax, living.posY+living.getEyeHeight()+ay, living.posZ+az, living.rotationYaw, living.rotationPitch);
		this.setPosition(this.posX, this.posY, this.posZ);
		//		this.yOffset = 0.0F;
		this.motionX = this.motionY = this.motionZ = 0.0D;
		this.bulletType = type;
	}

	public void setTickLimit(int limit)
	{
		this.tickLimit=limit;
	}


	@SideOnly(Side.CLIENT)
	@Override
	public boolean isInRangeToRenderDist(double p_70112_1_)
	{
		double d1 = this.getEntityBoundingBox().getAverageEdgeLength() * 4.0D;
		d1 *= 64.0D;
		return p_70112_1_ < d1 * d1;
	}

	@Override
	protected void entityInit()
	{
		this.dataWatcher.addObject(dataMarker_shooter, "");
	}

	public void setShooterSynced()
	{
		this.dataWatcher.updateObject(dataMarker_shooter, this.shootingEntity.getName());
	}
	public EntityLivingBase getShooterSynced()
	{
		return this.worldObj.getPlayerEntityByName(this.dataWatcher.getWatchableObjectString(dataMarker_shooter));
	}
	public Entity getShooter()
	{
		return shootingEntity;
	}

	@Override
	public void onUpdate()
	{
		if(this.getShooter() == null && this.worldObj.isRemote)
			this.shootingEntity = getShooterSynced();
		
		if(!this.worldObj.isRemote && (this.shootingEntity != null && this.shootingEntity.isDead))
			this.setDead();
		else
		{
			BlockPos blockpos = new BlockPos(this.xTile, this.yTile, this.zTile);
			IBlockState iblockstate = this.worldObj.getBlockState(blockpos);
			Block block = iblockstate.getBlock();

			if (block.getMaterial() != Material.air)
			{
				block.setBlockBoundsBasedOnState(this.worldObj, blockpos);
				AxisAlignedBB axisalignedbb = block.getCollisionBoundingBox(this.worldObj, blockpos, iblockstate);

				if (axisalignedbb != null && axisalignedbb.isVecInside(new Vec3(this.posX, this.posY, this.posZ)))
				{
					this.inGround = true;
				}
			}

			super.onUpdate();

			if(this.inGround)
			{
				int j = block.getMetaFromState(iblockstate);

				if(block==this.inTile && j==this.inData)
				{
					++this.ticksInGround;

					if (this.ticksInGround >= 1200)
					{
						this.setDead();
					}
				}
				else
				{
					this.inGround = false;
					this.motionX *= (double)(this.rand.nextFloat() * 0.2F);
					this.motionY *= (double)(this.rand.nextFloat() * 0.2F);
					this.motionZ *= (double)(this.rand.nextFloat() * 0.2F);
					this.ticksInGround = 0;
					this.ticksInAir = 0;
				}
			}
			else
				++this.ticksInAir;

			if(ticksInAir>=tickLimit)
			{
				this.onExpire();
				this.setDead();
				return;
			}

			Vec3 vec3 = new Vec3(this.posX, this.posY, this.posZ);
			Vec3 vec31 = new Vec3(this.posX + this.motionX, this.posY + this.motionY, this.posZ + this.motionZ);
			MovingObjectPosition movingobjectposition = this.worldObj.rayTraceBlocks(vec3, vec31);
			vec3 = new Vec3(this.posX, this.posY, this.posZ);
			vec31 = new Vec3(this.posX + this.motionX, this.posY + this.motionY, this.posZ + this.motionZ);

			if (movingobjectposition != null)
				vec31 = new Vec3(movingobjectposition.hitVec.xCoord, movingobjectposition.hitVec.yCoord, movingobjectposition.hitVec.zCoord);

			Entity entity = null;
			List list = this.worldObj.getEntitiesWithinAABBExcludingEntity(this, this.getEntityBoundingBox().addCoord(this.motionX, this.motionY, this.motionZ).expand(1.0D, 1.0D, 1.0D));
			double d0 = 0.0D;

			for (int i = 0; i < list.size(); ++i)
			{
				Entity entity1 = (Entity)list.get(i);
				if (entity1.canBeCollidedWith() && (!entity1.isEntityEqual(this.shootingEntity)))
				{
					float f = 0.3F;
					AxisAlignedBB axisalignedbb = entity1.getEntityBoundingBox().expand((double)f, (double)f, (double)f);
					MovingObjectPosition movingobjectposition1 = axisalignedbb.calculateIntercept(vec3, vec31);

					if (movingobjectposition1 != null)
					{
						double d1 = vec3.distanceTo(movingobjectposition1.hitVec);
						if (d1 < d0 || d0 == 0.0D)
						{
							entity = entity1;
							d0 = d1;
						}
					}
				}
			}

			if (entity != null)
				movingobjectposition = new MovingObjectPosition(entity);

			if (movingobjectposition != null)
				this.onImpact(movingobjectposition);

			this.posX += this.motionX;
			this.posY += this.motionY;
			this.posZ += this.motionZ;
			float f1 = MathHelper.sqrt_double(this.motionX * this.motionX + this.motionZ * this.motionZ);
			this.rotationYaw = (float)(Math.atan2(this.motionZ, this.motionX) * 180.0D / Math.PI) + 90.0F;

			for (this.rotationPitch = (float)(Math.atan2((double)f1, this.motionY) * 180.0D / Math.PI) - 90.0F; this.rotationPitch - this.prevRotationPitch < -180.0F; this.prevRotationPitch -= 360.0F);

			while (this.rotationPitch - this.prevRotationPitch >= 180.0F)
				this.prevRotationPitch += 360.0F;
			while (this.rotationYaw - this.prevRotationYaw < -180.0F)
				this.prevRotationYaw -= 360.0F;
			while (this.rotationYaw - this.prevRotationYaw >= 180.0F)
				this.prevRotationYaw += 360.0F;

			this.rotationPitch = this.prevRotationPitch + (this.rotationPitch - this.prevRotationPitch) * 0.2F;
			this.rotationYaw = this.prevRotationYaw + (this.rotationYaw - this.prevRotationYaw) * 0.2F;

			if (this.isInWater())
			{
				for (int j = 0; j < 4; ++j)
				{
					float f3 = 0.25F;
					this.worldObj.spawnParticle(EnumParticleTypes.WATER_BUBBLE, this.posX - this.motionX * (double)f3, this.posY - this.motionY * (double)f3, this.posZ - this.motionZ * (double)f3, this.motionX, this.motionY, this.motionZ);
				}
			}

			if(ticksExisted%4==0)
				this.worldObj.spawnParticle(EnumParticleTypes.SMOKE_NORMAL, this.posX, this.posY, this.posZ, 0.0D, 0.0D, 0.0D);
			this.setPosition(this.posX, this.posY, this.posZ);
		}
	}

	protected void onImpact(MovingObjectPosition mop)
	{
		boolean headshot = false;

		if(mop.entityHit != null)
		{
			if(mop.entityHit instanceof EntityLivingBase)
				headshot = Utils.isVecInEntityHead((EntityLivingBase)mop.entityHit, new Vec3(posX,posY,posZ));

			String dmgKey = bulletType==0?"Casull" :bulletType==1?"AP":
				bulletType==2?"Buck" :bulletType==4?"Dragon":
					bulletType==5?"Homing" :bulletType==6?"Wolfpack":
						bulletType==7?"Silver" :bulletType==8?"Potion": "";
			double damage = Config.getDouble("BulletDamage-"+dmgKey);
			DamageSource damageSrc = null;
			if(bulletType==0)
				damageSrc = IEDamageSources.causeCasullDamage(this, shootingEntity);
			else if(bulletType==1)
				damageSrc = IEDamageSources.causePiercingDamage(this, shootingEntity);
			else if(bulletType==2)
				damageSrc = IEDamageSources.causeBuckshotDamage(this, shootingEntity);
			else if(bulletType==4)
				damageSrc = IEDamageSources.causeDragonsbreathDamage(this, shootingEntity);
			else if(bulletType==5)
				damageSrc = IEDamageSources.causeHomingDamage(this, shootingEntity);
			else if(bulletType==6)
				damageSrc = IEDamageSources.causeWolfpackDamage(this, shootingEntity);
			else if(bulletType==7)
			{
				damageSrc = IEDamageSources.causeSilverDamage(this, shootingEntity);
				if(mop.entityHit instanceof EntityLivingBase && ((EntityLivingBase)mop.entityHit).isEntityUndead())
					damage *= 1.75;
			}
			else if(bulletType==8)
				damageSrc = IEDamageSources.causePotionDamage(this, shootingEntity);

			if(damageSrc!=null)
				if(headshot)
				{
					damage *= 1.5;
					EntityLivingBase living = (EntityLivingBase)mop.entityHit;
					if(living.isChild() && !living.isEntityInvulnerable(damageSrc) && (living.hurtResistantTime>0?living.getHealth()<=0:living.getHealth()<=damage))
					{
						if(this.worldObj.isRemote)
						{
							worldObj.makeFireworks(posX,posY,posZ, 0,0,0, Utils.getRandomFireworkExplosion(worldObj.rand, 4));
							worldObj.playSound(posX,posY,posZ, "immersiveengineering:birthdayParty", 1.5f,1, false);
							mop.entityHit.getEntityData().setBoolean("headshot", true);
						}
						else if(this.shootingEntity instanceof EntityPlayer)
							((EntityPlayer)this.shootingEntity).triggerAchievement(IEAchievements.secret_birthdayParty);
					}
				}

			if(!this.worldObj.isRemote)
			{
				if(mop.entityHit.attackEntityFrom(damageSrc, (float)damage))
				{
					if(bulletType==2)
						mop.entityHit.hurtResistantTime=0;
					if(bulletType==4)
						mop.entityHit.setFire(3);
				}
			}
		}
		if(!this.worldObj.isRemote)
		{
			if(bulletType==3)
				worldObj.createExplosion(shootingEntity, posX, posY, posZ, 2, false);
			this.secondaryImpact(mop);
		}
		this.setDead();
	}
	public void secondaryImpact(MovingObjectPosition mop)
	{
		if(bulletElectro && mop.entityHit instanceof EntityLivingBase)
		{
			((EntityLivingBase)mop.entityHit).addPotionEffect(new PotionEffect(Potion.moveSlowdown.id,15,4));
			for(int i=0; i<=4; i++)
			{
				ItemStack stack = ((EntityLivingBase)mop.entityHit).getEquipmentInSlot(i);
				if(stack!=null && stack.getItem() instanceof IFluxContainerItem)
				{
					int maxStore = ((IFluxContainerItem)stack.getItem()).getMaxEnergyStored(stack);
					int drain = Math.min((int)(maxStore*.15f), ((IFluxContainerItem)stack.getItem()).getEnergyStored(stack));
					int hasDrained = 0;
					while(hasDrained<drain)
					{
						int actualDrain = ((IFluxContainerItem)stack.getItem()).extractEnergy(stack, drain, false);
						if(actualDrain<=0)
							break;
						hasDrained += actualDrain;
					}
				}
//				if(stack!=null && Lib.IC2)
//				{
//					double charge = IC2Helper.getMaxItemCharge(stack);
//					IC2Helper.dischargeItem(stack, charge*.15f);
//				}
			}
		}

		if(bulletType==6)
		{
			Vec3 v = new Vec3(-motionX, -motionY, -motionZ);
			int split = 6;
			for(int i=0; i<split; i++)
			{	
				float angle = i * (360f/split);
				Matrix4 matrix = new Matrix4();
				matrix.rotate(angle, v.xCoord,v.yCoord,v.zCoord);
				Vec3 vecDir = new Vec3(0, 1, 0);
				vecDir = matrix.apply(vecDir);

				EntityWolfpackShot bullet = new EntityWolfpackShot(worldObj, this.shootingEntity, vecDir.xCoord*1.5,vecDir.yCoord*1.5,vecDir.zCoord*1.5, this.bulletType, null);
				if(mop.entityHit instanceof EntityLivingBase)
					bullet.targetOverride = (EntityLivingBase)mop.entityHit;
				bullet.setPosition(posX+vecDir.xCoord, posY+vecDir.yCoord, posZ+vecDir.zCoord);
				bullet.motionX = vecDir.xCoord*.375;
				bullet.motionY = vecDir.yCoord*.375;
				bullet.motionZ = vecDir.zCoord*.375;
				worldObj.spawnEntityInWorld(bullet);
			}
		}
		if(bulletType==8 && bulletPotion!=null && bulletPotion.getItem() instanceof ItemPotion)
		{
			List<PotionEffect> effects = ((ItemPotion)bulletPotion.getItem()).getEffects(bulletPotion);
			if(effects!=null)
//				if(bulletPotion.getItem().getClass().getName().equalsIgnoreCase("ganymedes01.etfuturum.items.LingeringPotion"))
//					EtFuturumHelper.createLingeringPotionEffect(worldObj, posX, posY, posZ, bulletPotion, shootingEntity);
//				else 
					if(ItemPotion.isSplash(bulletPotion.getItemDamage()))
				{
					List<EntityLivingBase> livingEntities = this.worldObj.getEntitiesWithinAABB(EntityLivingBase.class, this.getEntityBoundingBox().expand(4.0D, 2.0D, 4.0D));
					if(livingEntities!=null && !livingEntities.isEmpty())
						for(EntityLivingBase living : livingEntities)
						{
							double dist = this.getDistanceSqToEntity(living);
							if(dist<16D)
							{
								double dist2 = 1-Math.sqrt(dist)/4D;
								if(living == mop.entityHit)
									dist2 = 1D;
								for(PotionEffect p : effects)
								{
									int id = p.getPotionID();
									if(Potion.potionTypes[id].isInstant())
										Potion.potionTypes[id].affectEntity(this, this.shootingEntity, living,  p.getAmplifier(), dist2);
									else
									{
										int j = (int)(dist2*p.getDuration()+.5D);
										if(j>20)
											living.addPotionEffect(new PotionEffect(id, j, p.getAmplifier()));
									}
								}
							}
						}

				}
				else if(mop.entityHit instanceof EntityLivingBase)
					for(PotionEffect p : effects)
					{
						if(p.getDuration()<1)
							p = new PotionEffect(p.getPotionID(),1);
						((EntityLivingBase)mop.entityHit).addPotionEffect(p);
					}
			worldObj.playAuxSFX(2002, new BlockPos(posX, posY, posZ), bulletPotion.getItemDamage());
		}
	}
	public void onExpire()
	{

	}

	protected float getMotionFactor()
	{
		return 0.95F;
	}

	@Override
	//	public void writeToNBT(NBTTagCompound nbt)
	protected void writeEntityToNBT(NBTTagCompound nbt)
	{
		//		super.writeToNBT(nbt);
		nbt.setShort("xTile", (short)this.xTile);
		nbt.setShort("yTile", (short)this.yTile);
		nbt.setShort("zTile", (short)this.zTile);
		nbt.setByte("inTile", (byte)Block.getIdFromBlock(this.inTile));
		nbt.setInteger("inData", this.inData);
		nbt.setByte("inGround", (byte)(this.inGround ? 1 : 0));
		nbt.setTag("direction", this.newDoubleNBTList(new double[] {this.motionX, this.motionY, this.motionZ}));
		nbt.setShort("bulletType", (short)this.bulletType);
		if(bulletPotion!=null)
			nbt.setTag("bulletPotion", bulletPotion.writeToNBT(new NBTTagCompound()));
		if(this.shootingEntity!=null)
			nbt.setString("shootingEntity", this.shootingEntity.getName());
	}

	@Override
	//	public void readFromNBT(NBTTagCompound nbt)
	protected void readEntityFromNBT(NBTTagCompound nbt)
	{
		//		super.readFromNBT(nbt);
		this.xTile = nbt.getShort("xTile");
		this.yTile = nbt.getShort("yTile");
		this.zTile = nbt.getShort("zTile");
		this.inTile = Block.getBlockById(nbt.getByte("inTile") & 255);
		this.inData = nbt.getInteger("inData");
		this.inGround = nbt.getByte("inGround") == 1;
		this.bulletType= nbt.getShort("bulletType");
		if(nbt.hasKey("bulletPotion"))
			this.bulletPotion= ItemStack.loadItemStackFromNBT(nbt.getCompoundTag("bulletPotion"));

		if (nbt.hasKey("direction", 9))
		{
			NBTTagList nbttaglist = nbt.getTagList("direction", 6);
			this.motionX = nbttaglist.getFloatAt(0);
			this.motionY = nbttaglist.getFloatAt(1);
			this.motionZ = nbttaglist.getFloatAt(2);
		}
		else
		{
			this.setDead();
		}
		
		if(this.worldObj!=null)
			this.shootingEntity = this.worldObj.getPlayerEntityByName(nbt.getString("shootingEntity"));
	}

	@Override
	public float getCollisionBorderSize()
	{
		return 1.0F;
	}
	@Override
	public float getBrightness(float p_70013_1_)
	{
		return 1.0F;
	}
	@SideOnly(Side.CLIENT)
	@Override
	public int getBrightnessForRender(float p_70070_1_)
	{
		return 15728880;
	}
	@Override
	public boolean canBeCollidedWith()
	{
		return false;
	}
	@Override
	public boolean attackEntityFrom(DamageSource p_70097_1_, float p_70097_2_)
	{
		return false;
	}
}