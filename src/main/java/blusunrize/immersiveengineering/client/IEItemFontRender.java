package blusunrize.immersiveengineering.client;

import java.util.Arrays;
import java.util.HashMap;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.resources.IReloadableResourceManager;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

@SideOnly(Side.CLIENT)
public class IEItemFontRender extends FontRenderer
{
	int[] backupColours;
	String colourFormattingKeys = "0123456789abcdef";
	public float customSpaceWidth = 4f;
	public float spacingModifier = 0f;
	public boolean verticalBoldness = false;

	public IEItemFontRender()
	{
		super(ClientUtils.mc().gameSettings, new ResourceLocation("textures/font/ascii.png"), ClientUtils.mc().renderEngine, false);
		if (Minecraft.getMinecraft().gameSettings.language != null)
		{
			this.setUnicodeFlag(ClientUtils.mc().getLanguageManager().isCurrentLocaleUnicode());
			this.setBidiFlag(ClientUtils.mc().getLanguageManager().isCurrentLanguageBidirectional());
		}
		((IReloadableResourceManager)ClientUtils.mc().getResourceManager()).registerReloadListener(this);
		this.backupColours = Arrays.copyOf(this.colorCode, 32);
	}

	@Override
	public void renderStringAtPos(String text, boolean shadow)
	{
		int idx = -1;
		int loop = 0;
		HashMap<Integer, Integer> formattingReplacements = new HashMap<Integer, Integer>();
		while((idx=text.indexOf("<hexcol="))>=0 && loop++<20)
		{
			int end = text.indexOf(">",idx);
			if(end>=0)
			{
				String rep = "ERROR";
				String s = text.substring(idx, end+1);
				int formatEnd = s.indexOf(":");
				if(formatEnd>=0)
				{
					rep = s.substring(formatEnd+1, s.length()-1);
					String hex = s.substring("<hexcol=".length(), formatEnd);
					try{
						int hexColour = Integer.parseInt(hex,16);
						int formatting = 0;
						if(formattingReplacements.containsKey(hexColour))
							formatting = formattingReplacements.get(hexColour);
						else
							while(formatting<16 && text.contains("\u00A7"+colourFormattingKeys.charAt(formatting)))
								formatting++;
						if(formatting<16)
						{
							rep = "\u00A7"+colourFormattingKeys.charAt(formatting)+ rep + "\u00A7r";
							this.colorCode[formatting] = hexColour;
							this.colorCode[16+formatting] = ClientUtils.getDarkenedTextColour(hexColour);
						}
						formattingReplacements.put(hexColour, formatting);
					}catch(Exception e){}
				}
				text = text.replace(s, rep);
			}
		}
		if(verticalBoldness)
		{
			float startX = this.posX;
			float startY = this.posY;
			float yOffset = this.getUnicodeFlag()?.5f:1;

			super.renderStringAtPos(text, shadow);
			this.posY=startY+yOffset;
			this.posX=startX;
			super.renderStringAtPos(text, shadow);
			this.posY-=yOffset;
		}
		else
			super.renderStringAtPos(text, shadow);

		this.colorCode = Arrays.copyOf(backupColours, 32);
	}

	@Override
	public float func_181559_a(char ch, boolean italic)
	{
		if(ch==32)
			return customSpaceWidth;
		return super.func_181559_a(ch, italic)+spacingModifier;
	}
	public float getCharWidthFloat(char character)
	{
		if(character==32)
			return customSpaceWidth;
		return super.getCharWidth(character)+spacingModifier;
	}
	@Override
	public int getCharWidth(char character)
	{
		return (int)this.getCharWidthFloat(character);
	}
	@Override
	public int getStringWidth(String text)
	{
		if (text==null)
			return 0;
		else
		{
			float i = 0;
			boolean flag = false;
			for(int j=0; j<text.length(); ++j)
			{
				char c0 = text.charAt(j);
				float k = this.getCharWidthFloat(c0);
				if(k<0 && j<text.length()-1)
				{
					++j;
					c0 = text.charAt(j);

					if(c0!=108 && c0!=76)
					{
						if(c0==114 || c0==82)
							flag = false;
					}
					else
						flag = true;
					k = 0;
				}

				i += k;
				if(flag && k>0)
					++i;
			}
			return (int)i;
		}
	}
	@Override
	public int sizeStringToWidth(String str, int wrapWidth)
	{
		int i = str.length();
		float j = 0;
		int k = 0;
		int l = -1;

		for(boolean flag = false; k<i; ++k)
		{
			char c0 = str.charAt(k);
			switch(c0)
			{
			case '\n':
				--k;
				break;
			case ' ':
				l = k;
			default:
				j += this.getCharWidthFloat(c0);
				if(flag)
					++j;
				break;
			case '\u00a7':
				if (k < i - 1)
				{
					++k;
					char c1 = str.charAt(k);

					if (c1 != 108 && c1 != 76)
					{
						if(c1 == 114 || c1 == 82 || (c1>=48&&c1<=57 || c1>=97&&c1<=102 || c1>=65&&c1<=70))
							flag = false;
					}
					else
						flag = true;
				}
			}
			if(c0 == 10)
			{
				++k;
				l = k;
				break;
			}
			if(j>wrapWidth)
				break;
		}
		return k!=i && l!=-1 && l<k?l:k;
	}
}
