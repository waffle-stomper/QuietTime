package wafflestomper.quiettime;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.google.common.base.Joiner;
import com.google.common.base.Splitter;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiIngameMenu;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.launchwrapper.Launch;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.Style;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.util.text.event.HoverEvent;
import net.minecraftforge.client.event.ClientChatReceivedEvent;
import net.minecraftforge.client.event.GuiOpenEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.Mod.EventHandler;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent.Phase;
import net.minecraftforge.fml.common.gameevent.TickEvent.RenderTickEvent;

@Mod(modid = QuietTime.MODID, version = QuietTime.VERSION, name = QuietTime.NAME, updateJSON = "https://raw.githubusercontent.com/waffle-stomper/QuietTime/master/update.json", canBeDeactivated = true)
public class QuietTime{
	
    public static final String MODID = "QuietTime";
    public static final String VERSION = "0.3.7";
    public static final String NAME = "Quiet Time";
    
    class SnitchUser{
    	public SnitchUser(String _username, long _time){
    		this.username = _username;
    		this.time = _time;
    	}
    	String username;
    	long time;
    }
    
    private Minecraft mc = Minecraft.getMinecraft();
    private boolean chatEventsDisabled = false;
    private boolean snitchHoverConversion = true;
    private boolean snitchRateLimiting = false;
    private long snitchSecondsBetweenMessages = 10l;
    private String statusMessage = "";
    private long statusTicksLeft = 0;
    private KeyBindings keyBindings;
    private ConfigManager config;
    boolean devEnv = false;
    private static final Splitter NEWLINE_SPLITTER = Splitter.on('\n');
    private static final Joiner NEWLINE_STRING_JOINER = Joiner.on("\\n");
    private static final Logger LOGGER = LogManager.getLogger(net.minecraft.client.Minecraft.class);
    private static final Pattern snitchPattern = Pattern.compile("(?<preamble>^ \\* (?<username>[a-zA-Z0-9_\\-]+?) (?:entered|logged (?:in to|out in)) snitch at )(?<snitchname>[a-zA-Z0-9_\\-]+)? (?<snitchlocation>\\[[a-zA-Z0-9, \\-]+\\])");
    private ArrayList<SnitchUser> snitchHistory = new ArrayList();
    
    @EventHandler
    public void init(FMLInitializationEvent event){
        MinecraftForge.EVENT_BUS.register(this);
        FMLCommonHandler.instance().bus().register(this);
        this.keyBindings = new KeyBindings(this);
     	this.devEnv = (Boolean)Launch.blackboard.get("fml.deobfuscatedEnvironment");
     	
     	
    }
    
    
    @EventHandler
    public void preInit(FMLPreInitializationEvent event) {
    	this.config = new ConfigManager();
    	this.config.preInit(event);
    	this.chatEventsDisabled = this.config.chatDisabledAtStartup;
    	this.snitchHoverConversion = this.config.snitchHover;
    	this.snitchRateLimiting = this.config.snitchRateLimiting;
    	this.snitchSecondsBetweenMessages = this.config.snitchSecondsBetweenMessages;
    }
    
    
    /**
     * Converts all snitch positions from plain text to hover text so they won't be seen in screenshots
     */
    public ITextComponent snitchHoverConversion(ITextComponent message){ 
    	// Message content:
    	// * USER logged in to snitch at namedsnitch [world 623 4 271]
        // * USER logged out in snitch at  [world 625 4 271]
        // * USER entered snitch at namedsnitch [world 623 4 271]
    	// tellraw qt {"text":" * USER logged in to snitch at NAME [world 1337 69 1337]","color":"aqua"}
    	Iterator<ITextComponent> ichat = message.iterator();
    	TextComponentString newMessage = null;
        while (ichat.hasNext()) {
            ITextComponent part = ichat.next();
            String s = part.getUnformattedComponentText(); // Note that we're going to need more than just the unformatted nonsense
            if (!s.isEmpty()){
            	Matcher pm = snitchPattern.matcher(s);
            	if (pm.find()){
	        		if (pm.groupCount() >= 1){
	        			String preamble = pm.group("preamble");
	        			String snitchLocation = pm.group("snitchlocation");
	        			String snitchName = pm.group("snitchname");
	        			if (snitchName == null){
	    	    			snitchName = "no_name";
	    	    		}
	        			if (preamble != null && !preamble.isEmpty() && snitchLocation != null && !snitchLocation.isEmpty()){
	        				Style aqua = new Style().setColor(TextFormatting.AQUA);
	        				newMessage = new TextComponentString(preamble);
	        				newMessage.setStyle(aqua);
	        				HoverEvent hover = new HoverEvent(HoverEvent.Action.SHOW_TEXT, new TextComponentString(snitchLocation));
	        				TextComponentString magicName = new TextComponentString(snitchName);
	        				magicName.setStyle(aqua);
	        				magicName.setStyle(new Style().setHoverEvent(hover));
	        				newMessage.appendSibling(magicName);
	        	        	LOGGER.info("<Snitch location converted to hovertext> {} {}", snitchName, snitchLocation);
	        			}
	        		}
            	}
            }
        }
        if (newMessage != null){
        	return(newMessage);
        }
    	return(message);
    }
    
    
    /**
     * Silently adds a line to the chat history
     */
    private void injectChatLine(ITextComponent message){
    	Method targetMethod;
		try {
			if (this.devEnv){
				targetMethod = this.mc.ingameGUI.getChatGUI().getClass().getDeclaredMethod("setChatLine", ITextComponent.class, Integer.TYPE, Integer.TYPE, boolean.class);
			}
			else{
				targetMethod = this.mc.ingameGUI.getChatGUI().getClass().getDeclaredMethod("func_146237_a", ITextComponent.class, Integer.TYPE, Integer.TYPE, boolean.class);
			}
			targetMethod.setAccessible(true);
			targetMethod.invoke(this.mc.ingameGUI.getChatGUI(), message, 0, this.mc.ingameGUI.getUpdateCounter()-300, true);
		} catch (NoSuchMethodException e1) {
			e1.printStackTrace();
		} catch (SecurityException e1) {
			e1.printStackTrace();
		} catch (IllegalAccessException e) {
			e.printStackTrace();
		} catch (IllegalArgumentException e) {
			e.printStackTrace();
		} catch (InvocationTargetException e) {
			e.printStackTrace();
		}
    }
    
    /**
     * First determines if a message is a snitch alert, then finds the username in the message
     * If the user has generated an alert less than snitchSecondsBetweenMessages seconds ago, the message is discarded (but still logged)
     * If the message is to be blocked, the event will be cancelled
     */
    private ClientChatReceivedEvent checkSnitchRateLimited(ClientChatReceivedEvent event){
    	String unformattedMessage = event.getMessage().getUnformattedText();
    	Matcher matcher = snitchPattern.matcher(unformattedMessage);
    	if (matcher.find()){
    		if (matcher.groupCount() >= 1){
    			String username = matcher.group("username");
    			if (username != null){
    				long currTime = System.currentTimeMillis();
    				long pruneTime = currTime - this.snitchSecondsBetweenMessages*1000;
    				// Prune old messages
    				ArrayList<String> usersLeft = new ArrayList();
    				Iterator i = this.snitchHistory.iterator();
    				while (i.hasNext()) {
    					SnitchUser u = (SnitchUser) i.next();
    					if (u.time < pruneTime){
    						i.remove();
    					}
    					else{
    						usersLeft.add(u.username);
    					}
    				}
    				// Check if the new username still exists in the history (meaning that there's 
    				//   at least one message within the cutoff). If it does, cancel the event, but log the message
    				if (usersLeft.contains(username)){
    					event.setCanceled(true);
    					LOGGER.info("[CHAT] {}", new Object[] {NEWLINE_STRING_JOINER.join(NEWLINE_SPLITTER.split(unformattedMessage))});
    				}
    				else{
    					this.snitchHistory.add(new SnitchUser(username, currTime));
    				}
    			}
    		}
    	}
    	return(event);
    }
    
    
    @SubscribeEvent(priority=EventPriority.HIGHEST)
    public void chatEvent(ClientChatReceivedEvent event){
    	// First, process any snitch co-ordinates
    	if (event.getType() == 1){
    		// Handle rate limiting
    		checkSnitchRateLimited(event);
    		if (event.isCanceled()){
    			return;
    		}
    		// Handle hover conversion
    		event.setMessage(snitchHoverConversion(event.getMessage()));
    	}
    	// Hide messages if chat is disabled
    	if (chatEventsDisabled){
    		ITextComponent message = event.getMessage();
    		event.setCanceled(true);
        	injectChatLine(message);
        	LOGGER.info("[CHAT] {}", new Object[] {NEWLINE_STRING_JOINER.join(NEWLINE_SPLITTER.split(message.getUnformattedText()))});
    	}
    }
    
    
    @SubscribeEvent
    public void tick(RenderTickEvent event) 
    {
    	if(event.phase == Phase.END){
    		if (this.statusTicksLeft > 0){
    			this.statusTicksLeft--;
    			ScaledResolution res = new ScaledResolution(this.mc);
    			int screenWidth = res.getScaledWidth();
    			int screenHeight = res.getScaledHeight();
    			int xPos = screenWidth/2 - this.mc.fontRendererObj.getStringWidth(this.statusMessage)/2;
    			int yPos = screenHeight-32;
    	    	this.mc.fontRendererObj.drawString(this.statusMessage, xPos, yPos, 0xFF00FFFF, true);
        	}
    	}
    }
    
    
    @SubscribeEvent
	public void guiOpen(GuiOpenEvent event){
		GuiScreen eventGui = event.getGui();
		if (eventGui == null){return;}
		if (eventGui instanceof GuiIngameMenu && !(eventGui instanceof QuietTimeGui)){
			event.setGui(new QuietTimeGui(this));
		}
    }
    
    
    public boolean isChatDisabled(){
    	return this.chatEventsDisabled;
    }
    
    
    public void toggleChat(){
    	this.chatEventsDisabled = !this.chatEventsDisabled;
    	if (this.chatEventsDisabled){
    		this.statusMessage = "Chat disabled";
    	}
    	else{
    		this.statusMessage = "Chat enabled";
    	}
    	this.statusTicksLeft = 100;
    }
}
