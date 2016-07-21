package wafflestomper.quiettime;

import org.lwjgl.input.Keyboard;

import net.minecraft.client.settings.KeyBinding;
import net.minecraftforge.fml.client.registry.ClientRegistry;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.InputEvent.KeyInputEvent;

public class KeyBindings{
	
	private KeyBinding toggleChatKey;
	private QuietTime quietTime;
	
	
	public KeyBindings(QuietTime qtRef){
		this.quietTime = qtRef;
		this.init();
	}
	
	
	public void init(){
		this.toggleChatKey = new KeyBinding("Toggle Chat", Keyboard.KEY_Y, "Quiet Time");
		ClientRegistry.registerKeyBinding(this.toggleChatKey);
    	FMLCommonHandler.instance().bus().register(this);
	}
	
	
	/**
	 * This is a hack that I have to do because Forge hasn't fully implemented the KeyInputEvent event
	 */
	@SubscribeEvent
	public void inputEvent(KeyInputEvent event)
	{
		if (event.isCanceled()) return;
		if (this.toggleChatKey.isPressed()){
			this.quietTime.toggleChat();
		}
	}
}
